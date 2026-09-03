package org.easySocket;

/**
 * Called when a session opens or closes.
 *
 * Usage:
 *   .onOpen(session -> System.out.println("Connected: " + session.getRemoteAddress()))
 *   .onClose(session -> System.out.println("Disconnected"))
 */
@FunctionalInterface
public interface SessionListener {
    void onSession(Room room, SocketSession session);
}
