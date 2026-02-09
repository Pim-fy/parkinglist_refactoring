package com.parkinglist.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkinglist.backend.entity.ParkingStat;

public interface ParkingStatRepository extends JpaRepository<ParkingStat, Long> {
    Optional<ParkingStat> findByIntegratedParking_ParkingId(String parkingId);
}
