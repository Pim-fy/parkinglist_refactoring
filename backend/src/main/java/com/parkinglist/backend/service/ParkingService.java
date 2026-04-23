package com.parkinglist.backend.service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.parkinglist.backend.dto.ManualParkingRequestDto;
import com.parkinglist.backend.dto.ParkingFilterDto;
import com.parkinglist.backend.dto.ParkingLogRequestDto;
import com.parkinglist.backend.dto.ParkingResponseDto;
import com.parkinglist.backend.dto.TmapParkingRequestDto;
import com.parkinglist.backend.entity.BusinessLocation;
import com.parkinglist.backend.entity.BusinessParking;
import com.parkinglist.backend.entity.IntegratedParking;
import com.parkinglist.backend.entity.ParkingLog;
import com.parkinglist.backend.entity.ParkingStat;
import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.repository.BusinessInfoRepository;
import com.parkinglist.backend.repository.BusinessLocationRepository;
import com.parkinglist.backend.repository.BusinessParkingRepository;
import com.parkinglist.backend.repository.IntegratedParkingRepository;
import com.parkinglist.backend.repository.ParkingLogRepository;
import com.parkinglist.backend.repository.ParkingStatRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;  
import java.util.HashMap;  
import com.parkinglist.backend.dto.ParkingRecommendationRequestDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import com.parkinglist.backend.dto.ParkingDetailResponseDto;
import com.parkinglist.backend.dto.external.SeoulParkingDto;
import com.parkinglist.backend.entity.BusinessInfo;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.parkinglist.backend.dto.external.SeoulRealtimeDto;


@Slf4j                                  // 로그 사용을 위한 Slf4j.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)         // 읽기 전용 트랜잭션 설정.
public class ParkingService {

    // 의존성 주입.
    private final IntegratedParkingRepository integratedParkingRepository;
    private final ParkingStatRepository parkingStatRepository;
    private final BusinessParkingRepository businessParkingRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final BusinessInfoRepository businessInfoRepository;
    private final BusinessLocationRepository businessLocationRepository;
    private final WebClient webClient;

    // 설정값(API Key)을 읽어 필드에 주입.
    @Value("${api.key.seoul-static}")
    private String seoulStaticKey;
    @Value("${api.key.seoul-realtime}")
    private String seoulRealtimeKey;

    @Transactional
    // Tmap 주차장 데이터를 통합 DB에 동기화.
    // 최종 IntegratedParking 엔터티 반환.
    public IntegratedParking integratedParkingData(TmapParkingRequestDto tmapDto) {

        // Tmap PK로 중복 검사.
        String tmapPKey = tmapDto.getTmapPkey();
        // 빈 문자열 TmapPK는 null로 정규화하여 저장, 조회 시 중복 충돌을 방지.
        String normalizedTmapPKey = (tmapPKey != null && !tmapPKey.isBlank()) ? tmapPKey : null;
        Boolean requestedIsPublic = tmapDto.getIsPublic();
        Boolean requestedIsFree = tmapDto.getIsFree();

        boolean hasTmapPKey = normalizedTmapPKey != null;

        // Tmap PK가 있는 경우에만 PK로 중복 검사.
        Optional<IntegratedParking> existingByTmapPKey = Optional.empty();
        if(hasTmapPKey) {
            existingByTmapPKey = integratedParkingRepository.findByTmapPkey(normalizedTmapPKey);
        }

        if (existingByTmapPKey.isPresent()) {
            // PK 일치하는 기존 주차장 있는 경우.
            IntegratedParking parking = existingByTmapPKey.get();
            
            // 정보 동기화. IntegratedParking의 정보 업데이트용 메서드 호출.
            // 필터 정보는 Tmap DTO에 없다고 가정함.
            boolean nextIsPublic = (requestedIsPublic != null) ? requestedIsPublic : parking.isPublic();
            boolean nextIsFree = (requestedIsFree != null) ? requestedIsFree : parking.isFree();

            // 정보 동기화. Integratedparking의 정보 업데이트용 메서드 호출.
            // 필터 정보는 null이면 기존 값 유지.
            parking.updateParkingInfo(tmapDto.getName(), tmapDto.getAddress(), nextIsPublic, nextIsFree);
            
            return parking; 
        }

        // 좌표로 중복 검사.
        Optional<IntegratedParking> existingByCoords = integratedParkingRepository
            .findByLatitudeAndLongitude(
                tmapDto.getLatitude(),
                tmapDto.getLongitude()
            );
        
        if (existingByCoords.isPresent()) {
            // 좌표가 일치하는 기존 주차장 정보가 있는 경우 Tmap PK를 연결.
            IntegratedParking parking = existingByCoords.get();

            boolean nextIsPublic = (requestedIsPublic != null) ? requestedIsPublic : parking.isPublic();
            boolean nextIsFree = (requestedIsFree != null) ? requestedIsFree : parking.isFree();
            parking.updateParkingInfo(tmapDto.getName(), tmapDto.getAddress(), nextIsPublic, nextIsFree);

            // Tmap PK는 null/blank가 아닐 때만 연결함.
            if (hasTmapPKey) {
                parking.updateTmapPkey(normalizedTmapPKey);
            }

            return parking;
        }

        // IntegratedParking에 기존 주차장 미 존재 시 신규 주차장 생성.
        // 신규 주차장 생성 시작.

        // UUID구성의 parkingId 생성.
        String newParkingId = UUID.randomUUID().toString();
        
        // parkingId와 데이터 결합.
        // 없는 데이터는 빌더 패턴에 의해 기본값(false)로 설정됨.
        IntegratedParking newParking = IntegratedParking
            .builder()
            .parkingId(newParkingId)
            .name(tmapDto.getName())
            .address(tmapDto.getAddress())
            .latitude(tmapDto.getLatitude())
            .longitude(tmapDto.getLongitude())
            .tmapPkey(normalizedTmapPKey)
            .isPublic(requestedIsPublic != null ? requestedIsPublic : false)
            .isFree(requestedIsFree != null ? requestedIsFree : false)
            .build();
        
        // 주차장 선택 횟수 데이터(ParkingStat) 생성.
        ParkingStat newStat = ParkingStat
            .builder()
            .integratedParking(newParking)          // 1:1 관계 연결
            .selectionCount(0)      // 기본값 0
            .build();
        
        // 두 엔터티 모두 저장.
        // 메서드 종료 시 트랜잭션이 커밋되는 순간 INSERT문 생성.
        integratedParkingRepository.save(newParking);
        parkingStatRepository.save(newStat);

        return newParking;
    }

    @Transactional
    // 주차장 선택 로그 저장, 주차장 선택 횟수 증가.
    public void saveLogAndCount(ParkingLogRequestDto requestDto, User user) {

        // 요청 본문 null 차단.
        if (requestDto == null) {
            throw new IllegalArgumentException("요청 본문이 비어 있습니다.");
        }

        // parkingId 추출. 불필요한 공백 제거.
        String parkingId = (requestDto.getParkingId() == null) ? null : requestDto.getParkingId().trim();

        // parkingId null/blank 차단.
        if(parkingId == null || parkingId.isBlank()) {
            throw new IllegalArgumentException("parkingId는 필수입니다.");
        }

        // 최종 선택 목적지(SelectedDestinationName) 추출. 불필요한 공백 제거.
        String selectedDestinationName = (requestDto.getSelectedDestinationName() == null) ? null : requestDto.getSelectedDestinationName().trim();

        // parkingId로 integratedParking 엔터티 조회.
        IntegratedParking parking = integratedParkingRepository
            .findById(parkingId)
            .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + parkingId));

        // 로그인 사용자만 로그 저장.
        // parkingLog 엔터티 생성 및 저장.
        if(user != null) {
            // 최종 선택 목적지(SelectedDestinationName) null/blank 차단.
            if (selectedDestinationName == null || selectedDestinationName.isBlank()) {
                throw new IllegalArgumentException("최종 선택 목적지(SelectedDestinationName)는 필수입니다.");
            }
            
            // 최종 선택 목적지(SelectedDestinationName) 글자수 조건 확인.
            if (selectedDestinationName.length() > 100) {
                throw new IllegalArgumentException("최종 선택 목적지(SelectedDestinationName)는 100자 이하여야 합니다.");
            }

            ParkingLog newLog = ParkingLog
                .builder()
                .user(user)
                .integratedParking(parking)
                .selectedDestinationName(selectedDestinationName)
                .build();

            // 메서드 종료 시 트랜잭션이 커밋되는 순간 INSERT문 실행.
            parkingLogRepository.save(newLog);
        }
        
        // parkingStat 엔터티 조회.
        ParkingStat stat = parkingStatRepository
            .findByIntegratedParking_ParkingId(parkingId)
            .orElseThrow(() -> new EntityNotFoundException("주차장 통계 정보를 찾을 수 없습니다: " + parkingId));
        
        // 카운트 1 증가. parkingstat에 정의된 메서드 호출.
        stat.incrementCount();
    }

    // 필터링 및 3단계 정렬.
    @Transactional
    public List<ParkingResponseDto> getRecommendations(ParkingRecommendationRequestDto requestDto) {
        
        // DTO에서 데이터 분리.
        List<TmapParkingRequestDto> tmapParkingList = requestDto.getTmapParkingList();      // 주차장 목록.
        ParkingFilterDto filter = requestDto.getFilter();                                   // 필터 옵션.
        Long destinationLocationId = requestDto.getDestinationLocationId();                 // 사업장 ID.
        BigDecimal destinationLat = requestDto.getDestinationLat();                         // 좌표(위도).
        BigDecimal destinationLon = requestDto.getDestinationLon();                         // 좌표(경도).

        // Tmap 원본 주차장 목록 처리.
        List<IntegratedParking> tmapIntegratedList = tmapParkingList
            .stream()
            .map(this::integratedParkingData)
            .collect(Collectors.toList());
        
        // 좌표 검색 위한 허용 오차 범위(Bounding Box) 생성.
        // 0.0001도 ~= 11.1m.
        BigDecimal tolerance = new BigDecimal("0.0001"); 
        BigDecimal latMin = destinationLat.subtract(tolerance);
        BigDecimal latMax = destinationLat.add(tolerance);
        BigDecimal lonMin = destinationLon.subtract(tolerance);
        BigDecimal lonMax = destinationLon.add(tolerance);

        // 좌표 범위로 BusinessLocation을 찾음.
        List<BusinessLocation> foundLocations = businessLocationRepository
            .findByCoordinateRange(latMin, latMax, lonMin, lonMax);
        
        // 좌표로 1차 후보를 찾고, 목적지 ID(destinationLocationId)로 2차 식별.
        BusinessLocation matchedLocation = null;

        if (destinationLocationId != null) {
            // 목적지 ID가 있는 경우.
            
            // 좌표 후보 내 ID 일치 우선.
            matchedLocation = foundLocations
                .stream()
                .filter(loc -> destinationLocationId.equals(loc.getId()))
                .findFirst()
                .orElse(null);

            // 좌표 후보에서 못 찾으면 ID 직접 조회 fallback.
            if (matchedLocation == null) {
                matchedLocation = businessLocationRepository
                    .findById(destinationLocationId)
                    .orElse(null);
            }

            // 목적지 좌표와 500m 이상 차이나면 매칭 무효 처리.
            if (matchedLocation != null) {
                double distanceKm = calculateDistance(
                    destinationLat.doubleValue(),
                    destinationLon.doubleValue(),
                    matchedLocation.getLatitude().doubleValue(),
                    matchedLocation.getLongitude().doubleValue()
                );
            
                double limitKm = 0.5;

                if (distanceKm > limitKm) {
                    log.warn(
                        "destinationLocationId={} 좌표 검증 실패: {}km > {}km. 수동 매칭 생략.", 
                        destinationLocationId, 
                        distanceKm, 
                        limitKm
                    );
                    
                    matchedLocation = null;
                }
            }
        } else if (!foundLocations.isEmpty()) {
            // 목적지 ID가 없는 경우 : 목적지 좌표와 가장 가까운 후보 1건 선택.
            matchedLocation = foundLocations
                .stream()
                .min(Comparator
                    .comparingDouble(
                        (BusinessLocation loc) -> calculateDistance(
                            destinationLat.doubleValue(), 
                            destinationLon.doubleValue(), 
                            loc.getLatitude().doubleValue(), 
                            loc.getLongitude().doubleValue()
                        )
                    )
                    .thenComparing(BusinessLocation::getId)
            )
            .orElse(null);
        }

        // 수동 등록된 주차장 목록 조회.
        // 수동 등록된 주차장을 담을 빈 리스트 생성.
        List<IntegratedParking> manualIntegratedList = new ArrayList<>();

        if (matchedLocation != null) {
            // 목적지와 매칭된 사업장이 있는 경우.

            // 수동 등록된 주차장 목록 가져옴.
            List<BusinessParking> manualLinks = businessParkingRepository
                .findByBusinessLocation_Id(matchedLocation.getId());

            // 조회된 링크에서 IntegratedParking 목록 추출.
            manualIntegratedList = manualLinks
                .stream()
                .map(BusinessParking::getIntegratedParking)
                .collect(Collectors.toList());
        }


        // Tmap 주차장 목록과 수동 등록 주차장 목록을 중복 제거하여 합침.

        // 합쳐진 목록이 될 빈 Map 생성.
        // Key로 parkingId, value로 주차장 객체(integratedParking) 사용.
        Map<String, IntegratedParking> combinedParkingMap = new HashMap<>();

        // Tmap 원본 주차장 목록의 데이터 넣기.
        tmapIntegratedList.forEach(parking -> combinedParkingMap.put(parking.getParkingId(), parking));

        // 수동 등록 주차장 목록 중, Tmap 원본 주차장 목록에 없던 데이터 넣기.
        manualIntegratedList.forEach(parking -> combinedParkingMap.putIfAbsent(parking.getParkingId(), parking));
        
        // Map에서 주차장 객체(integratedParking)을 뽑아 주차장 객체들만 담긴 리스트 생성.
        List<IntegratedParking> combinedList = new ArrayList<>(combinedParkingMap.values());


        // 추천 데이터 먼저 조회. 1순위 사업자 추천 주차장을 최우선으로 다루기 위함.
        // 사업자가 등록한 추천 주차장 parkingId 집합 생성.
        // Set : 중복 허용하지 않는 유일한 값들의 모음.
        Set<String> businessParkingIds = businessParkingRepository
            .findAll()
            .stream()
            .map(parking -> parking.getIntegratedParking().getParkingId())
            .collect(Collectors.toSet());

        // parkingId와 주차장 선택 횟수(parkingStat) 매핑.
        // Map : Key와 Value 중 Value는 중복 가능.
        Map<String, ParkingStat> statMap = parkingStatRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(
                parking -> parking.getIntegratedParking().getParkingId(), 
                parking -> parking
            ));

        // parkingId와 추가 제공 정보(additionalText)의 매핑.
        // Map : Key와 Value 중 Value는 중복 가능.
        Map<String, String> infoMap = businessInfoRepository
            .findAll()
            .stream()
            .collect(Collectors.toMap(
                parking -> parking.getIntegratedParking().getParkingId(),
                parking -> parking.getAdditionalText()
                // 둘 중 아래 코드는 축약형 형태 가능. BusinessInfo::getAdditionalText()
            ));

        // 필터링.
        // .filter : true or false의 결과.
        List<IntegratedParking> filteredList = combinedList
            .stream()
            .filter(parking -> {

                // 이 주자창이 사업자 추천 주차장인지 확인.
                boolean isRecommended = businessParkingIds.contains(parking.getParkingId());
                
                // 사업자 추천 주차장으로 등록되어있는 경우 최우선 통과(필터 영향 X).
                if (isRecommended) {
                    return true;
                }
                
                // 추천 주차장을 제외한 주차장에 필터링 옵션 적용(무료, 공영).
                boolean passesFreeFilter = !filter.isFree() || parking.isFree();
                boolean passesPublicFilter = !filter.isPublic() || parking.isPublic();
                
                return passesFreeFilter && passesPublicFilter;
            })
            .collect(Collectors.toList());




        // 거리 정보맵 재구성.
        // Tmap 제공 거리 + 직접 계산한 거리.
        
        // Tmap 제공 거리 정보. 
        Map<String, Double> tmapDistanceMap = tmapParkingList
            .stream() 
            .filter(dto -> dto.getTmapPkey() != null && !dto.getTmapPkey().isBlank())
            .filter(dto -> dto.getDistance() != null)
            .collect(Collectors.toMap(
                TmapParkingRequestDto::getTmapPkey,     // Key : Tmap PK.
                TmapParkingRequestDto::getDistance,     // Value : 거리.
                (oldValue, newValue) -> oldValue        // PKey 중복 시 기존 값 사용.
            ));

        // 최종 거리 정보 맵. ParkingId 기준.
        Map<String, Double> finalDistanceMap = new HashMap<>();

        // combinedList : IntegratedParking의 주차장 객체들이 담긴 리스트. Tmap + 수동 등록. parkingId 기준.
        // list의 객체들을 parking이라는 이름으로 하나씩 꺼내 for문 실행.
        for (IntegratedParking parking : combinedList) {
            Double distance = null;

            final String tmapPKey = parking.getTmapPkey();

            // Tmap PKey가 있고, Tmap이 거리를 줬으면 그걸 사용
            if (tmapPKey != null && !tmapPKey.isBlank()) {
                distance = tmapDistanceMap.get(tmapPKey);
            }
            
            // Tmap의 거리 정보가 없거나, 수동 등록된 주차장인 경우.
            if (distance == null && destinationLat != null && destinationLon != null) {
                // calculateDistance 메서드로 직접 거리 계산.
                distance = calculateDistance(
                    destinationLat.doubleValue(), destinationLon.doubleValue(),
                    parking.getLatitude().doubleValue(), parking.getLongitude().doubleValue()
                );
            }
            
            // 거리 정보 저장. 
            // 실패 시(거리 값 없는 경우) 맨 뒤로 정렬되도록 MAX_VALUE.
            finalDistanceMap.put(parking.getParkingId(), distance != null ? distance : Double.MAX_VALUE);
        }

        // 최종 정렬.
        filteredList
            .sort(Comparator
                    // 1순위: 사업자 추천 여부.
                    .comparing((IntegratedParking parking) -> {
                        return businessParkingIds.contains(parking.getParkingId()) ? 0 : 1; 
                    })
                    // 2순위: 선택 횟수.
                    // revserseOrder : 내림차순(큰 값이 앞)으로 정렬.
                    .thenComparing((IntegratedParking parking) -> {
                        ParkingStat stat = statMap.get(parking.getParkingId());
                        return (stat != null) ? stat.getSelectionCount() : 0; 
                    }, Comparator.reverseOrder())
                    // 3순위: 거리(finalDistanceMap).
                    .thenComparing((IntegratedParking parking) -> {
                        return finalDistanceMap.getOrDefault(parking.getParkingId(), Double.MAX_VALUE);
                    })
            );

        // 최종 DTO 변환.
        return filteredList
            .stream()
            // map에서 각각의 DTO 생성.
            .map(parking -> {
                // 추천 여부.
                boolean isRecommended = businessParkingIds.contains(parking.getParkingId());
                // 선택 횟수.
                int selectionCount = Optional
                    .ofNullable(statMap.get(parking.getParkingId()))        // statMap에서 ParkingStat을 꺼냄.
                    .map(ParkingStat::getSelectionCount)                    // ParkingStat객체에서 선택 횟수만 뽑아냄.
                    .orElse(0);                                         // 선택 횟수 데이터가 없는 경우 0 반환.
                // 거리.
                double distance = finalDistanceMap.getOrDefault(parking.getParkingId(), Double.MAX_VALUE);
                // 추가 제공 정보.
                String additionalText = infoMap.get(parking.getParkingId());

                // ParkingResponseDto의 정적 팩토리 메서드 사용.
                return ParkingResponseDto.forRecommendation(
                    parking,
                    isRecommended,
                    selectionCount,
                    distance,
                    additionalText
                );
            })
            // 리스트로 DTO를 수집.
            .collect(Collectors.toList());
    }

    // 주차장 수동 등록용 Get or Create 메서드.
    @Transactional
    public IntegratedParking getOrCreateManualParking(ManualParkingRequestDto requestDto) {

        String tmapPkey = requestDto.getTmapPkey();
        String normalizedTmapPKey = (tmapPkey != null && !tmapPkey.isBlank()) ? tmapPkey : null;

        // Tmap PKey로 찾을 수 있는 경우.
        // Tmap PKey가 존재하며, 통합 DB에 있는 경우.
        if (normalizedTmapPKey != null) {
            Optional<IntegratedParking> existingByPKey = integratedParkingRepository.findByTmapPkey(normalizedTmapPKey);
            // TmapPkey로 통합 DB에서 찾았는데, 이름/주소가 다를 수 있으니 업데이트.
            if (existingByPKey.isPresent()) {
                IntegratedParking parking = existingByPKey.get();
                parking.updateParkingInfo(requestDto.getParkingName(), requestDto.getAddress(), parking.isPublic(), parking.isFree());
                return parking;
            }
        }

        // 좌표로 찾을 수 있는 경우.
        Optional<IntegratedParking> existingByCoords = integratedParkingRepository.findByLatitudeAndLongitude(
            requestDto.getLatitude(),
            requestDto.getLongitude()
        );
        // 좌표로 찾은 경우 tmapPkey가 null일 수 있으니 데이터 연결.
        if (existingByCoords.isPresent()) {
            IntegratedParking parking = existingByCoords.get();
            if (normalizedTmapPKey != null) {
                parking.updateTmapPkey(normalizedTmapPKey); 
            }
            return parking;
        }

        // 신규 주차장 생성.
        // UUID로 ParkingId 생성.
        String newParkingId = UUID.randomUUID().toString();
        IntegratedParking newParking = IntegratedParking
            .builder()
            .parkingId(newParkingId)
            .name(requestDto.getParkingName())      // 사용자가 입력한 이름.
            .address(requestDto.getAddress())
            .latitude(requestDto.getLatitude())
            .longitude(requestDto.getLongitude())
            .tmapPkey(normalizedTmapPKey)
            .build();

        // 신규 주차장에 대한 ParkingStat 생성.
        ParkingStat newStat = ParkingStat
            .builder()
            .integratedParking(newParking)
            .selectionCount(0)
            .build();

        // 신규 생성 주차장 저장.
        integratedParkingRepository.save(newParking);
        parkingStatRepository.save(newStat);

        return newParking;
    }

    // 상세 정보 메서드.
    @Transactional(readOnly = true)
    public ParkingDetailResponseDto getParkingDetails(String parkingId) {
        
        // 통합 DB 필수 정보 조회.
        IntegratedParking parking = integratedParkingRepository
            .findById(parkingId)
            .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + parkingId));
        // 선택 횟수.
        ParkingStat stat = parkingStatRepository
            .findByIntegratedParking_ParkingId(parkingId)
            .orElse(null); 
        // 사업자 제공 추가 정보.
        BusinessInfo info = businessInfoRepository
            .findByIntegratedParking(parking)
            .orElse(null); 

        // DB 주소에서 '자치구명' 추출.
        // 자치구명 -> 서울시 주차장 정보 API 요청에 사용, 1차 필터링.
        String district = parseDistrictFromAddress(parking.getAddress());
        
        // 두 API를 동시에 병렬 호출함.
        // CompletableFuture : 비동기 프로그래밍을 위한 도구. 해당 작업을 백그라운드에서 어떻게 실행, 끝난 뒤 무엇을 할지 고민하는 도구.
        CompletableFuture<SeoulParkingDto> staticFuture = fetchSeoulStaticInfo(district);               // fetchSeoulStaticInfo(): 서울시 API 호출 헬퍼 메서드.
        CompletableFuture<SeoulRealtimeDto> realtimeFuture = fetchSeoulRealtimeInfo(district);          // fetchSeoulRealtimeInfo(): 서울시 실시간 API 호출 헬퍼 메서드.

        try {
            // 두 API의 응답이 모두 올 때까지 대기.
            // allOf: 여러 개의 비동기 작업(CompleteableFuture)을 묶어줌.
            // allOf의 반환 타입은 void. 각 작업의 결과값을 돌려주지 않고, 전부 완료되었다는 상태만 알려줌.
            // join : 묶은 작업의 완료를 기다림.
            CompletableFuture
                .allOf(staticFuture, realtimeFuture)
                .join();
            
            // 응답 데이터 DTO 객체로 가져오기.
            // allOf().join() 이후라 즉시 반환됨.
            SeoulParkingDto staticData = staticFuture.join();
            SeoulRealtimeDto realtimeData = realtimeFuture.join();

            // 1차 매칭 - 좌표 매칭. findMatchingParking 메서드 사용.
            // 정적(Static) 데이터에서 좌표로 주차장 찾기.
            SeoulParkingDto.Row matchedStaticRow = findMatchingParking(
                staticData, 
                parking.getLatitude(), 
                parking.getLongitude()
            );

            // 변수 선언.
            String priceInfo = null;                // 요금 정보 저장.
            String operatingHours = null;           // 운영 시간 정보 저장.
            Integer availableSpots = null;          // 현재 주차 가능 대수 정보 저장(매칭 실패 or 미제공 시 null).
            String parkingCode = null;              // 주차장 코드 저장.

            // 좌표 매칭 성공 시 2차 매칭 준비.
            // 정적 정보(요금, 운영시간) 가공.
            if (matchedStaticRow != null) {
                // 요금.
                if (matchedStaticRow.getRates() != null && matchedStaticRow.getRates() > 0) {
                    priceInfo = String.format(
                        "%.0f분 %.0f원 (추가 %.0f분 %.0f원)", 
                        matchedStaticRow.getTimeRate(), 
                        matchedStaticRow.getRates(), 
                        matchedStaticRow.getAddTimeRate(), 
                        matchedStaticRow.getAddRates()
                    );
                } else if ("N".equals(matchedStaticRow.getPayYn())) {
                    priceInfo = "무료";
                }

                // 운영시간.
                if (matchedStaticRow.getWeekdayBeginTime() != null && matchedStaticRow.getWeekdayEndTime() != null) {
                    operatingHours = String.format(
                        "평일 %s ~ %s",
                        matchedStaticRow.getWeekdayBeginTime(),
                        matchedStaticRow.getWeekdayEndTime()
                    );
                }
                
                // 매칭된 주차장의 주차장 코드(parkingCode) 추출.
                parkingCode = matchedStaticRow.getParkingCode(); 
            }

            // 2차 매칭 - 주차장 코드. findMatchingRealtimeRow메서드 사용.
            // 실시간 데이터 목록에서 주차장 코드(parkingCode)로 매칭. 
            if (parkingCode != null && !parkingCode.isBlank()) {
                SeoulRealtimeDto.Row matchedRealtimeRow = findMatchingRealtimeRow(realtimeData, parkingCode);
                
                // 현재 주차 가능 대수 정보 가공.
                if (matchedRealtimeRow != null) {
                    Integer capacity = matchedRealtimeRow.getCapacity();
                    Integer curParking = matchedRealtimeRow.getCurParking();
                    if (capacity != null && curParking != null) {
                        availableSpots = capacity - curParking;
                        if (availableSpots < 0) {
                            availableSpots = 0;
                        }
                    }
                }
            }

            // 최종 DTO 빌드.
            return ParkingDetailResponseDto
                .builder()
                .parkingId(parking.getParkingId())
                .name(parking.getName())
                .address(parking.getAddress())
                .selectionCount(stat != null ? stat.getSelectionCount() : 0)
                .additionalText(info != null ? info.getAdditionalText() : null)
                .priceInfo(priceInfo)
                .operatingHours(operatingHours)
                .availableSpots(availableSpots)
                .build();

        } catch (CompletionException e) {
            Throwable cause = (e.getCause() != null) ? e.getCause() : e;
            log.error("API 응답 집계 중 비동기 오류 발생", cause);
            throw new RuntimeException("상세 정보 집계 중 오류가 발생했습니다.", cause);
        } catch (Exception e) {
            // 비동기 예외 처리.
            log.error("API 응답 집계 중 오류 발생", e);
            throw new RuntimeException("상세 정보 집계 중 오류가 발생했습니다.", e);
        }
    }

    // 거리 계산 헬퍼 메서드. 
    // Haversine formula 이용, km단위.
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0088;         // 지구 반지름(km).
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        // 부동소수 오차로 a가  [0,1] 범위를 벗어나는 경우 방어.
        a = Math.min(1.0d, Math.max(0.0d, a));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;                       // km 단위 거리
    }

    // 주소에서 자치구명 추출 헬퍼 메서드.
    // 현재 서울시 API 요청에 사용.
    private String parseDistrictFromAddress(String address) {
        if (address == null || address.isEmpty()) {
            return "";
        }
        // 공백을 기준으로 주소 나누기.
        String[] parts = address.split(" ");
        if (parts.length > 1) {
            return parts[1];
        }
        return parts[0];
    }

    // 서울시 API 호출 헬퍼 메서드.
    // GetParkInfo API, '자치구명' ADDR 파라미터 사용.
    private CompletableFuture<SeoulParkingDto> fetchSeoulStaticInfo(String districtName) {
        String apiUrl = String.format(
            "http://openapi.seoul.go.kr:8088/%s/json/GetParkInfo/1/1000/%s",            // 결과 수 1000개.
            seoulStaticKey,                                                             // API Key.
            districtName                                                                // 자치구명.
        );

        // 비동기 파이프라인.
        return webClient
            .get()              // HTTP 메서드를 GET으로 설정한 요청 빌더를 만듦.
            .uri(apiUrl)        // 실제 요청 URL을 넣음.
            .retrieve()         // 응답 처리 체인 시작.
            .bodyToMono(SeoulParkingDto.class)                      // JSON을 역직렬화 하는 비동기 Mono를 만듦.
            .timeout(Duration.ofSeconds(5))                         // 외부 API 타임아웃.
            .doOnError(error -> log.error("[API Fail] Seoul Static API Error: {}", error.getMessage()))         // 에러 발생시 로그를 남김.
            .onErrorReturn(new SeoulParkingDto())                   // 핵심 복구 지점. 호출 실패 시 예외가 아닌 빈 데이터.
            .toFuture();                                            // Mono를 CompletableFuture로 변환하여 반환.
    }

    // 서울시 실시간 API 호출 헬퍼 메서드.
    private CompletableFuture<SeoulRealtimeDto> fetchSeoulRealtimeInfo(String districtName) {
        String apiUrl = String.format(
            "http://openapi.seoul.go.kr:8088/%s/json/GetParkingInfo/1/1000/%s",         // 결과 수 1000개.
            seoulRealtimeKey,                                                           // API Key.
            districtName                                                                // 자치구명.
        );

        return webClient
            .get()              // HTTP 메서드를 GET으로 설정한 요청 빌더를 만듦.
            .uri(apiUrl)        // 실제 요청 URL을 넣음.
            .retrieve()         // 응답 처리 체인 시작.
            .bodyToMono(SeoulRealtimeDto.class)                     // JSON을 역직렬화 하는 비동기 Mono를 만듦.
            .timeout(Duration.ofSeconds(5))                         // 외부 API 타임아웃.
            .doOnError(error -> log.error("[API Fail] Seoul Realtime API Error: {}", error.getMessage()))       // 에러 발생시 로그를 남김.
            .onErrorReturn(new SeoulRealtimeDto())                  // 핵심 복구 지점. 호출 실패 시 예외가 아닌 빈 데이터.
            .toFuture();                                            // Mono를 CompletableFuture로 변환하여 반환.
    }

    // 좌표 기반 매칭 헬퍼 메서드.
    private SeoulParkingDto.Row findMatchingParking(SeoulParkingDto seoulDto, BigDecimal dbLat, BigDecimal dbLon) {
        
        if (seoulDto == null || dbLat == null || dbLon == null) {
            log.debug("[findMatchingParking] 입력값 누락: seoulDto={}, dbLat={}, dbLon={}", seoulDto, dbLat, dbLon);
            return null;
        }

        // 서울시 정적 API응답의 GetParkInfo 섹션(주차장 row 목록 컨테이너).
        SeoulParkingDto.ParkInfo parkInfo = seoulDto.getParkInfo();

        if (parkInfo == null || parkInfo.getRow() == null) {

            // 디버깅 로그. API응답이 비어있는지 확인.
            log.info("[Debug] findMatchingParking: 서울시 API 응답이 비어있거나 'row'가 없습니다.");

            // 서울시 API 응답이 비어있는 경우 null 반환.
            return null;
        }

        // 디버깅 로그. DB 좌표 출력.
        log.info("[Debug] findMatchingParking: DB 좌표 ({}, {})와 비교 시작", dbLat, dbLon);

        SeoulParkingDto.Row bestMatch = null;
        double minDistance = Double.MAX_VALUE;
        
        // 자치구명으로 검색된 주차장 목록(row) 순회.
        for (SeoulParkingDto.Row row : parkInfo.getRow()) {

            if (row == null) {
                continue;
            }

            if (row.getLatitude() != null && row.getLongitude() != null) {
                
                // DB 좌표와 API 좌표 간의 거리 계산. calculateDistance 재사용.
                double distance = calculateDistance(
                        dbLat.doubleValue(), dbLon.doubleValue(),
                        row.getLatitude(), row.getLongitude()
                );

                // 디버깅 로그. 500m 이내의 모든 주차장 정보를 로그 출력.
                if (distance < 0.5) {
                    log.info(
                        "[Debug] findMatchingParking: 500m 이내 후보 찾음: {} / 거리(km): {}", 
                        row.getParkingName(), 
                        distance
                    );
                }
                // 디버깅 로그. DB좌표, API 좌표, 계산된 거리 출력.
                log.info(
                    "DB좌표: {}, {} / API좌표: {}, {} / 계산된 거리(km): {}", 
                    dbLat, 
                    dbLon, 
                    row.getLatitude(), 
                    row.getLongitude(), 
                    distance
                );
                // 100미터 이내, 이전에 찾은 것보다 가깝다면 bestMatch로 설정.
                if (distance < 0.1 && distance < minDistance) { 
                    minDistance = distance;
                    bestMatch = row;
                }
            }
        }

        // 디버깅 로그. 최종 매칭 결과 출력.
        if (bestMatch != null) {
            log.info(
                "[Debug] findMatchingParking: 최종 매칭 성공: {} (거리: {}km)", 
                bestMatch.getParkingName(), 
                minDistance
            );
        } else {
            log.warn("[Debug] findMatchingParking: 100m 이내 매칭되는 주차장을 찾지 못했습니다.");
        }

        // 100m 이내 가장 가까운 주차장 1개 반환.
        return bestMatch;
    }

    // 주차장 코드 매칭 헬퍼 메서드.
    private SeoulRealtimeDto.Row findMatchingRealtimeRow(SeoulRealtimeDto realtimeDto, String parkingCode) {

        if (realtimeDto == null || parkingCode == null || parkingCode.isBlank()) {
            return null;
        }
        
        SeoulRealtimeDto.ParkingInfo parkingInfo = realtimeDto.getParkingInfo();
        if (parkingInfo == null || parkingInfo.getRow() == null) {
            return null;
        }

        // 실시간 목록(row)을 순회하며 parkingCode가 일치하는 첫 번째 항목을 찾음.
        SeoulRealtimeDto.Row match = parkingInfo.getRow()
            .stream()
            .filter(row -> row != null)
            .filter(row -> parkingCode.equals(row.getParkingCode())) 
            .findFirst()
            .orElse(null);

        log.info(
            "[Debug] 2차 코드 매칭 결과 (코드: {}): {}", 
            parkingCode, 
            (match != null ? match.getParkingName() : "실패")
        );

        return match;
    }
}
