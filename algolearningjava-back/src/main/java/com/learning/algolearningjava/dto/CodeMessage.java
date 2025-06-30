package com.learning.algolearningjava.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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
    private String content;
}
