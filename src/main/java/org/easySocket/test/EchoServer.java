package org.easySocket.test;


import org.easySocket.Client;
import org.easySocket.EasySocket;
import org.easySocket.EasySocketServer;
import org.easySocket.Room;

public class EchoServer {

    public static void main(String[] args) throws Exception {

//        EasySocketServer server = new EasySocketServer.Builder()
//                .port(8080)
//                .onOpen(session ->
//                        System.out.println("Client connected: " + session.getRemoteAddress())
//                )
//                .onMessage((session, msg) -> {
//                    System.out.println("Message: " + msg);
//                    session.send("Echo: " + msg);
//                })
//                .onClose(session ->
//                        System.out.println("Client disconnected: " + session.getRemoteAddress())
//                )
//                .onError(session -> {
//                    System.out.println("There is an error"+ session.getRemoteAddress());
//                })
//                .build();

//        server.start();



        // usage
//        EasySocket.port(8080);
//        Room chat = EasySocket.createRoom("chat");
//        EasySocket.start();
//
//        Thread.sleep(500);
//
//        Client sagar = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");
//        Client john  = EasySocket.joinRoom("ws://localhost:8080", "chat", "john");
//
//// now you get a Message object — you know who sent it
//        john.onMessage(msg ->
//                System.out.println("John received | from=" + msg.getFrom()
//                        + " | content=" + msg.getContent())
//        );
//
//        sagar.onMessage(msg ->
//                System.out.println("Sagar received | from=" + msg.getFrom()
//                        + " | content=" + msg.getContent())
//        );
//
//        Thread.sleep(500);
//        sagar.send("hello everyone!");
//        Thread.sleep(1000);


        EasySocket.port(8080);
        Room chat = EasySocket.createRoom("chat");
        EasySocket.start();

        Thread.sleep(500);

// test onValidate — reject names shorter than 3 chars
// add this to your builder in EasySocket.java temporarily:
// .onValidate((room, name) -> name.length() >= 3)

        Client sagar = EasySocket.joinRoom("ws://localhost:8080", "chat", "sagar");

        sagar.onMessage(msg -> {
            if (msg.isSystem()) {
                System.out.println("[SYSTEM WARNING] " + msg.getContent());
            } else {
                System.out.println("Sagar got: " + msg.getContent());
            }
        });

        Thread.sleep(300);

// test rate limiting — send 15 messages rapidly (max is 10)
        for (int i = 1; i <= 15; i++) {
            sagar.send("message " + i);
        }

        Thread.sleep(1000);
    }
}