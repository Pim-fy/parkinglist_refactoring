package com.parkinglist.backend.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parkinglist.backend.entity.BusinessLocation;
import com.parkinglist.backend.entity.User;

public interface BusinessLocationRepository extends JpaRepository<BusinessLocation, Long> {
    // 사업자(User)로 등록된 모든 사업장 목록을 조회
    List<BusinessLocation> findByBusinessUser(User businessUser);
    
    // 화면 안에 보이는 주차장만 필터링하여 조회.
    @Query("SELECT b FROM BusinessLocation b WHERE " +
           "b.latitude BETWEEN :latMin AND :latMax AND " +
           "b.longitude BETWEEN :lonMin AND :lonMax")
    List<BusinessLocation> findByCoordinateRange(
        @Param("latMin") BigDecimal latMin, 
        @Param("latMax") BigDecimal latMax, 
        @Param("lonMin") BigDecimal lonMin, 
        @Param("lonMax") BigDecimal lonMax
    );
}
