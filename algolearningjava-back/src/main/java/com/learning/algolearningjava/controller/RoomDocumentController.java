package com.learning.algolearningjava.controller;

import com.learning.algolearningjava.dto.CodeUpdateRequest;
import com.learning.algolearningjava.model.RoomDocument;
import com.learning.algolearningjava.service.RoomDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor
public class RoomDocumentController {

    private final RoomDocumentService roomDocumentService;

    @GetMapping("/{roomId}")
    public RoomDocument getRoomDocument(
            @PathVariable String roomId
    ) {
        return roomDocumentService.getRoomById(roomId);
    }

    @PutMapping("/{roomId}")
    public RoomDocument updateRoomDocument(
            @PathVariable String roomId,
            @RequestBody CodeUpdateRequest request
    ) {
        return roomDocumentService.saveCode(roomId, request.getCode());
    }
}
