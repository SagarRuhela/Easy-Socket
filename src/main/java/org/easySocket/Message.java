package org.easySocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnore;

public class Message {

    private static final ObjectMapper mapper = new ObjectMapper();

    // type tells both sides what kind of message this is
    // "chat"   → regular message
    // "ping"   → heartbeat check from client
    // "pong"   → heartbeat response from server
    // "system" → server notifications (joined, left, error)
    private String type;
    private String from;
    private String room;
    private String content;

    public Message() {}

    public Message(String type, String from, String room, String content) {
        this.type    = type;
        this.from    = from;
        this.room    = room;
        this.content = content;
    }

    // convenience constructor for regular chat messages
    public static Message chat(String from, String room, String content) {
        return new Message("chat", from, room, content);
    }

    // convenience constructor for ping
    public static Message ping(String from, String room) {
        return new Message("ping", from, room, "");
    }

    // convenience constructor for pong
    public static Message pong(String room) {
        return new Message("pong", "server", room, "");
    }

    // convenience constructor for system messages
    public static Message system(String room, String content) {
        return new Message("system", "server", room, content);
    }

    public String toJson() {
        try {
            return mapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message", e);
        }
    }

    public static Message fromJson(String json) {
        try {
            return mapper.readValue(json, Message.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse message: " + json, e);
        }
    }
    @JsonIgnore
    public boolean isPing()   { return "ping".equals(type); }
    @JsonIgnore
    public boolean isPong()   { return "pong".equals(type); }
    @JsonIgnore
    public boolean isChat()   { return "chat".equals(type); }
    @JsonIgnore

    public boolean isSystem() { return "system".equals(type); }

    public String getType()    { return type; }
    public String getFrom()    { return from; }
    public String getRoom()    { return room; }
    public String getContent() { return content; }

    public void setType(String type)       { this.type = type; }
    public void setFrom(String from)       { this.from = from; }
    public void setRoom(String room)       { this.room = room; }
    public void setContent(String content) { this.content = content; }

    @Override
    public String toString() {
        return "[type=" + type + " | from=" + from
                + " | room=" + room + " | content=" + content + "]";
    }
}