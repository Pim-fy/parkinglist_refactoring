package com.parkinglist.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParkingDetailResponseDto {

    // --- 1. 우리 DB 정보 (기본 정보) ---
    private String parkingId;
    private String name;
    private String address;
    private String additionalText; // (사업자 정보)
    private int selectionCount; // (누적 선택 횟수)

    // --- 2. 서울시 API 정보 (가공됨) ---
    private Integer availableSpots; // 실시간 주차 가능 대수
    private String priceInfo;       // 예: "30분 1500원, 추가 10분 500원"
    private String operatingHours;  // 예: "평일 09:00~18:00"
    
    // (향후 KEPCO API 등을 위한 확장 필드 예시)
    // private Integer evChargerTotalCount;
    // private Integer evChargerAvailableCount;
}
