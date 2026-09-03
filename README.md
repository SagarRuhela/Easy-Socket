# EasySocket

[![](https://jitpack.io/v/SagarRuhela/Easy-Socket.svg)](https://jitpack.io/#SagarRuhela/Easy-Socket)


A lightweight Java WebSocket library that lets you build real-time chat applications in minutes — no boilerplate, no framework lock-in.

---

## Why EasySocket?

Building real-time applications with raw WebSocket APIs means dealing with threads, connection lifecycle, JSON parsing, heartbeats, and reconnection logic — before you write a single line of business logic.

EasySocket handles all of that. You write the logic. We handle the plumbing.

```java
// create a room
EasySocket.port(8080);
Room chat = EasySocket.createRoom("chat");
EasySocket.start();

// connect a client
Client sagar = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");

// send a message
sagar.send("hello everyone!");
```

That is it. Three lines for the server. Two lines for the client.

---

## Installation

### Step 1 — Add JitPack repository to your `pom.xml`

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

### Step 2 — Add the dependency

```xml
<dependency>
    <groupId>com.github.SagarRuhela</groupId>
    <artifactId>Easy-Socket</artifactId>
    <version>v1.0.2</version>
</dependency>
```

### Requirements

- Java 17 or higher
- Maven 3.6 or higher

---

## Quick Start

### Server

```java
import org.easySocket.EasySocket;
import org.easySocket.Room;

public class ChatServer {
    public static void main(String[] args) throws Exception {

        EasySocket.port(8080);
        Room chat = EasySocket.createRoom("chat");
        EasySocket.start();

        System.out.println("Server running on port 8080");
        Thread.currentThread().join(); // keep alive
    }
}
```

### Client

```java
import org.easySocket.Client;
import org.easySocket.EasySocket;

public class ChatClient {
    public static void main(String[] args) throws Exception {

        Client sagar = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");
        Client john  = EasySocket.joinRoom("ws://localhost:8080", "chat", "john");

        // register message listener
        john.onMessage(msg ->
            System.out.println(msg.getFrom() + ": " + msg.getContent())
        );

        // send a message
        sagar.send("hello everyone!");

        // Output:
        // sagar: hello everyone!
    }
}
```

---

## Core Concepts

### Room

A Room is a shared space where multiple clients can connect and exchange messages.
Every message sent by a client is broadcast to all members of the room by default.

```java
EasySocket.port(8080);
Room chat = EasySocket.createRoom("chat");
Room game = EasySocket.createRoom("game");   // multiple rooms, one server
EasySocket.start();
```

### Client

A Client represents one person connected to a room.
It wraps the WebSocket connection and exposes a clean API for sending and receiving messages.

```java
Client client = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");

// send a message to the room
client.send("hello!");

// receive messages from the room
client.onMessage(msg -> {
    System.out.println("from: "    + msg.getFrom());
    System.out.println("content: " + msg.getContent());
    System.out.println("room: "    + msg.getRoom());
});
```

### Message

Every message is automatically serialized to JSON and includes metadata about the sender and room.

```json
{
  "type":    "chat",
  "from":    "sagar",
  "room":    "chat",
  "content": "hello everyone!"
}
```

Message types:

| Type     | Description                                      |
|----------|--------------------------------------------------|
| `chat`   | Regular message sent by a client                 |
| `ping`   | Heartbeat check sent by client to server         |
| `pong`   | Heartbeat response sent by server to client      |
| `system` | Server notification (rate limit, errors, etc.)   |

---

## Features

### Broadcast — send to everyone

```java
room.broadcast("server announcement!");
```

### Direct message — send to one person

```java
room.sendTo("john", "hey john, this is just for you!");
```

### Check room members

```java
List<String> members = room.getMembers();
int count            = room.getSize();
```

---

## Advanced Configuration

For full control over server behavior use the Builder directly:

```java
import org.easySocket.EasySocketServer;
import org.easySocket.Room;

EasySocketServer server = new EasySocketServer.Builder()
    .port(8080)
    .addRoom(new Room("chat"))
    .addRoom(new Room("game"))

    // security
    .maxMessageSize(65536)          // reject messages over 64KB
    .maxMessagesPerSecond(10)       // rate limit per client
    .onValidate((room, name) -> {   // custom connection validation
        return name.length() >= 3;  // reject names shorter than 3 chars
    })

    // lifecycle callbacks
    .onOpen((room, session) ->
        System.out.println(session.getMemberName() + " joined " + room.getRoomName())
    )
    .onMessage((room, session, msg) -> {
        // custom message routing
        System.out.println(msg.getFrom() + ": " + msg.getContent());
        room.broadcast(msg.toJson());
    })
    .onClose((room, session) ->
        System.out.println(session.getMemberName() + " left " + room.getRoomName())
    )
    .onError((room, session) ->
        System.out.println("error from " + session.getMemberName())
    )
    .build();

server.start();
```

---

## Resilience — Auto-reconnect & Heartbeat

EasySocket automatically handles connection failures.
By default, clients reconnect up to 5 times with a 3 second delay between attempts.
A heartbeat ping is sent every 30 seconds to detect dead connections.

```java
Client client = new Client.Builder()
    .serverUrl("ws://localhost:8080")
    .roomName("chat")
    .memberName("sagar")
    .autoReconnect(true)        // enable auto-reconnect (default: true)
    .reconnectDelay(3000)       // wait 3 seconds before retrying (default: 3000ms)
    .maxRetries(5)              // give up after 5 attempts (default: 5)
    .heartbeatInterval(30000)   // ping every 30 seconds (default: 30000ms)
    .heartbeatTimeout(5000)     // wait 5 seconds for pong (default: 5000ms)
    .build();
```

### How heartbeat works

```
every 30 seconds
      ↓
client sends ping → server
      ↓
server replies with pong → client
      ↓
pong received   → connection alive → reset timer
pong not received within 5s → connection dead → reconnect
```

---

## Protections

### Max message size

Oversized messages are rejected automatically and the sender is notified:

```java
.maxMessageSize(65536) // 64KB — default
```

Client receives a system message:
```
message rejected — too large (200000 bytes, max 65536)
```

### Rate limiting

Clients exceeding the message rate are rejected automatically:

```java
.maxMessagesPerSecond(10) // default
```

Client receives a system message:
```
rate limit exceeded — max 10 messages per second
```

### Connection validation

Reject connections before they join a room:

```java
.onValidate((roomName, memberName) -> {
    // return true to allow, false to reject
    return myAuthService.isAllowed(memberName, roomName);
})
```

---

## Handling System Messages

Your client can listen for system notifications from the server:

```java
client.onMessage(msg -> {
    if (msg.isSystem()) {
        System.out.println("Server warning: " + msg.getContent());
    } else if (msg.isChat()) {
        System.out.println(msg.getFrom() + ": " + msg.getContent());
    }
});
```

---

## Database Integration

EasySocket does not include a database — by design.
Use the lifecycle callbacks to plug in your own storage:

```java
.onOpen((room, session) -> {
    // save to your own database
    db.save(new JoinEvent(room.getRoomName(), session.getMemberName()));
})
.onMessage((room, session, msg) -> {
    // save message history
    db.save(new ChatMessage(msg.getFrom(), msg.getContent()));
    room.broadcast(msg.toJson());
})
.onClose((room, session) -> {
    db.save(new LeaveEvent(room.getRoomName(), session.getMemberName()));
})
```

---

## API Reference

### `EasySocket`

| Method | Description |
|--------|-------------|
| `EasySocket.port(int port)` | Set the server port |
| `EasySocket.createRoom(String name)` | Create and register a room |
| `EasySocket.start()` | Start the server |
| `EasySocket.joinRoom(String url, String room, String name)` | Connect a client to a room |
| `EasySocket.reset()` | Reset server state (useful for testing) |

### `Room`

| Method | Description |
|--------|-------------|
| `room.broadcast(String message)` | Send to all members |
| `room.sendTo(String name, String message)` | Send to one member |
| `room.getMembers()` | Get list of member names |
| `room.getSize()` | Get member count |
| `room.getRoomName()` | Get room name |

### `Client`

| Method | Description |
|--------|-------------|
| `client.send(String message)` | Send a message to the room |
| `client.onMessage(ClientMessageListener)` | Register message callback |
| `client.disconnect()` | Disconnect cleanly |
| `client.getState()` | Get current connection state |
| `client.getMemberName()` | Get this client's name |
| `client.getRoomName()` | Get the room this client is in |

### `Message`

| Method | Description |
|--------|-------------|
| `msg.getType()` | `chat`, `ping`, `pong`, or `system` |
| `msg.getFrom()` | Who sent it |
| `msg.getRoom()` | Which room |
| `msg.getContent()` | The message text |
| `msg.isChat()` | True if type is chat |
| `msg.isPing()` | True if type is ping |
| `msg.isPong()` | True if type is pong |
| `msg.isSystem()` | True if type is system |
| `msg.toJson()` | Serialize to JSON string |
| `Message.fromJson(String)` | Deserialize from JSON string |

### `ConnectionState`

| State | Description |
|-------|-------------|
| `CONNECTING` | Handshake in progress |
| `OPEN` | Connected and ready |
| `CLOSING` | Close frame sent |
| `CLOSED` | Fully disconnected |

---

## License

MIT License — free to use in personal and commercial projects.

---

## Contributing

Pull requests are welcome. For major changes please open an issue first.

---

Built with ❤️ using [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket) and [Jackson](https://github.com/FasterXML/jackson).
