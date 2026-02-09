package com.parkinglist.backend.entity;

import com.parkinglist.backend.entity.enums.UserRole;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity     // 엔터티 선언
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)  // JPA용 기본 생성자
@AllArgsConstructor     // 모든 필드를 인자로 받는 생성자 생성
@Builder
public class User {
    @Id     // 기본 키를 나타냄
    @GeneratedValue(strategy = GenerationType.IDENTITY)     // PK값 자동 생성
    private Long userNumber;
    private String userId;
    private String password;
    private String email;
    @Enumerated(EnumType.STRING)
    private UserRole role;
}
