package com.learning.algolearningjava.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.algolearningjava.dto.CodeMessage;
import com.learning.algolearningjava.model.Room;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Room> rooms = new HashMap<>();

    public void processMessage(WebSocketSession session, CodeMessage msg) throws Exception {
        Room room = rooms.computeIfAbsent(msg.getRoomId(), Room::new);

        switch (msg.getType()) {
            case "join":
                room.join(msg.getUserId(), session);
                break;
            case "codeChange":
                if (room.isWritable(msg.getUserId()))
                    broadcastRoom(room, msg);
                break;
            case "grandWrite":
                room.grantWrite(msg.getUserId());
                break;
            case "revokeWrite":
                room.revokeWrite(msg.getUserId());
                break;
        }
    }

    private void broadcastRoom(Room room, CodeMessage msg) throws Exception {
        String payload = objectMapper.writeValueAsString(msg);
        for (WebSocketSession s: room.getParticipants().values()) {
            if (s.isOpen())
                s.sendMessage(new TextMessage(payload));
        }
    }

    public Room getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, Room::new);
    }

    public void removeSession(WebSocketSession session) {
        for (Room room: rooms.values()) {
            room.getParticipants()
                    .entrySet()
                    .removeIf(entry -> entry.getValue().equals(session));
        }
    }
}
