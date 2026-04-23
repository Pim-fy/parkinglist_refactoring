package com.parkinglist.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)         // 모르는 데이터가 들어와도 무시. 원하는 필드만 추출해서 사용.
public class SeoulParkingDto {

    // 필드 선언
    // JsonProperty : JSON의 GetParkInfo 키를 해당 필드에 매핑하라.
    @JsonProperty("GetParkInfo") 
    private ParkInfo parkInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParkInfo {
        @JsonProperty("row")
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        // 좌표 매칭용 필드.
        @JsonProperty("LAT")
        private Double latitude;            // 위도

        @JsonProperty("LOT")
        private Double longitude;           // 경도

        @JsonProperty("PKLT_CD") 
        private String parkingCode;         // 주차장 코드.
        
        // 상세 정보 필드.
        @JsonProperty("PKLT_NM")
        private String parkingName;

        @JsonProperty("PRK_CRG")
        private Double rates;               // 기본 요금(원).

        @JsonProperty("PRK_HM")
        private Double timeRate;            // 기본 시간(분).

        @JsonProperty("ADD_CRG")
        private Double addRates;            // 추가 단위 요금(원).

        @JsonProperty("ADD_UNIT_TM_MNT")
        private Double addTimeRate;         // 추가 단위 시간(분).

        @JsonProperty("CHGD_FREE_SE")
        private String payYn;               // 유/무료 (Y/N).

        @JsonProperty("WD_OPER_BGNG_TM")
        private String weekdayBeginTime;    // 평일 운영 시작 시각(HHmm)

        @JsonProperty("WD_OPER_END_TM")
        private String weekdayEndTime;      // 평일 운영 종료 시각(HHmm)
    }
}
