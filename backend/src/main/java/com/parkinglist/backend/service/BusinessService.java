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

    // 의존성 주입.
    private final BusinessParkingRepository businessParkingRepository;
    private final BusinessInfoRepository businessInfoRepository;
    private final IntegratedParkingRepository integratedParkingRepository;
    private final BusinessLocationRepository businessLocationRepository;
    private final ParkingService parkingService;

    // 사업장 등록.
    @Transactional
    public void registerBusinessLocation(BusinessLocationDto.Request requestDto, User businessUser) {
        // DTO -> Entity 변환.
        // 빌더 패턴 사용.
        BusinessLocation newLocation = BusinessLocation
            .builder()
            .businessUser(businessUser)         // 소유자 연결
            .locationName(requestDto.getLocationName())
            .address(requestDto.getAddress())
            .latitude(requestDto.getLatitude())
            .longitude(requestDto.getLongitude())
            .build();

        // DB에 저장.
        businessLocationRepository.save(newLocation);
    }

    // 사업장 목록 조회.
    @Transactional(readOnly = true)
    public List<BusinessLocationDto.Response> getMyBusinessLocations(User businessUser) {
        // 현재 로그인한 사용자가 소유한 모든 사업장 목록을 조회.
        // 리스트에 엔터티 형태의 데이터 저장.
        List<BusinessLocation> locations = businessLocationRepository.findByBusinessUser(businessUser);
        
        // Entity List -> DTO List 변환하여 반환.
        // 데이터를 변환하여 리스트로 담을 때 stream - map - collect가 하나의 세트. 중간에 filter 삽입 가능.
        // JS의 map 동작과 JAVA의 map 동작 차이.
        return locations
            .stream()
            .map(BusinessLocationDto.Response::new) // DTO의 생성자 참조.
            .collect(Collectors.toList());
    }

    // 사업장 추천 주차장 등록.
    @Transactional
    public void registerBusinessParkingToLocation(Long locationId, BusinessParkingDto.Request requestDto, User businessUser) {
        
        // 사업장 조회 및 소유권 확인.
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // parkingId로 IntegratedParking 엔터티 조회.
        String targetParkingId = requestDto.getParkingId();
        IntegratedParking parking = integratedParkingRepository
            .findById(targetParkingId)
            .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + targetParkingId));

        // BusinessParking 엔티티 생성 및 저장.
        BusinessParking newBusinessParking = BusinessParking
            .builder()
            .businessLocation(location) 
            .integratedParking(parking) 
            .businessUserNumber(location.getBusinessUser().getUserNumber())
            .build();

        // 메서드 종료시 트랜잭션이 커밋되는 순간 INSERT문이 실행됨.
        businessParkingRepository.save(newBusinessParking);

        // BusinessInfo 엔터티 생성 및 저장.
        // 주차장 등록 요청에 추가 제공 정보가 있는 경우에만 실행.
        if (requestDto.getAdditionalText() != null && !requestDto.getAdditionalText().isEmpty()) {
            // 기존에 등록된 세부 정보(BusinessInfo)가 있는지 확인.
            // 조회된 주차장(IntegratedParking) 엔터티를 기준으로 함.
            Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);
            if (existingInfo.isPresent()) {
                // 이미 기존 정보가 있는 경우 텍스트만 업데이트.

                // get()으로 Optional 상자 안에서 실제 BusinessInfo 엔터티 객체 꺼냄.
                BusinessInfo infoToUpdate = existingInfo.get();
                // 엔터티 객체의 메서드 호출해 업데이트. 
                infoToUpdate.updateText(requestDto.getAdditionalText()); 
            } else {
                // 기존 정보가 없는 경우 BusinessInfo 엔터티 객체 생성.

                BusinessInfo newInfo = BusinessInfo
                    .builder()
                    .integratedParking(parking)         // 1:1 관계 연결
                    .additionalText(requestDto.getAdditionalText())
                    .build();

                // 메서드 종료시 트랜잭션이 커밋되는 순간 INSERT문이 실행됨.
                businessInfoRepository.save(newInfo);
            }
        }
    }

    // 주차장 검색을 통한 사업장의 추천 주차장 등록.
    @Transactional
    public void registerManualParkingToLocation(Long locationId, ManualParkingRequestDto requestDto, User businessUser) {
        
        // 사업장 조회 및 소유권 확인.
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // ParkingService를 호출해 IntegratedParking 엔티티를 가져오거나 생성.
        // 신규 생성 여부는 ParkingService 메서드에서 결정함.
        // requestDto : 이름, 주소, 좌표, Tmap PK, 추가 제공 정보.
        IntegratedParking parking = parkingService.getOrCreateManualParking(requestDto);

        // BusinessParking 엔터티 생성 및 저장.
        BusinessParking newBusinessParking = BusinessParking
            .builder()
            .businessLocation(location) 
            .integratedParking(parking) 
            .businessUserNumber(location.getBusinessUser().getUserNumber())
            .build();
        
        // 메서드 종료시 트랜잭션이 커밋되는 순간 INSERT문이 실행됨.
        businessParkingRepository.save(newBusinessParking);

        // BusinessInfo 엔티티 생성 및 저장.
        // 주차장 등록 요청에 추가 제공 정보가 있는 경우에만 실행.
        if (requestDto.getAdditionalText() != null && !requestDto.getAdditionalText().isEmpty()) {
            // 기존에 등록된 세부 정보(BusinessInfo)가 있는지 확인.
            // 조회된 주차장(IntegratedParking) 엔터티를 기준으로 함.
            Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);
            if (existingInfo.isPresent()) {
                // 이미 기존 정보가 있는 경우 텍스트만 업데이트.

                // get()으로 Optional 상자 안에서 실제 BusinessInfo 엔터티 객체 꺼냄.
                BusinessInfo infoToUpdate = existingInfo.get();
                // 엔터티 객체의 메서드 호출해 업데이트.
                infoToUpdate.updateText(requestDto.getAdditionalText());
            } else {
                // 기존 정보가 없는 경우 BusinessInfo 엔터티 객체 생성.

                BusinessInfo newInfo = BusinessInfo
                    .builder()
                    .integratedParking(parking)
                    .additionalText(requestDto.getAdditionalText())
                    .build();
                // 메서드 종료시 트랜잭션이 커밋되는 순간 INSERT문이 실행됨.
                businessInfoRepository.save(newInfo);
            }
        }
    }

    // 사업장의 추천 주차장 목록 조회.
    @Transactional(readOnly = true)
    public List<BusinessParkingDto.Response> getParkingsForLocation(Long locationId, User businessUser) {

        // 사업장 조회 및 소유권 확인.
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 해당 사업장에 연결된 주차장 목록 조회.
        List<BusinessParking> parkings = businessParkingRepository.findByBusinessLocation(location);
        
        // Entity List -> DTO List 변환하여 반환.
        // 데이터를 변환하여 리스트로 담을 때 stream - map - collect가 하나의 세트. 중간에 filter 삽입 가능.
        // JS의 map과 JAVA의 map 동작 차이.
        return parkings
            .stream()
            // parking : parkings 내에 들어있는 주차장 객체 각각을 지칭하는 임시 이름.
            .map(parking -> {
                // 사업자 제공 추가 정보(BusinessInfo) 조회.
                BusinessInfo info = businessInfoRepository
                    .findByIntegratedParking(parking.getIntegratedParking())
                    .orElse(null);

                // 주차장과 추가 정보 조합하여 DTO의 생성자 호출.
                // map에 대한 return으로 collect로 전달.
                return new BusinessParkingDto.Response(parking, info);
            })
            .collect(Collectors.toList());
    }

    // 주차장 추가 정보 업데이트.
    @Transactional
    public void updateParkingInfo(Long locationId, String parkingId, ParkingInfoUpdateDto requestDto, User businessUser) {
        
        // 유효성 검증. 사업장 조회 및 소유권 확인.
        findLocationByIdAndVerifyOwner(locationId, businessUser);

        // parkingId로 IntegratedParking 엔티티 조회
        IntegratedParking parking = integratedParkingRepository
            .findById(parkingId)
            .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + parkingId));

        // 변경할 추가 정보 텍스트.
        String newText = requestDto.getAdditionalText();

        // BusinessInfo 엔터티 생성 및 저장.
        // 요청에 추가 제공 정보가 있는 경우에만 실행.
        if (newText != null && !newText.isEmpty()) {
            // 사업자 제공 추가 정보(BusinessInfo) 엔터티 조회.
            Optional<BusinessInfo> existingInfo = businessInfoRepository.findByIntegratedParking(parking);

            if (existingInfo.isPresent()) {
                // 이미 기존 정보가 있는 경우 텍스트만 업데이트.

                // get()으로 Optional 상자 안에서 실제 BusinessInfo 엔터티 객체 꺼냄.
                BusinessInfo infoToUpdate = existingInfo.get();
                // 엔터티 객체의 메서드 호출해 업데이트.
                infoToUpdate.updateText(newText);
            } else {
                // 기존 정보가 없는 경우 BusinessInfo 엔터티 객체 생성.
                BusinessInfo newInfo = BusinessInfo
                    .builder()
                    .integratedParking(parking)
                    .additionalText(newText)
                    .build();
                
                // 메서드 종료 시 트랜잭션이 커밋되는 순간 INSERT문이 실행됨.
                businessInfoRepository.save(newInfo);
            }
        } else {
            // 요청에 추가 제공 정보가 없고, 기존 정보가 존재하는 경우 삭제.
            // 입력 값이 비어있으므로 기존 데이터가 존재한다면 제거하여 싱크를 맞춤.
            businessInfoRepository
                .findByIntegratedParking(parking)
                .ifPresent(info -> businessInfoRepository.delete(info));
        }
    }

    // 추천 주차장 연결 삭제.
    @Transactional
    public void deleteBusinessParking(Long businessParkingId, User businessUser) {
        
        // ID로 연결 정보(BusinessParking) 엔티티 조회.
        BusinessParking parkingLink = businessParkingRepository
            .findById(businessParkingId)
            .orElseThrow(() -> new EntityNotFoundException("해당 추천 주차장 정보를 찾을 수 없습니다."));

        // 유효성 검증.        
        // 연결 정보(BusinessParking)에서 추출한 사업장 ID(LocationID)를 넘겨 사업장 조회 및 소유권 확인.
        findLocationByIdAndVerifyOwner(parkingLink.getBusinessLocation().getId(), businessUser);

        // 추가 제공 정보가 있다면 함께 삭제.
        businessInfoRepository
            .findByIntegratedParking(parkingLink.getIntegratedParking())
            .ifPresent(businessInfoRepository::delete);
            // .ifPresent(info -> businessInfoRepository.delete(info)); 와 동일.

        // 엔터티 삭제.
        businessParkingRepository.delete(parkingLink);
        
        
        // 유효성 검증.
        // 현재 로그인 유저가 해당 연결을 등록한 유저(소유주)가 맞는지 확인.
        // if (!Objects.equals(parkingLink.getBusinessUserNumber(), businessUser.getUserNumber())) { 
        //      throw new AccessDeniedException("이 항목을 삭제할 권한이 없습니다.");
        // }

        // 엔터티 삭제.
        // businessParkingRepository.delete(parkingLink);
    }

    // 사업장 삭제 로직.
    @Transactional
    public void deleteBusinessLocation(Long locationId, User businessUser) {
        
        // 유효성 검증. 사업장 조회 및 소유권 확인.
        BusinessLocation location = findLocationByIdAndVerifyOwner(locationId, businessUser);

        // 해당 사업장에 연결된 추천 주차장 목록(BusinessParking) 조회.
        List<BusinessParking> parkingsToDelete = businessParkingRepository.findByBusinessLocation(location);

        // 각 연결에 대한 추가 제공 정보(BusinessInfo) 일괄 삭제.
        parkingsToDelete.forEach(link ->
            businessInfoRepository
                .findByIntegratedParking(link.getIntegratedParking())
                .ifPresent(businessInfoRepository::delete)
                // .ifPresent(info -> businessInfoRepository.delete(info))와 동일.
        );

        // 해당 사업장에 연결된 추천 주차장 전체 삭제.
        if (!parkingsToDelete.isEmpty()) {
            businessParkingRepository.deleteAll(parkingsToDelete);
        }

        // 사업장(BusinessLocation) 삭제.
        businessLocationRepository.delete(location);
    }

    // 사업장 ID로 엔터티 조회, 현재 로그인한 사용자가 소유자인지 검증.
    // 검증 완료 시 해당 사업장 엔터티 반환.
    private BusinessLocation findLocationByIdAndVerifyOwner(Long locationId, User currentUser) {
        // ID로 사업장 엔티티 찾기.
        BusinessLocation location = businessLocationRepository
            .findById(locationId)
            .orElseThrow(() -> new EntityNotFoundException("사업장 정보를 찾을 수 없습니다: " + locationId));
        
        // 유효성 검증.
        
        // 소유자 정보 존재 여부 확인.
        User owner = location.getBusinessUser();
        if(owner == null || owner.getUserNumber() == null) {
            // 시스템적으로 절대 있어서는 안 되는 상태일 때의 예외.
            throw new IllegalStateException("사업장의 소유자 데이터가 손상되었습니다. ID: " + locationId);
        }

        // 사업장 소유자 ID.
        Long targetId = owner.getUserNumber();
        // 로그인한 사용자 ID.
        Long currentId = currentUser.getUserNumber(); 


        // 해당 사업장의 소유자 ID와 현재 로그인한 사용자의 ID 비교.        
        if (!Objects.equals(targetId, currentId)) {
            // 소유자가 다르면 AccessDeniedException 발생 (403 Forbidden)
            throw new AccessDeniedException("사업장의 소유자만 접근 가능합니다.");
        }
        
        // 소유자가 맞으면 해당 사업장 엔티티 반환
        return location;
    }

}
