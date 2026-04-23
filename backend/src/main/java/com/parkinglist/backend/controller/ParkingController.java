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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parking")
public class ParkingController {

    // 의존성 주입.
    private final ParkingService parkingService;

    // 주차장 선택 로그 저장.
    @PostMapping("/log")
    @ResponseStatus(HttpStatus.CREATED)         // 성공 시 201 Created 응답.
    public void saveParkingLog(
        @Valid @RequestBody ParkingLogRequestDto requestDto,
        @AuthenticationPrincipal User user      // 비로그인 시 user는 null이 됨.
    ) {
        // 서비스 로직 호출.
        parkingService.saveLogAndCount(requestDto, user);       
    }

    // 주차장 추천 결과 목록 요청, 내부 동기화 포함.
    @PostMapping("/recommend")
    public ResponseEntity<List<ParkingResponseDto>> getRecommendations(
        @RequestBody ParkingRecommendationRequestDto requestDto) {
        
        // 요청 데이터 유효성 검증.
        // 필수 값 누락 시 400 Bad Request 반환.
        if (requestDto.getTmapParkingList() == null || 
            requestDto.getFilter() == null ||
            requestDto.getDestinationLat() == null || // 목적지 좌표 필수
            requestDto.getDestinationLon() == null) { // 목적지 좌표 필수
            return ResponseEntity.badRequest().build(); 
        }

        // 서비스 로직 호출.
        List<ParkingResponseDto> recommendations = parkingService.getRecommendations(requestDto);
        return ResponseEntity.ok(recommendations);
    }

    // 주차장 상세 정보 조회.
    @GetMapping("/details/{parkingId}")
    public ResponseEntity<ParkingDetailResponseDto> getParkingDetails(
        @PathVariable String parkingId
    ) {
        // 서비스 로직 호출.
        ParkingDetailResponseDto details = parkingService.getParkingDetails(parkingId);
        return ResponseEntity.ok(details);
    }
}
