package com.parkinglist.backend.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@NoArgsConstructor
public class SeoulRealtimeDto {

    // 1. 이 DTO는 "GetParkingInfo" 키를 찾습니다.
    @JsonProperty("GetParkingInfo") 
    private GetParkingInfo getParkingInfo;

    @Getter
    @NoArgsConstructor
    public static class GetParkingInfo {
        @JsonProperty("row")
        private List<Row> row;
    }

    @Getter
    @NoArgsConstructor
    public static class Row {
        // 2. 이 DTO는 매칭에 필요한 '주차장 코드'와
        @JsonProperty("PKLT_CD") // (실제 필드명 확인 필요)
        private String parkingCode;

        // 3. 실시간 정보만 포함합니다.
        @JsonProperty("TPKCT")
        private Integer capacity; 

        @JsonProperty("NOW_PRK_VHCL_CNT")
        private Integer curParking; 

        // ▼▼▼ [신규] 이 필드를 추가해주세요. ▼▼▼
        @JsonProperty("PKLT_NM") // (이것도 실제 필드명인지 확인 필요)
        private String parkingName;
        // ▲▲▲ [신규] 추가 완료 ▲▲▲
    }
}
