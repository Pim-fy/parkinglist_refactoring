package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 프론트엔드 -> 백엔드로 주차장 추천 최종 요청 DTO. 
public class ParkingRecommendationRequestDto {
    // Tmap 검색 결과 원본 주차장 목록.
    private List<TmapParkingRequestDto> tmapParkingList;
    // 목적지 사업장 식별자.
    private Long destinationLocationId;
    // 필터 옵션.
    private ParkingFilterDto filter;
    // 목적지 좌표.
    private BigDecimal destinationLat;      // 위도.
    private BigDecimal destinationLon;      // 경도.
}
