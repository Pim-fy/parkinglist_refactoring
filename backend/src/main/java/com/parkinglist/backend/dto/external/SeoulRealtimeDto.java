package com.parkinglist.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SeoulRealtimeDto {

    // 필드 선언.
    // JsonProperty : JSON의 GetParkingInfo 키를 해당 필드에 매핑하라.
    @JsonProperty("GetParkingInfo") 
    private ParkingInfo parkingInfo;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParkingInfo {
        @JsonProperty("row")
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        // 매칭에 필요한 주차장 코드와 실시간 정보만 포함됨.

        // 매칭용 주차장 코드.
        @JsonProperty("PKLT_CD")
        private String parkingCode;

        // 총 주차 면수.
        @JsonProperty("TPKCT")
        private Integer capacity; 

        // 현재 주차 대수.
        @JsonProperty("NOW_PRK_VHCL_CNT")
        private Integer curParking; 

        // 주차장 이름.
        @JsonProperty("PKLT_NM")
        private String parkingName;
    }
}
