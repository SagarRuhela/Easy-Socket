package org.easySocket;

import org.java_websocket.WebSocket;

public class SocketSession {

    private final WebSocket raw;
    private volatile ConnectionState state;
    private final String memberName;

    // package-private constructor
    // users never create sessions themselves
    SocketSession(WebSocket raw,String memberName) {
        this.raw = raw;
        this.state = ConnectionState.OPEN;
        this.memberName=memberName;
    }

    /**
     * Send a text message to this client.
     * Ignored if connection is not OPEN.
     */
    public void send(String message) {
        if (state == ConnectionState.OPEN) {
            System.out.println("[Send] to "+memberName+" msg= "+message);
            raw.send(message);
        } else {
            System.out.println("[WARN] send() on " + state + " session. Ignored.");
        }
    }

    /**
     * Close this connection cleanly.
     */
    public void close() {
        state = ConnectionState.CLOSING;
        raw.close();
    }

    public ConnectionState getState() {
        return state;
    }


    public String getRemoteAddress() {
        return raw.getRemoteSocketAddress().toString();
    }

    // called internally when connection closes
    void markClosed() {
        state = ConnectionState.CLOSED;
    }
    public String getMemberName(){return this.memberName;}
}