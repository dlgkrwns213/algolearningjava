package com.learning.algolearningjava.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
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

    @Getter
    private final Map<String, WebSocketSession> participants = new HashMap<>();
    private final Set<String> writableUsers = new HashSet<>();

    @Getter
    @Setter
    private String code = "";

    public Room(String roomId) {
        this.roomId = roomId;
    }

    public void join(String userId, WebSocketSession session) throws IOException {
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

    public void broadcast(String json) throws IOException {
        for (WebSocketSession session : participants.values()) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(json));
            }
        }
    }
}
