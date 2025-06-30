package com.learning.algolearningjava.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.algolearningjava.dto.CodeMessage;
import com.learning.algolearningjava.model.Room;
import com.learning.algolearningjava.model.RoomDocument;
import com.learning.algolearningjava.repository.RoomDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Room> rooms = new HashMap<>();
    private final RoomDocumentRepository roomDocumentRepository;

    public void processMessage(WebSocketSession session, CodeMessage msg) throws Exception {
        Room room = rooms.computeIfAbsent(msg.getRoomId(), id -> {
            Room r = new Room(id);
            // DB에서 마지막 코드 로딩
            RoomDocument doc = roomDocumentRepository.findById(id).orElse(null);
            if (doc != null) {
                r.setCode(doc.getCode()); // Room에 code 저장 필드가 있어야 함
            }
            return r;
        });

        switch (msg.getType()) {
            case "join":
                room.join(msg.getUserId(), session);

                CodeMessage initCodeMessage = CodeMessage.builder()
                        .type("codeChange")
                        .roomId(msg.getRoomId())
                        .userId("server")
                        .content(room.getCode())
                        .build();

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initCodeMessage)));
                break;
            case "codeChange":
                if (true || room.isWritable(msg.getUserId())) {
                    room.setCode(msg.getContent());
                    saveCodeToDB(msg.getRoomId(), msg.getContent());
                    broadcastRoom(room, msg);
                }
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

    private void saveCodeToDB(String roomId, String content) {
        System.out.println("Saving to DB: roomId=" + roomId + ", content length=" + content.length());
        try {
            roomDocumentRepository.save(
                    RoomDocument.builder()
                            .roomId(roomId)
                            .code(content)
                            .updateAt(LocalDateTime.now())
                            .build()
            );
            System.out.println("DB 저장 완료");
        } catch (Exception e) {
            System.err.println("DB 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public Room getOrCreateRoom(String roomId) {
        return rooms.computeIfAbsent(roomId, id -> {
            Room r = new Room(id);
            RoomDocument doc = roomDocumentRepository.findById(id).orElse(null);
            if (doc != null) r.setCode(doc.getCode());
            return r;
        });
    }

    public void removeSession(WebSocketSession session) {
        for (Room room: rooms.values()) {
            room.getParticipants()
                    .entrySet()
                    .removeIf(entry -> entry.getValue().equals(session));
        }
    }
}
