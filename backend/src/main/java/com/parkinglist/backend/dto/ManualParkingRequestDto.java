package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ManualParkingRequestDto {
    private String parkingName; // 사용자가 입력한 이름
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String tmapPkey;      // Tmap PKey (선택적)
    private String additionalText; // 추가 정보 (선택적)
}
