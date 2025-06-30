package com.learning.algolearningjava.socket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.algolearningjava.dto.CodeMessage;
import com.learning.algolearningjava.model.Room;
import com.learning.algolearningjava.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.bson.types.Code;
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
        String uri = session.getUri().toString();
        String roomId = getQueryParam(uri, "roomId");
        String userId = getQueryParam(uri, "userId");

        System.out.println("✅ WebSocket 연결됨: roomId=" + roomId + ", userId=" + userId);


        if (roomId == null || userId == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        session.getAttributes().put("roomId", roomId);
        session.getAttributes().put("userId", userId);

        System.out.println("WebSocket 연결됨: roomId=" + roomId + ", userId=" + userId);

        CodeMessage joinMsg = CodeMessage.builder()
                .roomId(roomId)
                .userId(userId)
                .type("join")
                .build();

        roomService.processMessage(session, joinMsg );
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        System.out.println("🔥 WebSocket 메시지 수신됨: " + message.getPayload());

        String payload = message.getPayload().toString();
        CodeMessage codeMessage = objectMapper.readValue(payload, CodeMessage.class);

        roomService.processMessage(session, codeMessage);
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

    private String getQueryParam(String uri, String key) {
        String[] parts = uri.split("\\?");

        if (parts.length < 2) return null;

        String[] params = parts[1].split("&");
        for (String param : params) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(key)) return keyValue[1];
        }

        return null;
    }
}
