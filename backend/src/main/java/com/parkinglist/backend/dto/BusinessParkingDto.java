package com.parkinglist.backend.dto;

import com.parkinglist.backend.entity.BusinessInfo;
import com.parkinglist.backend.entity.BusinessParking;

import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class BusinessParkingDto {
    @Getter
    @NoArgsConstructor
    public static class Request {
        private String parkingId;
        private String additionalText;
    }

    @Getter
    public static class Response {
        private Long id; 
        private String parkingId;
        private String parkingName;
        private String parkingAddress;
        private String additionalText;

        // 기본 정보 생성
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
