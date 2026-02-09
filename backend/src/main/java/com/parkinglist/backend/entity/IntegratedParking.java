package com.parkinglist.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)      // JPA를 위한 기본 생성자
@AllArgsConstructor                                     // @Builder를 위한 모든 필드 생성자
@Builder
@EntityListeners(AuditingEntityListener.class)
public class IntegratedParking {
    @Id
    @Column(name = "parking_id", length = 36)
    private String parkingId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255, nullable = false)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(name = "tmap_pkey", length = 20, unique = true)
    private String tmapPkey;

    // 공영주차장 여부
    @Builder.Default
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    // 무료주차장 여부
    @Builder.Default
    @Column(name = "is_free", nullable = false)
    private boolean isFree = false;

    // 데이터 최종 갱신 일시
    @LastModifiedDate
    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    // Tmap PK를 업데이트하는 메서드. 데이터 통합용
    public void updateTmapPkey(String tmapPkey) {
        this.tmapPkey = tmapPkey;
    }

    // 주차장 이름, 주소, 필터 정보 업데이트 메서드. 정보 갱신용
    public void updateParkingInfo(String name, String address, boolean isPublic, boolean isFree) {
        this.name = name;
        this.address = address;
        this.isPublic = isPublic;
        this.isFree = isFree;
    }
}
