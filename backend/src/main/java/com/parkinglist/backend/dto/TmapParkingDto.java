package com.parkinglist.backend.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TmapParkingDto {
    private String tmapPkey;
    private String name;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance;
}
