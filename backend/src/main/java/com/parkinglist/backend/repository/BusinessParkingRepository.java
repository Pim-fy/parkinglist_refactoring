package com.parkinglist.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkinglist.backend.entity.BusinessParking;
import com.parkinglist.backend.entity.BusinessLocation;

public interface BusinessParkingRepository extends JpaRepository<BusinessParking, Long> {
    List<BusinessParking> findByBusinessLocation(BusinessLocation businessLocation);

    List<BusinessParking> findByBusinessLocation_Id(Long locationId);
}
