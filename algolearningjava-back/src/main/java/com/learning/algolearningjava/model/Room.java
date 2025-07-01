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
    private String ownerId;

    @Getter
    private final Map<String, WebSocketSession> participants;

    @Getter
    private final Set<String> writableUsers;

    @Getter
    @Setter
    private String code = "";

    public Room(String roomId, String ownerId) {
        this.roomId = roomId;
        this.ownerId = ownerId;
        this.code = "// 코드 작성란";
        this.participants = new HashMap<>();
        this.writableUsers = new HashSet<>();
        this.writableUsers.add(ownerId);
    }

    public boolean isOwner(String userId) {
        return ownerId.equals(userId);
    }

    public void addWritableUser(String requesterId, String targetUserId) {
        if (isOwner(requesterId) && participants.containsKey(targetUserId))
            writableUsers.add(targetUserId);
    }

    public void removeWritableUser(String requesterId, String targetUserId) {
        if (isOwner(requesterId) && participants.containsKey(targetUserId))
            writableUsers.remove(targetUserId);
    }


    public boolean join(String userId, WebSocketSession session) throws IOException {
        if (participants.containsKey(userId)) {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(
                        "{\"type\":\"error\",\"message\":\"이미 접속 중인 사용자입니다.\"}"
                ));
            }

            session.close();
            return true;
        }

        participants.put(userId, session);

        if (ownerId == null) {
            ownerId = userId;
            writableUsers.add(userId);
        }

        return false;
    }

    public void leave(String userId) {
        participants.remove(userId);
        writableUsers.remove(userId);

        if (userId.equals(ownerId)) {
            if (!participants.isEmpty()) {
                ownerId = participants.keySet().iterator().next();
                writableUsers.add(ownerId);
            } else {
                ownerId = null;
            }
        }
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
