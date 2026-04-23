package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 외부 데이터를 꺼내 서비스 로직에 전달하는 용도로만 사용.
public class ManualParkingRequestDto {
    // 필수.
    private String parkingName;             // 사용자가 입력한 주차장 이름.
    private String address;                 // 주소.
    private BigDecimal latitude;            // 좌표(위도).
    private BigDecimal longitude;           // 좌표(경도).
    
    // 선택.
    private String tmapPkey;                // Tmap PK.
    private String additionalText;          // 추가 제공 정보.
}
