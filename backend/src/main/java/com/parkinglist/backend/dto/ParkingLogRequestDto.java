package com.parkinglist.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ParkingLogRequestDto {
    // 주차장 선택 로그 저장 위해 백엔드로 보낼 DTO
    private String parkingId;
    private String selectedDestinationName;
}
