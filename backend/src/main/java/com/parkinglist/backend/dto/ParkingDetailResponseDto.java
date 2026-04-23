package com.parkinglist.backend.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ParkingDetailResponseDto {

    // 백엔드 DB 정보.
    private String parkingId;
    private String name;
    private String address;
    private String additionalText;      // 사업자 제공 추가 정보.
    private int selectionCount;         // 주차장 누적 선택 횟수.

    // 서울시 API 정보.
    private Integer availableSpots;     // 실시간 주차 가능 대수
    private String priceInfo;           // 가격 정보. 예: "30분 1500원, 추가 10분 500원".
    private String operatingHours;      // 운영 시간 정보. 예:  "평일 09:00~18:00".
    
    // 향후 KEPCO API 등을 위한 확장 필드 예시
    // private Integer evChargerTotalCount;
    // private Integer evChargerAvailableCount;
}
