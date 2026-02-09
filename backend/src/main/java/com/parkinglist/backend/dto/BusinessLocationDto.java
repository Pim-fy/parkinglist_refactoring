package com.parkinglist.backend.dto;

import java.math.BigDecimal;

import com.parkinglist.backend.entity.BusinessLocation;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BusinessLocationDto {

    /**
     * 프론트엔드 -> 백엔드
     * 사업장 신규 등록 요청 DTO
     * (사용자님이 제안하신 '주소 수동 입력' 방식은 우선 Tmap 검색으로 좌표를 찾는 로직이 필요하므로,
     * 우선 사업장 이름, 주소, 좌표를 모두 받는 것으로 구현합니다.)
     */
    @Getter
    @NoArgsConstructor
    public static class Request {
        private String locationName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;
    }

    /**
     * 백엔드 -> 프론트엔드
     * 내 사업장 목록 조회 응답 DTO
     */
    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long id; // BusinessLocation의 고유 ID (pk)
        private String locationName;
        private String address;
        private BigDecimal latitude;
        private BigDecimal longitude;

        // 엔티티 -> DTO 변환 생성자
        public Response(BusinessLocation location) {
            this.id = location.getId();
            this.locationName = location.getLocationName();
            this.address = location.getAddress();
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
        }
    }
}
