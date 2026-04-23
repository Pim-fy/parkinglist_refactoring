package com.parkinglist.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 주차장 선택 로그 저장을 위한 DTO.
public class ParkingLogRequestDto {
    @NotBlank(message = "parkingId는 필수입니다.")
    private String parkingId;

    @Size(max = 100, message = "최종 선택 목적지(SelectedDestinationName)는 100자 이하여야 합니다.")
    private String selectedDestinationName;
}
