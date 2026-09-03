# EasySocket

A simple Java WebSocket library for building real-time chat applications in minutes.

## Installation

Add JitPack to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependency:

```xml
<dependency>
    <groupId>com.github.yourusername</groupId>
    <artifactId>easysocket</artifactId>
    <version>v1.0.0</version>
</dependency>
```

## Quick Start

### Server

```java
EasySocket.port(8080);
Room chat = EasySocket.createRoom("chat");
EasySocket.start();
```

### Client

```java
Client client = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");

client.onMessage(msg ->
    System.out.println(msg.getFrom() + ": " + msg.getContent())
);

client.send("hello everyone!");
```

## Features

- One line to create a room
- One line to join a room
- Automatic JSON messaging
- Heartbeat with auto-reconnect
- Rate limiting
- Max message size protection
- Connection validation hook

## Advanced Usage

### Custom server configuration

```java
new EasySocketServer.Builder()
    .port(8080)
    .addRoom(new Room("chat"))
    .maxMessageSize(65536)
    .maxMessagesPerSecond(10)
    .onValidate((room, name) -> name.length() >= 3)
    .onOpen((room, session) ->
        System.out.println(session.getMemberName() + " joined " + room.getRoomName())
    )
    .onMessage((room, session, msg) -> room.broadcast(msg.toJson()))
    .onClose((room, session) ->
        System.out.println(session.getMemberName() + " left")
    )
    .build()
    .start();
```

### Direct message

```java
room.sendTo("john", "hey john!");
```

### Auto-reconnect

```java
Client client = new Client.Builder()
    .serverUrl("ws://localhost:8080")
    .roomName("chat")
    .memberName("sagar")
    .autoReconnect(true)
    .reconnectDelay(3000)
    .maxRetries(5)
    .build();
```