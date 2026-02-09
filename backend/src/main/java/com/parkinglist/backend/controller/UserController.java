package com.parkinglist.backend.controller;

import java.util.Optional;
import com.parkinglist.backend.entity.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.parkinglist.backend.config.jwt.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;
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
        Optional<User> userOpt = userService.login(request);
        if(userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtTokenProvider.createToken(user);
            String role = user.getRole().name();
            log.info("로그인 성공 : userId = {}", request.getUserId());
            UserDto.LoginResponse response = new UserDto.LoginResponse(true, "로그인 성공.", token, role);
            return ResponseEntity.ok(response);
        } else {
            log.warn("로그인 실패 : userId = {}", request.getUserId());
            UserDto.LoginResponse response = new UserDto.LoginResponse(false, "ID 또는 비밀번호가 일치하지 않습니다.", null, null);
            return ResponseEntity.status(401).body(response);
        }
    }
}
