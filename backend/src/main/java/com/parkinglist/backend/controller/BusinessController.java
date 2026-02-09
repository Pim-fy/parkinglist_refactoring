package com.parkinglist.backend.controller;

import com.parkinglist.backend.dto.BusinessLocationDto;
import com.parkinglist.backend.dto.BusinessParkingDto;
import com.parkinglist.backend.dto.ManualParkingRequestDto;
import com.parkinglist.backend.dto.ParkingInfoUpdateDto;
import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.entity.enums.UserRole;
import com.parkinglist.backend.service.BusinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/business")
public class BusinessController {

    private final BusinessService businessService;

    // 사업장 등록
    @PostMapping("/location")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> registerLocation(
            @RequestBody BusinessLocationDto.Request requestDto,
            @AuthenticationPrincipal User user
    ) {
        // 사업자 검증
        if (user.getRole() != UserRole.ROLE_BUSINESS) {
            throw new AccessDeniedException("사업자 회원만 이 기능을 사용할 수 있습니다.");
        }
        
        // 서비스 로직 호출
        businessService.registerBusinessLocation(requestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body("사업장이 성공적으로 등록되었습니다.");
    }

    // 내 사업장 목록 조회
    @GetMapping("/location")
    public ResponseEntity<List<BusinessLocationDto.Response>> getMyLocations(
            @AuthenticationPrincipal User user
    ) {
        // 사업자 검증
        if (user.getRole() != UserRole.ROLE_BUSINESS) {
            throw new AccessDeniedException("사업자 회원만 이 기능을 사용할 수 있습니다.");
        }

        // 서비스 로직 호출
        List<BusinessLocationDto.Response> myList = businessService.getMyBusinessLocations(user);
        return ResponseEntity.ok(myList);
    }

    // 특정 사업장에 추천 주차장 등록
    @PostMapping("/location/{locationId}/parking")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> registerParkingToLocation(
            @PathVariable Long locationId, // ★ (추가) URL 경로에서 locationId 받기
            @RequestBody BusinessParkingDto.Request requestDto,
            @AuthenticationPrincipal User user
    ) {
        // (사업자 검증은 서비스 레이어의 findLocationByIdAndVerifyOwner에서 자동으로 처리됨)
        
        // 서비스 로직 호출
        businessService.registerBusinessParkingToLocation(locationId, requestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body("주차장이 성공적으로 등록되었습니다.");
    }

    //  특정 사업장의 추천 주차장 목록 조회
    @GetMapping("/location/{locationId}/parking")
    public ResponseEntity<List<BusinessParkingDto.Response>> getParkingsForLocation(
            @PathVariable Long locationId, // ★ (추가) URL 경로에서 locationId 받기
            @AuthenticationPrincipal User user
    ) {
        // (사업자 검증은 서비스 레이어의 findLocationByIdAndVerifyOwner에서 자동으로 처리됨)

        // 서비스 로직 호출
        List<BusinessParkingDto.Response> myList = businessService.getParkingsForLocation(locationId, user);
        return ResponseEntity.ok(myList);
    }

    // [신규] 특정 사업장에 '수동으로' 추천 주차장 등록
    @PostMapping("/location/{locationId}/parking-manual")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<String> registerManualParkingToLocation(
            @PathVariable Long locationId,
            @RequestBody ManualParkingRequestDto requestDto, // [신규] DTO 사용
            @AuthenticationPrincipal User user
    ) {
        businessService.registerManualParkingToLocation(locationId, requestDto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body("주차장이 성공적으로 등록되었습니다.");
    }

    // [신규] 특정 사업장의 추천 주차장 '추가 정보'만 수정
    @PutMapping("/location/{locationId}/parking-info/{parkingId}")
    public ResponseEntity<String> updateParkingInfo(
            @PathVariable Long locationId,
            @PathVariable String parkingId, // IntegratedParking의 ID (UUID)
            @RequestBody ParkingInfoUpdateDto requestDto,
            @AuthenticationPrincipal User user
    ) {
        businessService.updateParkingInfo(locationId, parkingId, requestDto, user);
        return ResponseEntity.ok("주차장 추가 정보가 수정되었습니다.");
    }

    // [신규] 특정 사업장의 추천 주차장 '연결' 삭제
    // (BusinessParking.id를 직접 받음)
    @DeleteMapping("/parking/{businessParkingId}")
    public ResponseEntity<String> deleteBusinessParking(
            @PathVariable Long businessParkingId,
            @AuthenticationPrincipal User user
    ) {
        businessService.deleteBusinessParking(businessParkingId, user);
        return ResponseEntity.ok("추천 주차장이 삭제되었습니다.");
    }

    // [신규] 사업장 삭제 (및 관련 추천 주차장 모두 삭제)
    @DeleteMapping("/location/{locationId}")
    public ResponseEntity<String> deleteBusinessLocation(
            @PathVariable Long locationId,
            @AuthenticationPrincipal User user
    ) {
        businessService.deleteBusinessLocation(locationId, user);
        return ResponseEntity.ok("사업장이 삭제되었습니다.");
    }
}
