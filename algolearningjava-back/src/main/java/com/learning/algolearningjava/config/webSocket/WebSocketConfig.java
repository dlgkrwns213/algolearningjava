package com.learning.algolearningjava.config.webSocket;

import com.learning.algolearningjava.socket.CodeSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final CodeSocketHandler codeSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(codeSocketHandler, "/ws/code")
                .setAllowedOrigins("*")
                .addInterceptors(new CustomHandShakeInterceptor());
    }
}
