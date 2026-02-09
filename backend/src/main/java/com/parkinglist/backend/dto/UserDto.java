package com.parkinglist.backend.dto;

import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.entity.enums.UserRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

public class UserDto {
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class LoginRequest {
        private String userId;
        private String password;
    }
    @Getter
    @AllArgsConstructor     // 클래스의 모든 필드를 파라미터로 받는 생성자를 자동으로 생성
    @NoArgsConstructor      // 파라미터 없는 기본 생성자를 자동으로 생성
    public static class LoginResponse {
        private boolean success;
        private String message;
        private String token;       // JWT 토큰 필드
        private String role;
    }
    @Getter
    @Setter
    public static class RegisterRequest {
        private String userId;
        private String password;
        private String email;
        private UserRole role;

        public User toUserEntity(String encodedPassword) {
            return User.builder()
                    .userId(this.userId)
                    .password(encodedPassword)
                    .email(this.email)
                    .role(this.role)
                    .build();
        }
    }
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegisterResponse {
        private boolean success;
        private String message;
    }
}
