package com.learning.algolearningjava.service;

import com.learning.algolearningjava.model.RoomDocument;
import com.learning.algolearningjava.repository.RoomDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class RoomDocumentService {

    private final RoomDocumentRepository roomDocumentRepository;

    public RoomDocument saveCode(String roomId, String code) {
        RoomDocument doc = RoomDocument.builder()
                .roomId(roomId)
                .code(code)
                .updateAt(LocalDateTime.now())
                .build();
        return roomDocumentRepository.save(doc);
    }

    public RoomDocument getRoomById(String roomId) {
        return roomDocumentRepository.findById(roomId)
                .orElse(RoomDocument.builder()
                        .roomId(roomId)
                        .code("")
                        .updateAt(LocalDateTime.now())
                        .build());
    }
}
