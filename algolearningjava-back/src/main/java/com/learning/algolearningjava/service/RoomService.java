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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Room> rooms = new HashMap<>();
    private final RoomDocumentRepository roomDocumentRepository;

    public void processMessage(WebSocketSession session, CodeMessage msg) throws Exception {
//        System.out.println("📩 수신 메시지: " + msg.getType() + ", from: " + msg.getUserId());

        Room room = rooms.computeIfAbsent(msg.getRoomId(), id -> {
            Room r = new Room(id, msg.getUserId());
            // DB에서 마지막 코드 로딩
            RoomDocument doc = roomDocumentRepository.findById(id).orElse(null);
            if (doc != null) {
                r.setCode(doc.getCode()); // Room에 code 저장 필드가 있어야 함
            }
            return r;
        });

        CodeMessage newMsg;
        switch (msg.getType()) {
            case "join":
                if (room.join(msg.getUserId(), session))
                    return;

                // 1. 현재 코드 전송
                CodeMessage initCodeMessage = CodeMessage.builder()
                        .type("codeChange")
                        .roomId(msg.getRoomId())
                        .userId("server")
                        .content(room.getCode())
                        .build();

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(initCodeMessage)));

                // 2. owner 정보 전송
                CodeMessage ownerInfoMessage = CodeMessage.builder()
                        .type("ownerInfo")
                        .roomId(msg.getRoomId())
                        .userId("server")
                        .ownerId(room.getOwnerId())
                        .build();

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ownerInfoMessage)));

                // 3. 현재 참여자 목록 전송
                CodeMessage participantListMessage = CodeMessage.builder()
                        .type("participantList")
                        .roomId(msg.getRoomId())
                        .userId("server")
                        .participants(new ArrayList<>(room.getParticipants().keySet()))
                        .build();
                broadcastRoom(room, participantListMessage);

                // 4. 현재 쓰기 권한자 목록 전송
                CodeMessage writableListMessage = CodeMessage.builder()
                        .type("writerListChanged")
                        .roomId(msg.getRoomId())
                        .targetUserIds(new ArrayList<>(room.getWritableUsers()))
                        .build();
                broadcastRoom(room, writableListMessage);
                break;
            case "codeChange":
                if (room.isWritable(msg.getUserId())) {
                    room.setCode(msg.getContent());
                    saveCodeToDB(msg.getRoomId(), msg.getContent());
                    broadcastRoom(room, msg);
                }
                break;
            case "setWriter":
                if (msg.getTargetUserId() != null) {
                    room.addWritableUser(msg.getUserId(), msg.getTargetUserId());
                } else if (msg.getTargetUserIds() != null) {
                    for (String targetUserId : msg.getTargetUserIds()) {
                        room.addWritableUser(msg.getUserId(), targetUserId);
                    }
                }

                newMsg = CodeMessage.builder()
                        .type("writerListChanged")
                        .roomId(room.getRoomId())
                        .targetUserIds(new ArrayList<>(room.getWritableUsers()))
                        .build();

                broadcastRoom(room, newMsg);
                break;
            case "removeWriter":
                if (msg.getTargetUserId() != null) {
                    room.removeWritableUser(msg.getUserId(), msg.getTargetUserId());
                } else if (msg.getTargetUserIds() != null) {
                    for (String targetUserId : msg.getTargetUserIds()) {
                        room.removeWritableUser(msg.getUserId(), targetUserId);
                    }
                }

                newMsg = CodeMessage.builder()
                        .type("writerListChanged")
                        .roomId(room.getRoomId())
                        .targetUserIds(new ArrayList<>(room.getWritableUsers()))
                        .build();
                broadcastRoom(room, newMsg);
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
//        System.out.println("Saving to DB: roomId=" + roomId + ", content length=" + content.length());
        try {
            roomDocumentRepository.save(
                    RoomDocument.builder()
                            .roomId(roomId)
                            .code(content)
                            .updateAt(LocalDateTime.now())
                            .build()
            );
//            System.out.println("DB 저장 완료");
        } catch (Exception e) {
//            System.err.println("DB 저장 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }



    public Room getOrCreateRoom(String roomId, String ownerId) {
        return rooms.computeIfAbsent(roomId, id -> {
            Room r = new Room(id, ownerId);
            RoomDocument doc = roomDocumentRepository.findById(id).orElse(null);
            if (doc != null) r.setCode(doc.getCode());
            return r;
        });
    }

    public void removeSession(WebSocketSession session) {
        for (Room room: rooms.values()) {
            String leavingUserId = null;

            for (Map.Entry<String, WebSocketSession> entry: room.getParticipants().entrySet()) {
                if (entry.getValue().equals(session)) {
                    leavingUserId = entry.getKey();
                    break;
                }
            }

            if (leavingUserId != null) {
                room.leave(leavingUserId);

                CodeMessage participantListMsg = CodeMessage.builder()
                        .type("participantList")
                        .roomId(room.getRoomId())
                        .participants(new ArrayList<>(room.getParticipants().keySet()))
                        .build();

                CodeMessage ownerMsg = CodeMessage.builder()
                        .type("ownerInfo")
                        .roomId(room.getRoomId())
                        .ownerId(room.getOwnerId())
                        .build();

                CodeMessage writerListMsg = CodeMessage.builder()
                        .type("writerList")
                        .roomId(room.getRoomId())
                        .targetUserIds(new ArrayList<>(room.getWritableUsers()))
                        .build();

                try {
                    broadcastRoom(room, participantListMsg);
                    broadcastRoom(room, ownerMsg);
                    broadcastRoom(room, writerListMsg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }
}
