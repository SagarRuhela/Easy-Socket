package org.easySocket;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    // -------------------------------------------------------
    // Message.chat() tests
    // -------------------------------------------------------

    @Test
    void chatMessage_isChat_returnsTrue() {
        Message msg = Message.chat("sagar", "chat", "hello");
        assertTrue(msg.isChat());
    }

    @Test
    void chatMessage_isNotPing() {
        Message msg = Message.chat("sagar", "chat", "hello");
        assertFalse(msg.isPing());
    }

    @Test
    void chatMessage_isNotPong() {
        Message msg = Message.chat("sagar", "chat", "hello");
        assertFalse(msg.isPong());
    }

    @Test
    void chatMessage_isNotSystem() {
        Message msg = Message.chat("sagar", "chat", "hello");
        assertFalse(msg.isSystem());
    }

    @Test
    void chatMessage_fieldsAreCorrect() {
        Message msg = Message.chat("sagar", "chat", "hello everyone!");
        assertEquals("sagar",           msg.getFrom());
        assertEquals("chat",            msg.getRoom());
        assertEquals("hello everyone!", msg.getContent());
        assertEquals("chat",            msg.getType());
    }

    // -------------------------------------------------------
    // Message.ping() tests
    // -------------------------------------------------------

    @Test
    void pingMessage_isPing_returnsTrue() {
        Message msg = Message.ping("sagar", "chat");
        assertTrue(msg.isPing());
    }

    @Test
    void pingMessage_isNotChat() {
        Message msg = Message.ping("sagar", "chat");
        assertFalse(msg.isChat());
    }

    @Test
    void pingMessage_hasEmptyContent() {
        Message msg = Message.ping("sagar", "chat");
        assertEquals("", msg.getContent());
    }

    // -------------------------------------------------------
    // Message.pong() tests
    // -------------------------------------------------------

    @Test
    void pongMessage_isPong_returnsTrue() {
        Message msg = Message.pong("chat");
        assertTrue(msg.isPong());
    }

    @Test
    void pongMessage_fromIsServer() {
        Message msg = Message.pong("chat");
        assertEquals("server", msg.getFrom());
    }

    @Test
    void pongMessage_isNotChat() {
        Message msg = Message.pong("chat");
        assertFalse(msg.isChat());
    }

    // -------------------------------------------------------
    // Message.system() tests
    // -------------------------------------------------------

    @Test
    void systemMessage_isSystem_returnsTrue() {
        Message msg = Message.system("chat", "welcome!");
        assertTrue(msg.isSystem());
    }

    @Test
    void systemMessage_fromIsServer() {
        Message msg = Message.system("chat", "welcome!");
        assertEquals("server", msg.getFrom());
    }

    @Test
    void systemMessage_contentIsCorrect() {
        Message msg = Message.system("chat", "rate limit exceeded");
        assertEquals("rate limit exceeded", msg.getContent());
    }

    // -------------------------------------------------------
    // toJson() tests
    // -------------------------------------------------------

    @Test
    void toJson_containsAllFields() {
        Message msg = Message.chat("sagar", "chat", "hello");
        String json = msg.toJson();

        assertTrue(json.contains("\"from\":\"sagar\""));
        assertTrue(json.contains("\"room\":\"chat\""));
        assertTrue(json.contains("\"content\":\"hello\""));
        assertTrue(json.contains("\"type\":\"chat\""));
    }

    @Test
    void toJson_doesNotContainHelperBooleans() {
        // isPing, isPong, isChat, isSystem should NOT appear in JSON
        // they are @JsonIgnore
        Message msg = Message.chat("sagar", "chat", "hello");
        String json = msg.toJson();

        assertFalse(json.contains("\"ping\""));
        assertFalse(json.contains("\"pong\""));
        assertFalse(json.contains("\"system\""));
    }

    // -------------------------------------------------------
    // fromJson() tests
    // -------------------------------------------------------

    @Test
    void fromJson_parsesCorrectly() {
        String json = "{\"type\":\"chat\",\"from\":\"sagar\","
                + "\"room\":\"chat\",\"content\":\"hello\"}";

        Message msg = Message.fromJson(json);

        assertEquals("chat",  msg.getType());
        assertEquals("sagar", msg.getFrom());
        assertEquals("chat",  msg.getRoom());
        assertEquals("hello", msg.getContent());
    }

    @Test
    void toJson_thenFromJson_roundTrip() {
        // serialize then deserialize — should get same values back
        Message original = Message.chat("sagar", "chat", "hello everyone!");
        String json      = original.toJson();
        Message parsed   = Message.fromJson(json);

        assertEquals(original.getType(),    parsed.getType());
        assertEquals(original.getFrom(),    parsed.getFrom());
        assertEquals(original.getRoom(),    parsed.getRoom());
        assertEquals(original.getContent(), parsed.getContent());
    }

    @Test
    void fromJson_invalidJson_throwsException() {
        // invalid JSON should throw RuntimeException
        assertThrows(RuntimeException.class, () ->
                Message.fromJson("this is not json")
        );
    }

    // -------------------------------------------------------
    // toString() tests
    // -------------------------------------------------------

    @Test
    void toString_containsAllFields() {
        Message msg = Message.chat("sagar", "chat", "hello");
        String str  = msg.toString();

        assertTrue(str.contains("sagar"));
        assertTrue(str.contains("chat"));
        assertTrue(str.contains("hello"));
    }
}