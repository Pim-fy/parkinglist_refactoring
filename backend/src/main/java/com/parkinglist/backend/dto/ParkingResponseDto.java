package com.parkinglist.backend.dto;

import java.math.BigDecimal;

import com.parkinglist.backend.entity.IntegratedParking;

import lombok.Builder;
import lombok.Getter;

// 백 -> 프론트로 반환할 모든 상세 정보가 포함된 최종 주차장 DTO
@Getter
@Builder
public class ParkingResponseDto {
    private String parkingId;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private boolean isFree;
    private boolean isPublic;

    private boolean isRecommended;
    private int selectionCount;
    private double distance;
    private String additionalText;

    public static ParkingResponseDto from (
        IntegratedParking parking,
        boolean isRecommended,
        int selectionCount,
        double distance,
        String additionalText
    ) {
        return ParkingResponseDto.builder()
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
