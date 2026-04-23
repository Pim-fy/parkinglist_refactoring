package com.parkinglist.backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 필터 옵션 DTO.
public class ParkingFilterDto {
    private boolean isFree = false;
    private boolean isPublic = false;
}
