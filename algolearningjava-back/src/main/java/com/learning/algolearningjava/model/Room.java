package com.learning.algolearningjava.model;

import lombok.Getter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
public class Room {
    private final String roomId;
    private final Map<String, WebSocketSession> participants = new HashMap<>();
    private final Set<String> writableUsers = new HashSet<>();

    public Room(String roomId) {
        this.roomId = roomId;
    }

    public void join(String userId, WebSocketSession session) {
        participants.put(userId, session);
    }

    public void leave(String userId) {
        participants.remove(userId);
        writableUsers.remove(userId);
    }

    public boolean isWritable(String userId) {
        return writableUsers.contains(userId);
    }

    public void grantWrite(String userId) {
        writableUsers.add(userId);
    }

    public void revokeWrite(String userId) {
        writableUsers.remove(userId);
    }

    public void broadcast(WebSocketSession sender, String message) throws IOException {
        for (WebSocketSession session : participants.values()) {
            if (!session.equals(sender) && session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        }
    }

}
