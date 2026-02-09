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
public class BusinessInfo {
    // 추가 제공 정보 테이블 고유 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "info_id")
    private Long id;

    // 주차장(FK)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_id", nullable = false, unique = true)
    private IntegratedParking integratedParking;

    // 추가 제공 정보 텍스트
    @Column(name = "additional_text", columnDefinition = "TEXT")
    private String additionalText;

    public void updateText(String additionalText) {
        this.additionalText = additionalText;
    }
}
