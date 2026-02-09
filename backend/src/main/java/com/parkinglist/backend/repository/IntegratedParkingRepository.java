package com.parkinglist.backend.repository;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkinglist.backend.entity.IntegratedParking;

public interface IntegratedParkingRepository extends JpaRepository<IntegratedParking, String> {
    Optional<IntegratedParking> findByTmapPkey(String tmapPkey);
    Optional<IntegratedParking> findByLatitudeAndLongitude(BigDecimal latitude, BigDecimal longitude);
}
