package com.parkinglist.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
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
public class ParkingStat {
    // 통계 테이블 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 통계 대상 주차장
    @OneToOne(fetch = FetchType.LAZY)   // 1 : 1 관계 설정
    @JoinColumn(name = "parking_id", nullable = false, unique = true)   // FK 컬럼명 설정, null불가, 유니크
    private IntegratedParking integratedParking;

    // 모든 유저의 누적 선택 횟수
    @Builder.Default
    @Column(name = "selection_count", nullable = false)
    private Integer selectionCount = 0;

    // 선택 횟수 1 증가 함수
    public void incrementCount() {
        this.selectionCount += 1;
    }
}
