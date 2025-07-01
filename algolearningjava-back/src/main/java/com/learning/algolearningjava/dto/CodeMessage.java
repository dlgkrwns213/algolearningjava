package com.learning.algolearningjava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CodeMessage {
    private String type;
    private String roomId;
    private String userId;
    private String ownerId;
    private String content;

    private List<String> participants;
    private String targetUserId;
    private List<String> targetUserIds;
}
