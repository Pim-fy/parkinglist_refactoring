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
import com.parkinglist.backend.dto.TmapParkingDto;
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
import java.util.ArrayList;  
import java.util.HashMap;  
import com.parkinglist.backend.dto.ParkingRecommendationRequestDto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import com.parkinglist.backend.dto.ParkingDetailResponseDto;
import com.parkinglist.backend.dto.external.SeoulParkingDto;
import com.parkinglist.backend.entity.BusinessInfo;
import lombok.extern.slf4j.Slf4j;

// [신규] 비동기 처리를 위한 CompletableFuture 임포트 ▼▼▼
import java.util.concurrent.CompletableFuture;
// [신규] 새로 만든 실시간 DTO 임포트 ▼▼▼
import com.parkinglist.backend.dto.external.SeoulRealtimeDto;


@Slf4j // (로그 사용을 위해 @Slf4j 추가)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)         // 읽기 전용 트랜잭션 설정
public class ParkingService {
    // Repository 주입
    private final IntegratedParkingRepository integratedParkingRepository;
    private final ParkingStatRepository parkingStatRepository;
    private final BusinessParkingRepository businessParkingRepository;
    private final ParkingLogRepository parkingLogRepository;
    private final BusinessInfoRepository businessInfoRepository;
    private final BusinessLocationRepository businessLocationRepository;
    private final WebClient webClient;

    @Value("${api.key.seoul-static}")
    private String seoulStaticKey;

    @Value("${api.key.seoul-realtime}")
    private String seoulRealtimeKey;

    @Transactional
    public IntegratedParking integratedParkingData(TmapParkingDto tmapDto) {
        // Tmap PK로 중복 검사
        Optional<IntegratedParking> existingByPKey = integratedParkingRepository.findByTmapPkey(tmapDto.getTmapPkey());
        if (existingByPKey.isPresent()) {
            // PK 일치하는 기존 주차장 있는 경우
            IntegratedParking parking = existingByPKey.get();
            
            // 이름/주소 등 업데이트
            // 현 상태는 isPublic, isFree는 Tmap DTO에 없다고 가정하고 false 전달
            parking.updateParkingInfo(tmapDto.getName(), tmapDto.getAddress(), false, false);
            
            // save()는 @Transactional에 의해 트랜잭션 종료 시 자동으로 수행됨
            return parking; 
        }

        // 좌표로 중복 검사
        Optional<IntegratedParking> existingByCoords = integratedParkingRepository.findByLatitudeAndLongitude(
            tmapDto.getLatitude(),
            tmapDto.getLongitude()
        );
        if (existingByCoords.isPresent()) {
            // 좌표 일치하는 기존 주차장 있는 경우
            // PKey를 연결
            IntegratedParking parking = existingByCoords.get();
            parking.updateTmapPkey(tmapDto.getTmapPkey());
            return parking;
        }

        // 신규 주차장
        // UUID구성의 parkingId 생성
        String newParkingId = UUID.randomUUID().toString();
        IntegratedParking newParking = IntegratedParking.builder()
                .parkingId(newParkingId)
                .name(tmapDto.getName())
                .address(tmapDto.getAddress())
                .latitude(tmapDto.getLatitude())
                .longitude(tmapDto.getLongitude())
                .tmapPkey(tmapDto.getTmapPkey())
                // isPublic, isFree는 Tmap DTO에 없으면 기본값(false)으로 설정됨 (by @Builder.Default)
                .build();
        
        // 통계 테이블인 ParkingStat에도 레코드 생성
        ParkingStat newStat = ParkingStat.builder()
                .integratedParking(newParking) // 1:1 관계 연결
                .selectionCount(0) // 기본값 0
                .build();
        
        // 두 엔터티 모두 저장
        integratedParkingRepository.save(newParking);
        parkingStatRepository.save(newStat);

        return newParking;
    }

    @Transactional
    public void saveLogAndCount(ParkingLogRequestDto requestDto, User user) {
        // 주차장 선택 로그 저장, 해당 주차장 선택 횟수 1 증가

        // parkingId로 integratedParking 엔터티 조회
        IntegratedParking parking = integratedParkingRepository.findById(requestDto.getParkingId())
                .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + requestDto.getParkingId()));

        // 로그인 사용자만 로그 저장
        // parkingLog 엔터티 생성 및 저장
        if(user != null) {
            ParkingLog newLog = ParkingLog.builder()
                .user(user)
                .integratedParking(parking)
                .selectedDestinationName(requestDto.getSelectedDestinationName())
                .build();
            parkingLogRepository.save(newLog);
        }
        

        // parkingStat 엔터티 조회, 카운트 1 증가
        ParkingStat stat = parkingStatRepository.findByIntegratedParking_ParkingId(requestDto.getParkingId())
                .orElseThrow(() -> new EntityNotFoundException("주차장 통계 정보를 찾을 수 없습니다: " + requestDto.getParkingId()));
        stat.incrementCount();
    }

    // 필터링 및 3단계 정렬
    @Transactional
    public List<ParkingResponseDto> getRecommendations(ParkingRecommendationRequestDto requestDto) { // DTO 통째로 받기
        
        // --- 1. DTO에서 데이터 분리 ---
        List<TmapParkingDto> tmapParkingList = requestDto.getTmapParkingList();
        ParkingFilterDto filter = requestDto.getFilter();
        // Long destinationLocationId = requestDto.getDestinationLocationId();
        BigDecimal destinationLat = requestDto.getDestinationLat();
        BigDecimal destinationLon = requestDto.getDestinationLon();

        // --- 2. Tmap 목록 처리 (기존 로직) ---
        List<IntegratedParking> tmapIntegratedList = tmapParkingList.stream()
                .map(this::integratedParkingData)
                .collect(Collectors.toList());

        // --- 3. (신규) 수동 등록된 주차장 목록 조회 ---
        // ---  (★★★ 로직 대폭 수정 ★★★) ---
        // '좌표'로 사업장을 찾아서 수동 등록 주차장을 조회
        List<IntegratedParking> manualIntegratedList = new ArrayList<>();
        
        // 3-1. (신규) 좌표 검색을 위한 '허용 오차 범위' (Bounding Box) 생성
        // (0.0001도는 약 11.1미터에 해당)
        BigDecimal tolerance = new BigDecimal("0.0001"); 
        BigDecimal latMin = destinationLat.subtract(tolerance);
        BigDecimal latMax = destinationLat.add(tolerance);
        BigDecimal lonMin = destinationLon.subtract(tolerance);
        BigDecimal lonMax = destinationLon.add(tolerance);

        // 3-2. (신규) '범위'로 BusinessLocation을 찾습니다.
        List<BusinessLocation> foundLocations = businessLocationRepository
                .findByCoordinateRange(latMin, latMax, lonMin, lonMax);

        // 3-3. 일치하는 사업장을 찾았을 경우에만 (첫 번째 항목을 사용)
        if (!foundLocations.isEmpty()) {
            // (여러 개가 찾아질 경우를 대비해 첫 번째 항목을 사용)
            Long foundLocationId = foundLocations.get(0).getId();
            
            // 3-4. 해당 ID로 수동 등록 주차장을 조회합니다. (기존 로직)
            List<BusinessParking> manualLinks = businessParkingRepository.findByBusinessLocation_Id(foundLocationId);
            manualIntegratedList = manualLinks.stream()
                    .map(BusinessParking::getIntegratedParking)
                    .collect(Collectors.toList());
        }

        // --- 4. (신규) Tmap 목록 + 수동 목록 = 중복 제거하여 합치기 ---
        Map<String, IntegratedParking> combinedParkingMap = new HashMap<>();
        tmapIntegratedList.forEach(p -> combinedParkingMap.put(p.getParkingId(), p));
        manualIntegratedList.forEach(p -> combinedParkingMap.putIfAbsent(p.getParkingId(), p)); // Tmap에 없던 것만 추가
        
        List<IntegratedParking> combinedList = new ArrayList<>(combinedParkingMap.values());

        // --- 5. (순서 변경) 추천 데이터 먼저 조회 ---
        Set<String> businessParkingIds = businessParkingRepository.findAll().stream()
                    .map(bp -> bp.getIntegratedParking().getParkingId())
                    .collect(Collectors.toSet());

        Map<String, ParkingStat> statMap = parkingStatRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            ps -> ps.getIntegratedParking().getParkingId(), 
                            ps -> ps
                    ));
        
        Map<String, String> infoMap = businessInfoRepository.findAll().stream()
                .collect(Collectors.toMap(
                        info -> info.getIntegratedParking().getParkingId(),
                        info -> info.getAdditionalText()
                ));

        // --- 6. 필터링 ---
        List<IntegratedParking> filteredList = combinedList.stream()
            .filter(p -> {
                // 1. 이 주차장이 추천 주차장인지 확인
                boolean isRecommended = businessParkingIds.contains(p.getParkingId());
                
                // 2. ★ 추천 주차장이면 필터와 관계없이 무조건 통과! ★
                if (isRecommended) {
                    return true;
                }
                
                // 3. 추천 주차장이 아닌 일반 주차장만 필터링
                boolean passesFreeFilter = !filter.isFree() || p.isFree();
                boolean passesPublicFilter = !filter.isPublic() || p.isPublic();
                
                return passesFreeFilter && passesPublicFilter;
            })
            .collect(Collectors.toList());

        // --- 7. (신규) 거리 정보맵 재구성 (Tmap 거리 + 직접 계산 거리) ---
        
        // 7-1. Tmap이 제공한 거리 정보 (PKey 기준)
        Map<String, Double> tmapDistanceMap = tmapParkingList.stream() 
                    .collect(Collectors.toMap(
                            TmapParkingDto::getTmapPkey, // PKey를 Key로
                            TmapParkingDto::getDistance, // 거리를 Value로
                            (d1, d2) -> d1 // 혹시 PKey 중복 시 첫 번째 값 사용
                    ));

        // 7-2. 최종 거리맵 (ParkingId 기준)
        Map<String, Double> finalDistanceMap = new HashMap<>();
        for (IntegratedParking p : combinedList) { // ★★★ 합쳐진 목록(combinedList) 기준
            Double distance = null;
            // Tmap PKey가 있고, Tmap이 거리를 줬으면 그걸 사용
            if (p.getTmapPkey() != null) {
                distance = tmapDistanceMap.get(p.getTmapPkey());
            }
            
            // Tmap이 거리를 안 줬거나(null) 수동 등록된 주차장인 경우
            if (distance == null && destinationLat != null && destinationLon != null) {
                // 위에서 만든 calculateDistance 함수로 직접 거리 계산
                distance = calculateDistance(
                    destinationLat.doubleValue(), destinationLon.doubleValue(),
                    p.getLatitude().doubleValue(), p.getLongitude().doubleValue()
                );
            }
            
            // 계산된 거리 저장 (실패 시 맨 뒤로 정렬되도록 MAX_VALUE)
            finalDistanceMap.put(p.getParkingId(), distance != null ? distance : Double.MAX_VALUE);
        }

        // --- 8. 최종 정렬 (기존 로직과 유사) ---
        filteredList.sort(Comparator.comparing((IntegratedParking p) -> {
                // 1순위: 사업자 추천 여부 (기존과 동일)
                return businessParkingIds.contains(p.getParkingId()) ? 0 : 1; 
            }).thenComparing((IntegratedParking p) -> {
                // 2순위: 선택 횟수 (기존과 동일)
                ParkingStat stat = statMap.get(p.getParkingId());
                return (stat != null) ? stat.getSelectionCount() : 0; 
            }, Comparator.reverseOrder())
            .thenComparing((IntegratedParking p) -> {
                // 3순위: 거리 (★★★ 수정된 finalDistanceMap 사용)
                return finalDistanceMap.getOrDefault(p.getParkingId(), Double.MAX_VALUE);
            }));

        // --- 9. 최종 DTO 변환 (기존 로직과 유사) ---
        return filteredList.stream()
                .map(parking -> {
                    boolean isRecommended = businessParkingIds.contains(parking.getParkingId()); 
                    int selectionCount = Optional.ofNullable(statMap.get(parking.getParkingId()))
                                                  .map(ParkingStat::getSelectionCount)
                                                  .orElse(0); // [cite: 714]
                    // ★★★ 수정된 finalDistanceMap 사용
                    double distance = finalDistanceMap.getOrDefault(parking.getParkingId(), 0.0);
                    String additionalText = infoMap.get(parking.getParkingId());

                    // ParkingResponseDto의 정적 팩토리 메서드 사용 [cite: 564]
                    return ParkingResponseDto.from(
                        parking,
                        isRecommended,
                        selectionCount,
                        distance,
                        additionalText
                    );
                })
                .collect(Collectors.toList());
    }

    // [신규] 수동 등록용 주차장 Get or Create 로직
    @Transactional
    public IntegratedParking getOrCreateManualParking(ManualParkingRequestDto requestDto) {
        
        // 1. Tmap PKey가 있고, 그걸로 찾을 수 있다면, 그걸 사용 (가장 좋음)
        if (requestDto.getTmapPkey() != null) {
            Optional<IntegratedParking> existingByPKey = integratedParkingRepository.findByTmapPkey(requestDto.getTmapPkey());
            if (existingByPKey.isPresent()) {
                // TmapPkey로 찾았는데 이름/주소가 다를 수 있으니 업데이트
                IntegratedParking parking = existingByPKey.get();
                parking.updateParkingInfo(requestDto.getParkingName(), requestDto.getAddress(), parking.isPublic(), parking.isFree());
                return parking;
            }
        }

        // 2. 좌표로 찾을 수 있다면, 그걸 사용
        Optional<IntegratedParking> existingByCoords = integratedParkingRepository.findByLatitudeAndLongitude(
            requestDto.getLatitude(),
            requestDto.getLongitude()
        );
        if (existingByCoords.isPresent()) {
            // 좌표로 찾았으면 tmapPkey가 null일 수 있으니 연결
            IntegratedParking parking = existingByCoords.get();
            if (requestDto.getTmapPkey() != null) {
                 parking.updateTmapPkey(requestDto.getTmapPkey()); 
            }
            return parking;
        }

        // 3. PKey로도, 좌표로도 못 찾음 -> 신규 주차장 생성
        String newParkingId = UUID.randomUUID().toString();
        IntegratedParking newParking = IntegratedParking.builder()
                .parkingId(newParkingId)
                .name(requestDto.getParkingName()) // ★ 사용자가 입력한 이름
                .address(requestDto.getAddress())
                .latitude(requestDto.getLatitude())
                .longitude(requestDto.getLongitude())
                .tmapPkey(requestDto.getTmapPkey())
                // isPublic, isFree는 알 수 없으므로 기본값(false)
                .build();

        // 4. 새 주차장에 대한 통계(ParkingStat) 레코드도 생성
        ParkingStat newStat = ParkingStat.builder()
                .integratedParking(newParking)
                .selectionCount(0)
                .build();

        integratedParkingRepository.save(newParking);
        parkingStatRepository.save(newStat);

        return newParking;
    }

    // ★★★ (신규) Haversine formula를 이용한 거리 계산 메서드 (km 단위) ★★★
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // km 단위 거리
    }

    // ▼▼▼ [신규] 주소에서 '자치구명' (두 번째 단어)을 추출하는 헬퍼 메서드 ▼▼▼
    private String parseDistrictFromAddress(String address) {
        if (address == null || address.isEmpty()) return "";
        String[] parts = address.split(" ");
        // "서울시 강남구 역삼동..." -> "강남구"
        if (parts.length > 1) return parts[1];
        return parts[0];
    }
    // ▲▲▲ [신규] 헬퍼 추가 완료 ▲▲▲

    // ▼▼▼ [신규] 서울시 API 호출 헬퍼 메서드 (비동기) ▼▼▼
    // (GetParkInfo API, '자치구명' ADDR 파라미터 사용)
    private CompletableFuture<SeoulParkingDto> fetchSeoulStaticInfo(String districtName) {
        String apiUrl = String.format(
            "http://openapi.seoul.go.kr:8088/%s/json/GetParkInfo/1/1000/%s", // (결과 수를 1000으로 늘림)
            seoulStaticKey, 
            districtName // "강남구" 등 자치구명 전달
        );

        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(SeoulParkingDto.class)
                .doOnError(error -> log.error("[API Fail] Seoul Static API Error: {}", error.getMessage()))
                .onErrorReturn(new SeoulParkingDto()) // (실패 시 빈 객체 반환)
                .toFuture();
                
    }
    // ▲▲▲ [신규] 헬퍼 추가 완료 ▲▲▲

    // ▼▼▼ [신규] 2. 실시간 API (GetParkingInfo) 호출 헬퍼 ▼▼▼
    private CompletableFuture<SeoulRealtimeDto> fetchSeoulRealtimeInfo(String districtName) {
        String apiUrl = String.format(
            "http://openapi.seoul.go.kr:8088/%s/json/GetParkingInfo/1/1000/%s", 
            seoulRealtimeKey, 
            districtName 
        );

        return webClient.get()
                .uri(apiUrl)
                .retrieve()
                .bodyToMono(SeoulRealtimeDto.class) // (★ GetParkingInfo용 새 DTO 사용)
                .doOnError(error -> log.error("[API Fail] Seoul Realtime API Error: {}", error.getMessage()))
                .onErrorReturn(new SeoulRealtimeDto())
                .toFuture(); // Mono를 CompletableFuture로 변환
    }
    // ▲▲▲ [신규] 헬퍼 추가 완료 ▲▲▲

    // ▼▼▼ [신규] 좌표 기반 매칭 헬퍼 메서드 ▼▼▼
    private SeoulParkingDto.Row findMatchingParking(
            SeoulParkingDto seoulDto, 
            BigDecimal dbLat, 
            BigDecimal dbLon
    ) {
        if (seoulDto.getGetParkInfo() == null || seoulDto.getGetParkInfo().getRow() == null) {

            // ▼▼▼ [디버깅 로그 1] API 응답이 비어있는지 확인 ▼▼▼
            log.info("[Debug] findMatchingParking: 서울시 API 응답이 비어있거나 'row'가 없습니다.");
            // ▲▲▲ [디버깅 로그 1] 완료 ▲▲▲

            return null; // 서울시 API 응답이 비어있으면 null 반환
        }

        SeoulParkingDto.Row bestMatch = null;
        double minDistance = Double.MAX_VALUE;

        // ▼▼▼ [디버깅 로그 2] DB 좌표를 한 번만 출력 ▼▼▼
        log.info("[Debug] findMatchingParking: DB 좌표 ({}, {})와 비교 시작", dbLat, dbLon);
        // ▲▲▲ [디버깅 로그 2] 완료 ▲▲▲
        
        // '자치구명'으로 검색된 모든 주차장 목록(row)을 순회
        for (SeoulParkingDto.Row row : seoulDto.getGetParkInfo().getRow()) {
            if (row.getLatitude() != null && row.getLongitude() != null) {
                
                
                // DB 좌표와 API 좌표 간의 거리 계산 (기존 calculateDistance 재사용) [cite: 1136-1140]
                double distance = calculateDistance(
                        dbLat.doubleValue(), dbLon.doubleValue(),
                        row.getLatitude(), row.getLongitude()
                );

                // ▼▼▼ [디버깅 로그 3] 500m 이내의 모든 주차장 정보를 로그로 출력 ▼▼▼
                if (distance < 0.5) { // (기존 0.1km -> 0.5km로 로그 범위 확장)
                    log.info("[Debug] findMatchingParking: 500m 이내 후보 찾음: {} / 거리(km): {}", 
                             row.getParkingName(), distance);
                }
                // ▲▲▲ [디버깅 로그 3] 완료 ▲▲▲

                log.info("DB좌표: {}, {} / API좌표: {}, {} / 계산된 거리(km): {}", 
                 dbLat, dbLon, row.getLatitude(), row.getLongitude(), distance);

                // 0.1km (100미터) 이내이고, 이전에 찾은 것보다 가깝다면
                if (distance < 0.1 && distance < minDistance) { 
                    minDistance = distance;
                    bestMatch = row;
                }
            }
        }

        // ▼▼▼ [디버깅 로그 4] 최종 매칭 결과 출력 ▼▼▼
        if (bestMatch != null) {
            log.info("[Debug] findMatchingParking: ★★★ 최종 매칭 성공: {} (거리: {}km)", 
                     bestMatch.getParkingName(), minDistance);
        } else {
            log.warn("[Debug] findMatchingParking: 100m 이내 매칭되는 주차장을 찾지 못했습니다.");
        }
        // ▲▲▲ [디버깅 로그 4] 완료 ▲▲▲

        return bestMatch; // (100미터 이내에서 가장 가까운 주차장 1개를 반환)
    }
    // ▲▲▲ [신규] 헬퍼 추가 완료 ▲▲▲

    // ▼▼▼ [신규] 4. 주차장 코드로 매칭하는 헬퍼 ▼▼▼
    private SeoulRealtimeDto.Row findMatchingRealtimeRow(SeoulRealtimeDto realtimeDto, String parkingCode) {
        if (realtimeDto.getGetParkingInfo() == null || realtimeDto.getGetParkingInfo().getRow() == null || parkingCode == null) {
            return null;
        }
        
        // 실시간 목록(row)을 순회하며 parkingCode가 일치하는 첫 번째 항목을 찾음
        SeoulRealtimeDto.Row match = realtimeDto.getGetParkingInfo().getRow().stream()
                .filter(row -> parkingCode.equals(row.getParkingCode())) 
                .findFirst()
                .orElse(null);

        log.info("[Debug] 2차 코드 매칭 결과 (코드: {}): {}", parkingCode, (match != null ? match.getParkingName() : "실패"));
        return match;
    }
    // ▲▲▲ [신규] 헬퍼 추가 완료 ▲▲▲

    // ▼▼▼ [대폭 수정] 상세 정보 API 메인 메서드 (비동기 적용) ▼▼▼
    @Transactional(readOnly = true)
    public ParkingDetailResponseDto getParkingDetails(String parkingId) {
        
        // 1. (DB) 필수 정보 조회
        IntegratedParking parking = integratedParkingRepository.findById(parkingId)
                .orElseThrow(() -> new EntityNotFoundException("주차장 정보를 찾을 수 없습니다: " + parkingId));
        
        ParkingStat stat = parkingStatRepository.findByIntegratedParking_ParkingId(parkingId).orElse(null); 
        BusinessInfo info = businessInfoRepository.findByIntegratedParking(parking).orElse(null); 

        // 2. (외부 API) DB 주소에서 '자치구명' 추출
        String district = parseDistrictFromAddress(parking.getAddress());
        
        // 3. (외부 API) ★★★ 두 API를 동시에 병렬 호출 ★★★
        CompletableFuture<SeoulParkingDto> staticFuture = fetchSeoulStaticInfo(district);
        CompletableFuture<SeoulRealtimeDto> realtimeFuture = fetchSeoulRealtimeInfo(district);

        // 4. (대기) 두 API의 응답이 모두 올 때까지 대기
        CompletableFuture.allOf(staticFuture, realtimeFuture).join();

        try {
            // 5. (결과) 응답 데이터 DTO 객체로 가져오기
            SeoulParkingDto staticData = staticFuture.get();
            SeoulRealtimeDto realtimeData = realtimeFuture.get();

            // 6. (1차 매칭 - 좌표) 정적(Static) 데이터에서 좌표로 주차장 찾기
            SeoulParkingDto.Row matchedStaticRow = findMatchingParking(staticData, parking.getLatitude(), parking.getLongitude());

            // 7. (변수 선언)
            String priceInfo = null;
            String operatingHours = null;
            Integer availableSpots = null;
            String parkingCode = null; // (주차장 코드를 저장할 변수)

            // 8. (2차 매칭 준비) 1차 매칭(좌표)에 성공했다면
            if (matchedStaticRow != null) {
                // 8a. 정적 정보(요금, 시간) 가공
                if (matchedStaticRow.getRates() != null && matchedStaticRow.getRates() > 0) {
                    priceInfo = String.format("%.0f분 %.0f원 (추가 %.0f분 %.0f원)", 
                                    matchedStaticRow.getTimeRate(), matchedStaticRow.getRates(), 
                                    matchedStaticRow.getAddTimeRate(), matchedStaticRow.getAddRates());
                } else if ("N".equals(matchedStaticRow.getPayYn())) {
                    priceInfo = "무료";
                }
                operatingHours = String.format("평일 %s ~ %s", 
                                    matchedStaticRow.getWeekdayBeginTime(), matchedStaticRow.getWeekdayEndTime());
                
                // 8b. ★★★ 매칭된 주차장의 '주차장 코드' 추출 ★★★
                parkingCode = matchedStaticRow.getParkingCode(); 
            }

            // 9. (2차 매칭 - 코드) 1차 매칭에서 '주차장 코드'를 찾았다면
            if (parkingCode != null) {
                // 9a. 실시간(Realtime) 데이터 목록에서 '주차장 코드'로 매칭
                SeoulRealtimeDto.Row matchedRealtimeRow = findMatchingRealtimeRow(realtimeData, parkingCode);
                
                if (matchedRealtimeRow != null) {
                    // 9b. 실시간 정보(주차 대수) 가공
                    if (matchedRealtimeRow.getCapacity() != null && matchedRealtimeRow.getCurParking() != null) {
                        availableSpots = matchedRealtimeRow.getCapacity() - matchedRealtimeRow.getCurParking();
                        if (availableSpots < 0) availableSpots = 0;
                    }
                }
            }

            // 10. (결합) 최종 DTO 빌드
            return ParkingDetailResponseDto.builder()
                    .parkingId(parking.getParkingId())
                    .name(parking.getName())
                    .address(parking.getAddress())
                    .selectionCount(stat != null ? stat.getSelectionCount() : 0)
                    .additionalText(info != null ? info.getAdditionalText() : null)
                    .priceInfo(priceInfo)         // (API 1의 정보)
                    .operatingHours(operatingHours) // (API 1의 정보)
                    .availableSpots(availableSpots) // (API 2의 정보)
                    .build();

        } catch (Exception e) {
            // (비동기 예외 처리)
            log.error("API 응답 집계 중 오류 발생", e);
            throw new RuntimeException("상세 정보 집계 중 오류가 발생했습니다.", e);
        }
    }
    // ▲▲▲ [대폭 수정] 완료 ▲▲▲

    
}
