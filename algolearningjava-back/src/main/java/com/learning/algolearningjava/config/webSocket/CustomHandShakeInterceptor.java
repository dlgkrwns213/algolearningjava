package com.learning.algolearningjava.config.webSocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import java.util.Map;

public class CustomHandShakeInterceptor extends HttpSessionHandshakeInterceptor {
    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {

        // 쿼리 파라미터에서 roomId, userId 추출하여 attributes에 저장
        String query = request.getURI().getQuery(); // e.g. roomId=1234&userId=user-9999
        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    attributes.put(kv[0], kv[1]);
                }
            }
        }

        return super.beforeHandshake(request, response, wsHandler, attributes);
    }
}
