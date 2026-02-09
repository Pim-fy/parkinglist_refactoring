package com.parkinglist.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class SeoulParkingDto {
    // API 응답의 최상위 객체 (예: "GetParkInfo" 또는 "GetParkingInfo")
    // ★ 샘플 URL의 "GetParkInfo"를 기준으로 작성했습니다.
    @JsonProperty("GetParkInfo") 
    private GetParkInfo getParkInfo;

    @Getter
    @NoArgsConstructor
    public static class GetParkInfo {
        @JsonProperty("row")
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    public static class Row {
        // --- 좌표 매칭용 필드 ---
        @JsonProperty("LAT")
        private Double latitude; // 위도

        @JsonProperty("LOT")
        private Double longitude; // 경도

        // ▼▼▼ [신규] 코드 매칭용 '주차장 코드' 필드 추가 ▼▼▼
        @JsonProperty("PKLT_CD") 
        private String parkingCode;
        // ▲▲▲ [신규] 추가 완료 ▲▲▲
        
        // --- 상세 정보 필드 ---
        @JsonProperty("PKLT_NM")
        private String parkingName;

        @JsonProperty("PRK_CRG")
        private Double rates; // 기본 요금

        @JsonProperty("PRK_HM")
        private Double timeRate; // 기본 시간(분)

        @JsonProperty("ADD_CRG")
        private Double addRates; // 추가 단위 요금

        @JsonProperty("ADD_UNIT_TM_MNT")
        private Double addTimeRate; // 추가 단위 시간(분)

        @JsonProperty("CHGD_FREE_SE")
        private String payYn; // 유/무료 (Y/N)

        @JsonProperty("WD_OPER_BGNG_TM")
        private String weekdayBeginTime; // 평일 운영 시작

        @JsonProperty("WD_OPER_END_TM")
        private String weekdayEndTime; // 평일 운영 종료
    }
}
