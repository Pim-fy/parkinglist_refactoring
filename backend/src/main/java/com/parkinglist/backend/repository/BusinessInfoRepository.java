package com.parkinglist.backend.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkinglist.backend.entity.BusinessInfo;
import com.parkinglist.backend.entity.IntegratedParking;

public interface BusinessInfoRepository extends JpaRepository<BusinessInfo, Long> {
    Optional<BusinessInfo> findByIntegratedParking(IntegratedParking integratedParking);
}
