package org.easySocket;

/**
 * Callback interface for handling incoming messages.
 *
 * This is a functional interface, which means users of your library
 * can use a lambda instead of writing a full class. For example:
 *
 *   session.onMessage(msg -> System.out.println("Got: " + msg));
 *
 * That one line is your entire "one function" API goal coming to life.
 */
@FunctionalInterface
public interface MessageListener {

    /**
     * Called when a text message arrives on the connection.
     *
     * @param message the raw text payload from the WebSocket frame
     */
    void onMessage(Room room, SocketSession session, Message message);
}