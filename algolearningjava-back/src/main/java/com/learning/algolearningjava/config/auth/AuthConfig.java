package com.learning.algolearningjava.config.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class AuthConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // WebSocket 사용 시 CSRF 보통 비활성화
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll() // WebSocket 허용
                        .anyRequest().permitAll()              // 전체 허용 (테스트 목적)
                );
        return http.build();
    }
}
