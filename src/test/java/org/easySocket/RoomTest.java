package org.easySocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    private Room room;

    // creates a fake SocketSession without a real WebSocket connection
    private SocketSession mockSession(String name) {
        return new SocketSession(null, name);
    }

    @BeforeEach
    void setUp() {
        // fresh room before every test
        room = new Room("chat");
    }

    // -------------------------------------------------------
    // addMember tests
    // -------------------------------------------------------

    @Test
    void addMember_increasesSize() {
        room.addMembers("sagar", mockSession("sagar"));
        assertEquals(1, room.getSize());
    }

    @Test
    void addMember_memberAppearsInList() {
        room.addMembers("sagar", mockSession("sagar"));
        assertTrue(room.getMembers().contains("sagar"));
    }

    @Test
    void addMultipleMembers_allAppearInList() {
        room.addMembers("sagar", mockSession("sagar"));
        room.addMembers("john",  mockSession("john"));
        room.addMembers("priya", mockSession("priya"));

        assertEquals(3, room.getSize());
        assertTrue(room.getMembers().contains("sagar"));
        assertTrue(room.getMembers().contains("john"));
        assertTrue(room.getMembers().contains("priya"));
    }

    // -------------------------------------------------------
    // removeMember tests
    // -------------------------------------------------------

    @Test
    void removeMember_decreasesSize() {
        room.addMembers("sagar", mockSession("sagar"));
        room.removeMember("sagar");
        assertEquals(0, room.getSize());
    }

    @Test
    void removeMember_memberDisappearsFromList() {
        room.addMembers("sagar", mockSession("sagar"));
        room.removeMember("sagar");
        assertFalse(room.getMembers().contains("sagar"));
    }

    @Test
    void removeMember_thatDoesNotExist_doesNotCrash() {
        // removing a name that was never added should not throw
        assertDoesNotThrow(() -> room.removeMember("nobody"));
    }

    @Test
    void removeOneMember_othersStillPresent() {
        room.addMembers("sagar", mockSession("sagar"));
        room.addMembers("john",  mockSession("john"));
        room.removeMember("sagar");

        assertEquals(1, room.getSize());
        assertFalse(room.getMembers().contains("sagar"));
        assertTrue(room.getMembers().contains("john"));
    }

    // -------------------------------------------------------
    // getMembers tests
    // -------------------------------------------------------

    @Test
    void getMembers_emptyRoom_returnsEmptyList() {
        assertTrue(room.getMembers().isEmpty());
    }

    @Test
    void getMembers_returnsCorrectNames() {
        room.addMembers("sagar", mockSession("sagar"));
        room.addMembers("john",  mockSession("john"));

        assertTrue(room.getMembers().contains("sagar"));
        assertTrue(room.getMembers().contains("john"));
        assertEquals(2, room.getMembers().size());
    }

    // -------------------------------------------------------
    // getSize tests
    // -------------------------------------------------------

    @Test
    void getSize_emptyRoom_returnsZero() {
        assertEquals(0, room.getSize());
    }

    @Test
    void getSize_afterAddAndRemove_isCorrect() {
        room.addMembers("sagar", mockSession("sagar"));
        room.addMembers("john",  mockSession("john"));
        room.removeMember("sagar");
        assertEquals(1, room.getSize());
    }

    // -------------------------------------------------------
    // getRoomName tests
    // -------------------------------------------------------

    @Test
    void getRoomName_returnsCorrectName() {
        assertEquals("chat", room.getRoomName());
    }

    @Test
    void differentRooms_haveDifferentNames() {
        Room gameRoom = new Room("game");
        assertEquals("game", gameRoom.getRoomName());
        assertNotEquals(room.getRoomName(), gameRoom.getRoomName());
    }

    // -------------------------------------------------------
    // sendTo tests
    // -------------------------------------------------------

    @Test
    void sendTo_memberNotFound_doesNotCrash() {
        // sendTo a name that does not exist should not throw
        assertDoesNotThrow(() -> room.sendTo("nobody", "hello"));
    }
}