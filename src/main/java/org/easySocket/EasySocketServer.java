package org.easySocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EasySocketServer {

    private final InternalServer server;

    private EasySocketServer(InternalServer server) {
        this.server = server;
    }

    public void start() throws InterruptedException {
        server.start();
        System.out.println("[EasySocket] Server started on port " + server.getPort());
        Thread.currentThread().join();
    }

    public void stop() throws Exception {
        server.stop();
        System.out.println("[EasySocket] Server stopped.");
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static class Builder {

        private int port                    = 8080;
        private SessionListener onOpen      = null;
        private MessageListener onMessage   = null;
        private SessionListener onClose     = null;
        private SessionListener onError     = null;
        private ConnectionValidator onValidate = null;
        private final Map<String, Room> rooms = new ConcurrentHashMap<>();
        private int messageMaxSize          = 65536; // 64KB
        private int maxMessagesPerSecond    = 10;    // default rate limit

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder maxMessageSize(int sizeInBytes) {
            this.messageMaxSize = sizeInBytes;
            return this;
        }

        public Builder maxMessagesPerSecond(int max) {
            this.maxMessagesPerSecond = max;
            return this;
        }

        public Builder onValidate(ConnectionValidator validator) {
            this.onValidate = validator;
            return this;
        }

        public Builder onOpen(SessionListener listener) {
            this.onOpen = listener;
            return this;
        }

        public Builder onMessage(MessageListener listener) {
            this.onMessage = listener;
            return this;
        }

        public Builder onClose(SessionListener listener) {
            this.onClose = listener;
            return this;
        }

        public Builder onError(SessionListener listener) {
            this.onError = listener;
            return this;
        }

        public Builder addRoom(Room room) {
            rooms.put(room.getRoomName(), room);
            return this;
        }

        public EasySocketServer build() {
            if (onMessage == null) {
                throw new IllegalStateException(
                        "onMessage() is required. What should the server do with messages?"
                );
            }
            InternalServer internal = new InternalServer(
                    port, onOpen, onMessage, onClose, onError,
                    onValidate, rooms, messageMaxSize, maxMessagesPerSecond
            );
            return new EasySocketServer(internal);
        }
    }

    // -------------------------------------------------------------------------
    // InternalServer
    // -------------------------------------------------------------------------

    private static class InternalServer extends WebSocketServer {

        private Map<String, Room> rooms                          = new ConcurrentHashMap<>();
        private final Map<WebSocket, Room> sessionRooms          = new ConcurrentHashMap<>();
        private final Map<WebSocket, String> sessionNames        = new ConcurrentHashMap<>();
        private final Map<WebSocket, SocketSession> sessions     = new ConcurrentHashMap<>();

        // rate limiting maps
        private final Map<WebSocket, Integer> messageCount       = new ConcurrentHashMap<>();
        private final Map<WebSocket, Long> windowStart           = new ConcurrentHashMap<>();

        private final SessionListener onOpen;
        private final MessageListener onMessage;
        private final SessionListener onClose;
        private final SessionListener onError;
        private final ConnectionValidator onValidate;
        private final int messageMaxSize;
        private final int maxMessagesPerSecond;

        InternalServer(int port,
                       SessionListener onOpen,
                       MessageListener onMessage,
                       SessionListener onClose,
                       SessionListener onError,
                       ConnectionValidator onValidate,
                       Map<String, Room> rooms,
                       int messageMaxSize,
                       int maxMessagesPerSecond) {
            super(new InetSocketAddress(port));
            this.onOpen               = onOpen;
            this.onMessage            = onMessage;
            this.onClose              = onClose;
            this.onError              = onError;
            this.onValidate           = onValidate;
            this.rooms                = rooms;
            this.messageMaxSize       = messageMaxSize;
            this.maxMessagesPerSecond = maxMessagesPerSecond;
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            String query = conn.getResourceDescriptor();

            String[] params   = query.substring(2).split("&");
            String roomName   = params[0].split("=")[1];
            String memberName = params[1].split("=")[1];

            if (!rooms.containsKey(roomName)) {
                System.out.println("[SERVER] room not found: " + roomName);
                conn.close(1008, "room not found: " + roomName);
                return;
            }

            Room room = rooms.get(roomName);

            // Protection — duplicate name
            if (room.getMembers().contains(memberName)) {
                conn.close(1008, "name '" + memberName + "' already taken in room");
                return;
            }

            // Protection 3 — onValidate hook
            if (onValidate != null && !onValidate.validate(roomName, memberName)) {
                System.out.println("[SERVER] connection rejected by validator: "
                        + memberName + " → " + roomName);
                conn.close(1008, "connection rejected by server");
                return;
            }

            SocketSession session = new SocketSession(conn, memberName);
            sessions.put(conn, session);
            room.addMembers(memberName, session);
            sessionRooms.put(conn, room);
            sessionNames.put(conn, memberName);

            // initialise rate limit tracking for this connection
            messageCount.put(conn, 0);
            windowStart.put(conn, System.currentTimeMillis());

            System.out.println("[SERVER] " + memberName + " joined room: " + roomName);

            if (onOpen != null) {
                onOpen.onSession(room, session);
            }
        }

        @Override
        public void onMessage(WebSocket conn, String json) {

            SocketSession session = sessions.get(conn);
            Room room             = sessionRooms.get(conn);

            if (session == null || room == null) return;

            try {
                // Protection 1 — max message size
                if (json.length() > messageMaxSize) {
                    System.out.println("[SERVER] message too large from "
                            + session.getMemberName()
                            + " — size: " + json.length()
                            + " max: " + messageMaxSize);
                    session.send(Message.system(
                            room.getRoomName(),
                            "message rejected — too large ("
                                    + json.length() + " bytes, max "
                                    + messageMaxSize + ")"
                    ).toJson());
                    return;
                }

                // Protection 2 — rate limiting
                long now          = System.currentTimeMillis();
                long windowAge    = now - windowStart.getOrDefault(conn, now);

                if (windowAge > 1000) {
                    // new 1-second window — reset counters
                    windowStart.put(conn, now);
                    messageCount.put(conn, 0);
                }

                int count = messageCount.getOrDefault(conn, 0) + 1;
                messageCount.put(conn, count);

                if (count > maxMessagesPerSecond) {
                    System.out.println("[SERVER] rate limit exceeded by "
                            + session.getMemberName()
                            + " — " + count + " msgs/sec, max "
                            + maxMessagesPerSecond);
                    session.send(Message.system(
                            room.getRoomName(),
                            "rate limit exceeded — max "
                                    + maxMessagesPerSecond
                                    + " messages per second"
                    ).toJson());
                    return;
                }

                // parse JSON
                Message message = Message.fromJson(json);

                // handle ping internally
                if (message.isPing()) {
                    System.out.println("[SERVER] ping from "
                            + message.getFrom() + " — sending pong");
                    session.send(Message.pong(room.getRoomName()).toJson());
                    return;
                }

                // log chat messages
                if (message.isChat()) {
                    System.out.println("[MESSAGE] room=" + room.getRoomName()
                            + " | from=" + message.getFrom()
                            + " | content=" + message.getContent());
                }

                // call user's lambda
                onMessage.onMessage(room, session, message);

            } catch (Exception e) {
                System.out.println("[SERVER] failed to parse message: " + json);
            }
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            SocketSession session  = sessions.remove(conn);
            Room room              = sessionRooms.remove(conn);
            String memberName      = sessionNames.remove(conn);

            // clean up rate limiting maps
            messageCount.remove(conn);
            windowStart.remove(conn);

            if (room != null) room.removeMember(memberName);

            if (session != null) {
                session.markClosed();
                System.out.println("[CLOSE] " + session.getMemberName()
                        + " | code=" + code
                        + " | remaining: " + sessions.size());
                if (onClose != null) {
                    onClose.onSession(room, session);
                }
            }
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            System.out.println("[ERROR] " + ex.getMessage());
            if (conn != null && onError != null) {
                SocketSession session = sessions.get(conn);
                Room room             = sessionRooms.get(conn);
                if (session != null) {
                    onError.onSession(room, session);
                }
            }
        }

        @Override
        public void onStart() {
            System.out.println("[START] Listening on port " + getPort());
        }
    }
}