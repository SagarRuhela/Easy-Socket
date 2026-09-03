package org.easySocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Client {

    private WebSocketClient raw;
    private volatile ConnectionState state;
    private final List<ClientMessageListener> listeners;
    private final String memberName;
    private final String roomName;
    private final String serverUrl;

    // --- Phase 4 fields ---
    private final boolean autoReconnect;
    private final int reconnectDelay;    // milliseconds between retries
    private final int maxRetries;        // max reconnect attempts
    private final int heartbeatInterval; // milliseconds between pings
    private final int heartbeatTimeout;  // milliseconds to wait for pong

    private int retryCount = 0;
    private volatile boolean intentionalClose = false; // true when user calls disconnect()

    // scheduler runs heartbeat and reconnect timers on background threads
    private final ScheduledExecutorService scheduler
            = Executors.newScheduledThreadPool(2);

    private ScheduledFuture<?> heartbeatTask;  // the repeating ping task
    private ScheduledFuture<?> pongTimeoutTask; // the pong deadline timer

    private Client(Builder builder,
                   WebSocketClient raw,
                   List<ClientMessageListener> listeners) {
        this.raw               = raw;
        this.listeners         = listeners;
        this.memberName        = builder.memberName;
        this.roomName          = builder.roomName;
        this.serverUrl         = builder.serverUrl;
        this.autoReconnect     = builder.autoReconnect;
        this.reconnectDelay    = builder.reconnectDelay;
        this.maxRetries        = builder.maxRetries;
        this.heartbeatInterval = builder.heartbeatInterval;
        this.heartbeatTimeout  = builder.heartbeatTimeout;
        this.state             = ConnectionState.CONNECTING;
    }


    /**
     * Send a chat message to the room.
     * Automatically wraps in JSON with type=chat.
     */
    public void send(String content) {
        if (state == ConnectionState.OPEN) {
            Message message = Message.chat(memberName, roomName, content);
            raw.send(message.toJson());
        } else {
            System.out.println("[CLIENT] send() ignored — state: " + state);
        }
    }

    /**
     * Register a callback for messages coming FROM the server.
     * Only fires for type=chat and type=system messages.
     * ping/pong are handled internally — never reach your listeners.
     */
    public void onMessage(ClientMessageListener listener) {
        listeners.add(listener);
    }

    /**
     * Disconnect cleanly — will NOT auto-reconnect.
     */
    public void disconnect() {
        intentionalClose = true; // signal: don't reconnect
        stopHeartbeat();
        scheduler.shutdown();
        state = ConnectionState.CLOSING;
        raw.close();
    }

    public ConnectionState getState() { return state; }
    public String getMemberName()     { return memberName; }
    public String getRoomName()       { return roomName; }


    /**
     * Starts sending a ping every heartbeatInterval milliseconds.
     * Each ping starts a pong timeout — if pong doesn't arrive
     * within heartbeatTimeout ms, connection is declared dead.
     */
    private void startHeartbeat() {
        if (heartbeatInterval <= 0) return; // heartbeat disabled

        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (state == ConnectionState.OPEN) {
                sendPing();
                startPongTimeout();
            }
        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);

        System.out.println("[CLIENT] heartbeat started — interval: "
                + heartbeatInterval + "ms");
    }

    /**
     * Sends a ping message to the server.
     */
    private void sendPing() {
        Message ping = Message.ping(memberName, roomName);
        raw.send(ping.toJson());
        System.out.println("[CLIENT] ping sent");
    }

    /**
     * Starts a countdown — if pong doesn't arrive before it fires,
     * the connection is declared dead and reconnect is triggered.
     */
    private void startPongTimeout() {
        // cancel any existing timeout before starting a new one
        cancelPongTimeout();

        pongTimeoutTask = scheduler.schedule(() -> {
            System.out.println("[CLIENT] pong timeout — connection dead");
            // force close the raw connection — this will trigger onClose
            // which triggers reconnect if autoReconnect is enabled
            raw.close();
        }, heartbeatTimeout, TimeUnit.MILLISECONDS);
    }

    /**
     * Called when pong arrives — cancels the timeout so reconnect is not triggered.
     */
    private void cancelPongTimeout() {
        if (pongTimeoutTask != null && !pongTimeoutTask.isDone()) {
            pongTimeoutTask.cancel(false);
        }
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isDone()) {
            heartbeatTask.cancel(false);
        }
        cancelPongTimeout();
    }


    /**
     * Attempts to reconnect to the server after reconnectDelay ms.
     * Stops after maxRetries attempts.
     */
    private void reconnect() {
        if (intentionalClose) return;    // user called disconnect() — don't reconnect
        if (!autoReconnect) return;      // feature disabled
        if (retryCount >= maxRetries) {
            System.out.println("[CLIENT] max retries reached ("
                    + maxRetries + ") — giving up");
            return;
        }

        retryCount++;
        System.out.println("[CLIENT] reconnecting in " + reconnectDelay
                + "ms (attempt " + retryCount + "/" + maxRetries + ")");

        scheduler.schedule(() -> {
            try {
                state = ConnectionState.CONNECTING;

                // build a fresh WebSocketClient with the same URI and callbacks
                URI uri = new URI(serverUrl + "?room=" + roomName
                        + "&name=" + memberName);
                raw = buildRawClient(uri);
                raw.connectBlocking();

                // if we get here, connection succeeded
                retryCount = 0; // reset retry count on success

            } catch (Exception e) {
                System.out.println("[CLIENT] reconnect attempt failed: "
                        + e.getMessage());
                reconnect(); // try again
            }
        }, reconnectDelay, TimeUnit.MILLISECONDS);
    }



    /**
     * Creates a WebSocketClient with all the callbacks wired up.
     * Extracted so reconnect() can create a fresh one with the same logic.
     */
    private WebSocketClient buildRawClient(URI uri) {
        return new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                state = ConnectionState.OPEN;
                System.out.println("[CLIENT] connected to room '"
                        + roomName + "' as '" + memberName + "'");
                stopHeartbeat();   // stop any old heartbeat first
                startHeartbeat();  // start fresh heartbeat
            }

            @Override
            public void onMessage(String json) {
                try {
                    Message message = Message.fromJson(json);

                    // ping/pong handled internally — never passed to user
                    if (message.isPong()) {
                        System.out.println("[CLIENT] pong received");
                        cancelPongTimeout(); // connection confirmed alive
                        return;
                    }

                    if (message.isPing()) {
                        // server sent a ping — reply with pong
                        Message pong = Message.pong(roomName);
                        raw.send(pong.toJson());
                        return;
                    }

                    // chat and system messages → call user's listeners
                    for (ClientMessageListener listener : listeners) {
                        listener.onMessage(message);
                    }

                } catch (Exception e) {
                    System.out.println("[CLIENT] failed to parse: " + json);
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                state = ConnectionState.CLOSED;
                stopHeartbeat();
                System.out.println("[CLIENT] disconnected — reason: "
                        + reason + " | remote: " + remote);

                // reconnect must happen on a separate thread
                new Thread(this::reconnect).start();
            }

            @Override
            public void onError(Exception ex) {
                System.out.println("[CLIENT] error: " + ex.getMessage());
            }
        };
    }



    public static class Builder {

        private String serverUrl;
        private String roomName;
        private String memberName;
        private ClientMessageListener initialListener = null;

        // Phase 4 config — sensible defaults
        private boolean autoReconnect     = true;
        private int reconnectDelay        = 3000;  // 3 seconds
        private int maxRetries            = 5;
        private int heartbeatInterval     = 30000; // 30 seconds
        private int heartbeatTimeout      = 5000;  // 5 seconds to receive pong

        public Builder serverUrl(String serverUrl) {
            this.serverUrl = serverUrl;
            return this;
        }

        public Builder roomName(String roomName) {
            this.roomName = roomName;
            return this;
        }

        public Builder memberName(String memberName) {
            this.memberName = memberName;
            return this;
        }

        public Builder onMessage(ClientMessageListener listener) {
            this.initialListener = listener;
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
            return this;
        }

        public Builder reconnectDelay(int milliseconds) {
            this.reconnectDelay = milliseconds;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder heartbeatInterval(int milliseconds) {
            this.heartbeatInterval = milliseconds;
            return this;
        }

        public Builder heartbeatTimeout(int milliseconds) {
            this.heartbeatTimeout = milliseconds;
            return this;
        }

        public Client build() throws Exception {

            if (serverUrl  == null) throw new IllegalStateException("serverUrl() is required");
            if (roomName   == null) throw new IllegalStateException("roomName() is required");
            if (memberName == null) throw new IllegalStateException("memberName() is required");

            List<ClientMessageListener> listeners = new CopyOnWriteArrayList<>();
            if (initialListener != null) {
                listeners.add(initialListener);
            }

            // create Client first with a null raw — buildRawClient needs
            // the Client to exist so it can reference its fields
            Client client = new Client(this, null, listeners);

            URI uri = new URI(serverUrl + "?room=" + roomName
                    + "&name=" + memberName);

            // build the raw WebSocketClient using the factory method
            WebSocketClient raw = client.buildRawClient(uri);
            client.raw = raw;

            raw.connectBlocking();
            return client;
        }
    }
}