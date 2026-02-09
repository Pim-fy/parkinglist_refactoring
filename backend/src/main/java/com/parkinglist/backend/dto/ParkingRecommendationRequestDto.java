package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 프론트 -> 백으로 주차장 추천을 요청할 때 보낼 최종 DTO

@Getter
@NoArgsConstructor
public class ParkingRecommendationRequestDto {
    // tmap api에서 받은 원본 목록
    private List<TmapParkingDto> tmapParkingList;
    // 필터 옵션
    private ParkingFilterDto filter;
    // private Long destinationLocationId;      // 주석 처리 추후 처리 필수.
    private BigDecimal destinationLat;
    private BigDecimal destinationLon;
}
