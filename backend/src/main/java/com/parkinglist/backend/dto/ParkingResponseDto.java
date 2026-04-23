package com.parkinglist.backend.dto;

import java.math.BigDecimal;

import com.parkinglist.backend.entity.IntegratedParking;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
// 백엔드 -> 프론트엔드로 반환할 모든 상세 정보가 포함된 최종 주차장 DTO.
public class ParkingResponseDto {
    // 필수. 기본 정보.
    private String parkingId;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // 필수. 필터 옵션.
    private boolean isFree;
    private boolean isPublic;

    // 필수. 세부 정보.
    private boolean isRecommended;
    private int selectionCount;
    private double distance;
    private String additionalText;

    // 여러 값을 조합해 추천 응답 DTO를 생성하는 정적 팩토리 메서드.
    public static ParkingResponseDto forRecommendation (
        IntegratedParking parking,
        boolean isRecommended,
        int selectionCount,
        double distance,
        String additionalText
    ) {
        return ParkingResponseDto
            .builder()
            .parkingId(parking.getParkingId())
            .name(parking.getName())
            .address(parking.getAddress())
            .latitude(parking.getLatitude())
            .longitude(parking.getLongitude())
            .isFree(parking.isFree())
            .isPublic(parking.isPublic())
            .isRecommended(isRecommended)
            .selectionCount(selectionCount)
            .distance(distance)
            .additionalText(additionalText)
            .build();
    }
}
