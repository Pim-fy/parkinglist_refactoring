package com.parkinglist.backend.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
    // 동일 주차장을 추천하지 못하도록 제약조건 추가
    name = "business_parking", // 엔티티가 사용할 테이블 이름 (기존과 동일하게)
    uniqueConstraints = {
        @UniqueConstraint(
            name = "location_parking_unique",
            columnNames = {"location_id", "parking_id"} // 이 두 컬럼의 조합이 유일해야 함
        )
    }
)
public class BusinessParking {
    // 사업자 추천 테이블 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "business_parking_id")
    private Long id;

    // 추천한 사업장
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private BusinessLocation businessLocation;

    // 추천된 주차장(FK)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id", nullable = false)
    private IntegratedParking integratedParking;

    // 주차장을 등록한 사업자 회원 번호 (필수 컬럼)
    @Column(name = "business_user_number", nullable = false)
    private Long businessUserNumber; // Long 타입으로 가정

    // 등록 일시
    @CreatedDate
    @Column(name = "registered_date", nullable = false, updatable = false)
    private LocalDateTime registeredDate;
}
