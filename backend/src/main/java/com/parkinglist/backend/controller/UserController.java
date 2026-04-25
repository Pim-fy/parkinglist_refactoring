package com.parkinglist.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.parkinglist.backend.dto.UserDto;
import com.parkinglist.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;
    // 회원가입
    @PostMapping("/register")
    public ResponseEntity<UserDto.RegisterResponse> register(@RequestBody UserDto.RegisterRequest request) {
        try {
            userService.register(request);
            UserDto.RegisterResponse response = new UserDto.RegisterResponse(true, "회원가입이 완료되었습니다.");
            return ResponseEntity.ok(response);
        } catch(IllegalArgumentException e) {
            log.warn("회원가입 실패 : {}", e.getMessage());
            UserDto.RegisterResponse response = new UserDto.RegisterResponse(false, e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserDto.LoginResponse> login(@RequestBody UserDto.LoginRequest request) {
        UserDto.LoginResponse response = userService.login(request);

        if(response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }
}
