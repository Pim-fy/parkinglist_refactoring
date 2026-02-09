package com.parkinglist.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.parkinglist.backend.dto.ParkingDetailResponseDto;
import com.parkinglist.backend.dto.ParkingLogRequestDto;
import com.parkinglist.backend.dto.ParkingRecommendationRequestDto;
import com.parkinglist.backend.dto.ParkingResponseDto;
import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.service.ParkingService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking")
public class ParkingController {
    private final ParkingService parkingService;

    @PostMapping("/log")
    @ResponseStatus(HttpStatus.CREATED)         // 성공 시 201 Created 응답
    public void saveParkingLog(
        @RequestBody ParkingLogRequestDto requestDto,
        @AuthenticationPrincipal User user      // 비로그인 시 user는 null이 됨
    ) {
        // 실제 로그인된 user 객체를 서비스로 전달
        parkingService.saveLogAndCount(requestDto, user);       
    }

    @PostMapping("/recommend")
    public ResponseEntity<List<ParkingResponseDto>> getRecommendations(
            @RequestBody ParkingRecommendationRequestDto requestDto) {
        
        // 이 유효성 검사는 목적지 좌표까지 확인하도록 강화하는 것이 좋습니다.
        if (requestDto.getTmapParkingList() == null || 
            requestDto.getFilter() == null ||
            requestDto.getDestinationLat() == null || // 목적지 좌표 필수
            requestDto.getDestinationLon() == null) { // 목적지 좌표 필수
            return ResponseEntity.badRequest().build(); 
        }

        // ★★★ DTO 객체 전체를 전달하도록 수정 ★★★
        List<ParkingResponseDto> recommendations = parkingService.getRecommendations(requestDto);
        
        return ResponseEntity.ok(recommendations);
    }

    //  [신규] 상세 정보 API 엔드포인트 추가 
    @GetMapping("/details/{parkingId}")
    public ResponseEntity<ParkingDetailResponseDto> getParkingDetails(
            @PathVariable String parkingId
    ) {
        ParkingDetailResponseDto details = parkingService.getParkingDetails(parkingId);
        return ResponseEntity.ok(details);
    }
}
