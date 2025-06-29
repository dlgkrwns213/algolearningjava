package com.learning.algolearningjava.controller;

import com.learning.algolearningjava.dto.RoomJoinRequestDto;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
public class RoomController {

    @PostMapping("/join")
    public String joinRoom(
            @RequestBody RoomJoinRequestDto request,
            HttpSession session
    ) {
        session.setAttribute("roomId", request.getRoomId());
        session.setAttribute("userId", request.getUserId());
        session.setAttribute("userName", request.getUserName());

        return "joined";
    }

    @GetMapping("/current")
    public RoomJoinRequestDto getCurrentSession(HttpSession session) {
        RoomJoinRequestDto dto = new RoomJoinRequestDto();
        dto.setRoomId((String) session.getAttribute("roomId"));
        dto.setUserId((String) session.getAttribute("userId"));
        dto.setUserName((String) session.getAttribute("userName"));
        return dto;
    }
}
