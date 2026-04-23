package com.parkinglist.backend.dto;

import com.parkinglist.backend.entity.BusinessInfo;
import com.parkinglist.backend.entity.BusinessParking;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BusinessParkingDto {

    // 사업장 추천 주차장 등록 요청 DTO.
    @Getter
    @NoArgsConstructor
    public static class Request {
        private String parkingId;
        private String additionalText;
    }

    // 사업장 추천 주차장 목록 조회 응답 DTO.
    @Getter
    public static class Response {
        private Long id; 
        private String parkingId;
        private String parkingName;
        private String parkingAddress;
        private String additionalText;

        // 기본 정보 생성.
        public Response(BusinessParking parkingEntity, BusinessInfo infoEntity) {
            this.id = parkingEntity.getId();
            this.parkingId = parkingEntity.getIntegratedParking().getParkingId();
            this.parkingName = parkingEntity.getIntegratedParking().getName();
            this.parkingAddress = parkingEntity.getIntegratedParking().getAddress();
            
            if (infoEntity != null) {
                this.additionalText = infoEntity.getAdditionalText();
            }
        }
    }
}
