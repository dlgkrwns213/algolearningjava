package com.learning.algolearningjava.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.algolearningjava.dto.CodeMessage;
import com.learning.algolearningjava.model.Room;
import com.learning.algolearningjava.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.logging.SocketHandler;

@Component
@RequiredArgsConstructor
public class CodeSocketHandler implements WebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomService roomService;

    @Override
    public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) throws Exception {
        // HttpSession에서 복사된 속성 확인
        String roomId = (String) session.getAttributes().get("roomId");
        String userId = (String) session.getAttributes().get("userId");

        System.out.println("WebSocket 연결됨: roomId=" + roomId + ", userId=" + userId);

        // RoomService를 통해 방에 참가
        Room room = roomService.getOrCreateRoom(roomId);
        room.join(userId, session);
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        CodeMessage msg = objectMapper.readValue(payload, CodeMessage.class);

        String roomId = msg.getRoomId();
        Room room = roomService.getOrCreateRoom(roomId);

        switch (msg.getType()) {
            case "codeChange":
                // 같은 방에 있는 다른 사용자에게 코드 변경 내용 전송
                room.broadcast(session, payload);
                break;

            case "grantWrite":
                room.grantWrite(msg.getUserId());
                room.broadcast(session, payload); // 권한 부여 알림
                break;

            case "revokeWrite":
                room.revokeWrite(msg.getUserId());
                room.broadcast(session, payload); // 권한 회수 알림
                break;

            default:
                System.out.println("알 수 없는 메시지 타입: " + msg.getType());
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        roomService.removeSession(session);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
