package com.learning.algolearningjava.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomJoinRequestDto {
    private String roomId;
    private String userId;
    private String userName;
}
