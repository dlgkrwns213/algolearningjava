package com.learning.algolearningjava.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDocument {
    @Id
    private String roomId;
    private String code;
    private LocalDateTime updateAt;
}
