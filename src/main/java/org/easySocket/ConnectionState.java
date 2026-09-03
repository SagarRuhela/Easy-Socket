package org.easySocket;


public enum ConnectionState {

    /**
     * Handshake is in progress.
     * The TCP connection exists but WebSocket is not ready yet.
     * You cannot call send() in this state.
     */
    CONNECTING,

    /**
     * Handshake completed successfully.
     * This is the only state where send() is allowed.
     */
    OPEN,

    /**
     * A close frame has been sent.
     * Waiting for the other side to acknowledge.
     * You cannot call send() in this state.
     */
    CLOSING,

    /**
     * Connection is fully closed.
     * No more messages can be sent or received.
     */
    CLOSED,
    RECONNECTING,

}