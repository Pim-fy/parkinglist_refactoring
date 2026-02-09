package com.parkinglist.backend.service;

import com.parkinglist.backend.dto.BusinessLocationDto;
import com.parkinglist.backend.dto.BusinessParkingDto;
import com.parkinglist.backend.dto.ManualParkingRequestDto;
import com.parkinglist.backend.dto.ParkingInfoUpdateDto;
import com.parkinglist.backend.entity.BusinessInfo;
import com.parkinglist.backend.entity.BusinessLocation;
import com.parkinglist.backend.entity.BusinessParking;
import com.parkinglist.backend.entity.IntegratedParking;
import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.repository.BusinessInfoRepository;
import com.parkinglist.backend.repository.BusinessLocationRepository;
import com.parkinglist.backend.repository.BusinessParkingRepository;
import com.parkinglist.backend.repository.IntegratedParkingRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BusinessService {

    private final BusinessParkingRepository businessParkingRepository;
    private final BusinessInfoRepository businessInfoRepository;
    private final IntegratedParkingRepository integratedParkingRepository;
    private final BusinessLocationRepository businessLocationRepository;
    private final ParkingService parkingService;

    // 사업장 등록
    @Transactional
    public void registerBusinessLocation(BusinessLocationDto.Request requestDto, User businessUser) {
        // DTO -> Entity 변환
        BusinessLocation newLocation = BusinessLocation.builder()
                .businessUser(businessUser) // 소유자 연결
                .locationName(requestDto.getLocationName())
                .address(requestDto.getAddress())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .build();
        
        // DB에 저장
        businessLocationRepository.save(newLocation);
    }

    // 사업장 목록 조회
    @Transactional(readOnly = true)
    public List<BusinessLocationDto.Response> getMyBusinessLocations(User businessUser) {
        // 현재 로그인한 사용자가 소유한 모든 사업장 목록을 조회
        List<BusinessLocation> locations = businessLocationRepository.findByBusinessUser(businessUser);
        
        // Entity List -> DTO List 변환하여 반환
        return locations.stream()
                .map(BusinessLocationDto.Response::new) // 생성자 참조
                .collect(Collectors.toList());
    }


    // 사업장 추천 주차장 등록
    @Transactional // readOnly=false
    public void registerBusinessParkingToLocation(Long locationId, BusinessParkingDto.Request requestDto, User businessUser) {
        
        // 사업장 조회 및 소유권 확인
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // parkingId로 IntegratedParking 엔티티 조회
        IntegratedParking parking = integratedParkingRepository.findById(requestDto.getParkingId())
                .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + requestDto.getParkingId()));

        // BusinessParking 엔티티 생성 및 저장
        BusinessParking newBusinessParking = BusinessParking.builder()
                .businessLocation(location) 
                .integratedParking(parking) 
                .businessUserNumber(location.getBusinessUser().getUserNumber())
                .build();
        
        businessParkingRepository.save(newBusinessParking);

        // BusinessInfo 엔티티 생성 및 저장
        if (requestDto.getAdditionalText() != null && !requestDto.getAdditionalText().isEmpty()) {
            Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);
            if (existingInfo.isPresent()) {
                // 이미 정보가 있으면, 텍스트만 업데이트
                BusinessInfo infoToUpdate = existingInfo.get();
                infoToUpdate.updateText(requestDto.getAdditionalText()); 
            } else {
                // 정보가 없으면, 새로 생성
                BusinessInfo newInfo = BusinessInfo.builder()
                        .integratedParking(parking) // 1:1 관계 연결
                        .additionalText(requestDto.getAdditionalText())
                        .build();
            
                businessInfoRepository.save(newInfo);
            }
        }
    }

    @Transactional
    public void registerManualParkingToLocation(Long locationId, ManualParkingRequestDto requestDto, User businessUser) {
        
        // 1. 사업장 조회 및 소유권 확인 (기존 로직 재사용)
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 2. ParkingService를 호출하여 IntegratedParking 엔티티를 가져오거나 생성
        //    (좌표, PKey, 이름, 주소 등 모든 정보를 넘겨줌)
        IntegratedParking parking = parkingService.getOrCreateManualParking(requestDto);

        // 3. BusinessParking 엔티티 생성 및 저장 (기존 로직 재사용)
        BusinessParking newBusinessParking = BusinessParking.builder()
                .businessLocation(location) 
                .integratedParking(parking) 
                .businessUserNumber(location.getBusinessUser().getUserNumber())
                .build();
        
        businessParkingRepository.save(newBusinessParking);

        // 4. BusinessInfo 엔티티 생성 및 저장 (기존 로직 재사용)
        if (requestDto.getAdditionalText() != null && !requestDto.getAdditionalText().isEmpty()) {
            Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);
            if (existingInfo.isPresent()) {
                existingInfo.get().updateText(requestDto.getAdditionalText());
            } else {
                BusinessInfo newInfo = BusinessInfo.builder()
                        .integratedParking(parking)
                        .additionalText(requestDto.getAdditionalText())
                        .build();
                businessInfoRepository.save(newInfo);
            }
        }
    }

    // 사업장의 추천 주차장 목록 조회
    @Transactional(readOnly = true)
    public List<BusinessParkingDto.Response> getParkingsForLocation(Long locationId, User businessUser) {

        // 사업장 조회 및 소유권 확인
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 해당 사업장에 연결된 주차장 목록 조회 (★ findByBusinessUser -> findByBusinessLocation)
        List<BusinessParking> parkings = businessParkingRepository.findByBusinessLocation(location);
        
        // DTO로 변환
        return parkings.stream()
                .map(parking -> {
                    // 관련된 BusinessInfo를 추가로 조회
                    BusinessInfo info = businessInfoRepository
                            .findByIntegratedParking(parking.getIntegratedParking())
                            .orElse(null);

                    // 수정된 DTO 생성자 호출
                    return new BusinessParkingDto.Response(parking, info);
                })
                .collect(Collectors.toList());
    }

    // 사업장 ID로 엔티티를 조회하고, 현재 로그인한 사용자가 소유자인지 검증하는 내부 메서드
    private BusinessLocation findLocationByIdAndVerifyOwner(Long locationId, User currentUser) {
        // ID로 사업장 엔티티를 찾음
        BusinessLocation location = businessLocationRepository.findById(locationId)
                .orElseThrow(() -> new EntityNotFoundException("사업장 정보를 찾을 수 없습니다: " + locationId));

        // 해당 사업장의 소유자(User) ID와 현재 로그인한 사용자의 ID를 비교
        if (!Objects.equals(location.getBusinessUser().getUserNumber(), currentUser.getUserNumber())) {
            // 소유자가 다르면 AccessDeniedException 발생 (403 Forbidden)
            throw new AccessDeniedException("이 사업장에 접근할 권한이 없습니다.");
        }
        
        // 소유자가 맞으면 해당 사업장 엔티티 반환
        return location;
    }

    // [신규] 주차장 추가 정보 업데이트 로직
    @Transactional
    public void updateParkingInfo(Long locationId, String parkingId, ParkingInfoUpdateDto requestDto, User businessUser) {
        
        // 1. (보안) 이 사용자가 해당 사업장(locationId)의 소유주가 맞는지 확인
        findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 2. parkingId로 IntegratedParking 엔티티 조회
        IntegratedParking parking = integratedParkingRepository.findById(parkingId)
                .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + parkingId));

        // 3. BusinessInfo 엔티티 조회
        Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);

        String newText = requestDto.getAdditionalText();

        if (existingInfo.isPresent()) {
            // 4. [업데이트] 이미 정보가 있으면, 텍스트만 업데이트
            BusinessInfo infoToUpdate = existingInfo.get();
            infoToUpdate.updateText(newText);
        } else {
            // 5. [신규 생성] 정보가 없었는데, 새로 텍스트를 입력한 경우
            if (newText != null && !newText.isEmpty()) {
                BusinessInfo newInfo = BusinessInfo.builder()
                        .integratedParking(parking)
                        .additionalText(newText)
                        .build();
                businessInfoRepository.save(newInfo);
            }
        }
    }

    // [신규] 추천 주차장 (연결) 삭제 로직
    @Transactional
    public void deleteBusinessParking(Long businessParkingId, User businessUser) {
        
        // 1. ID로 BusinessParking (연결) 엔티티를 조회
        BusinessParking parkingLink = businessParkingRepository.findById(businessParkingId)
                .orElseThrow(() -> new EntityNotFoundException("해당 추천 주차장 정보를 찾을 수 없습니다."));

        // 2. (보안) 현재 로그인한 유저가 해당 '연결'을 등록한 소유주가 맞는지 확인
        // (BusinessParking 테이블에는 businessUserNumber가 저장되어 있음)
        if (!Objects.equals(parkingLink.getBusinessUserNumber(), businessUser.getUserNumber())) { 
             throw new AccessDeniedException("이 항목을 삭제할 권한이 없습니다.");
        }

        // 3. 엔티티 삭제
        businessParkingRepository.delete(parkingLink);
    }

    // [신규] 사업장 삭제 로직
    @Transactional
    public void deleteBusinessLocation(Long locationId, User businessUser) {
        
        // 1. (보안) 사업장 조회 및 소유권 확인
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 2. (중요) 이 사업장에 연결된 "추천 주차장 목록(BusinessParking)"을 먼저 조회
        List<BusinessParking> parkingsToDelete = businessParkingRepository.findByBusinessLocation(location);

        // 3. (중요) 연결된 "추천 주차장" 먼저 모두 삭제
        if (!parkingsToDelete.isEmpty()) {
            businessParkingRepository.deleteAll(parkingsToDelete);
        }

        // 4. "사업장(BusinessLocation)" 본체 삭제
        businessLocationRepository.delete(location);
    }

}
