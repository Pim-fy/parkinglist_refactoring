package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// Tmap 추천 원본 주차장 1건을 담는 요청 DTO.
public class TmapParkingRequestDto {
    // 필수.
    private String tmapPkey;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    // 선택.
    private Boolean isPublic;
    private Boolean isFree;
    // 선택.
    private Double distance;
}
