package org.easySocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class Room {
    private final String roomName;
    private final ConcurrentHashMap<String, SocketSession> members = new ConcurrentHashMap<>();

    public  Room(String name){
        this.roomName=name;
    }

    public void broadcast(String message){
        for(SocketSession session:members.values()){
            session.send(message);
        }
    }

     public void addMembers(String name, SocketSession session){
        members.put(name,session);

    }

    public void sendTo(String name, String message){
        SocketSession session=members.get(name);
        if(session==null){
            System.out.println("[ROOM] member not found: " + name);
            return;        }
        session.send(message);
    }

     void removeMember(String name){
        members.remove(name);
    }

    public List<String> getMembers(){
        List<String> ans=new ArrayList<>();
        for(String members:members.keySet()){
                ans.add(members);
        }
        return ans;
    }



    public int getSize(){
        return members.size();
    }
    public  String getRoomName(){
        return roomName;
    }

}
