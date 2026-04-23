package com.parkinglist.backend.dto;

import java.math.BigDecimal;

import com.parkinglist.backend.entity.BusinessLocation;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BusinessLocationDto {
    
    // 사업장 신규 등록 요청 DTO.
    // 사업장 이름, 주소, 좌표(위,경도).
    @Getter
    @NoArgsConstructor
    public static class Request {
        private String locationName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }


    // 내 사업장 목록 조회 응답 DTO.
    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long id; // BusinessLocation의 고유 ID.
        private String locationName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;

        // 엔티티 -> DTO 변환 생성자.
        public Response(BusinessLocation location) {
            this.id = location.getId();
            this.locationName = location.getLocationName();
            this.address = location.getAddress();
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
        }
    }
}
