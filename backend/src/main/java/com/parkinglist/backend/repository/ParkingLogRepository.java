package com.parkinglist.backend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.parkinglist.backend.entity.ParkingLog;
import com.parkinglist.backend.entity.User;

public interface ParkingLogRepository extends JpaRepository<ParkingLog, Long> {
    List<ParkingLog> findByUser(User user);
}
