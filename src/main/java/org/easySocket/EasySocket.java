package org.easySocket;

public class EasySocket {

    private EasySocket() {}

    // one static Builder — collects all rooms before server starts
    private static final EasySocketServer.Builder builder = new EasySocketServer.Builder()
            .onMessage((room, session, msg) -> room.broadcast(msg.toJson()));

    // set the port — returns EasySocket class for chaining
    public static void port(int port) {
        builder.port(port);
    }

    public static Room createRoom(String roomName) {
        Room room = new Room(roomName);
        builder.addRoom(room);
        return room;
    }

    public static void start() {
        EasySocketServer server = builder.build();
        Thread thread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public static Client joinRoom(String serverUrl,
                                  String roomName,
                                  String memberName) throws Exception {
        return new Client.Builder()
                .serverUrl(serverUrl)
                .roomName(roomName)
                .memberName(memberName)
                .build();
    }
}