package com.learning.algolearningjava.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CodeMessage {
    private String type;
    private String roomId;
    private String userId;
    private String content;
}
