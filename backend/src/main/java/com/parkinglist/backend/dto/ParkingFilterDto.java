package com.parkinglist.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

// 필터 옵션 DTO
@Getter
@NoArgsConstructor
public class ParkingFilterDto {
    private boolean isFree = false;
    private boolean isPublic = false;
}
