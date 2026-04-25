package com.parkinglist.backend.service;

import org.springframework.stereotype.Service;
import com.parkinglist.backend.dto.UserDto;
import com.parkinglist.backend.entity.User;
import com.parkinglist.backend.repository.UserRepository;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.parkinglist.backend.config.jwt.JwtTokenProvider;

@Slf4j
@Service
@RequiredArgsConstructor        // final 키워드 붙은 필드를 인자로 받는 생성자 자동 생성
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 로그인 처리
    public UserDto.LoginResponse login(UserDto.LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUserId(request.getUserId());

        if(userOpt.isEmpty()) {
            log.debug("로그인 시도 : loginId = {} -> 사용자 없음", request.getUserId());
            return new UserDto.LoginResponse(
                false, 
                "ID또는 비밀번호가 일치하지 않습니다",
                null,
                null
            );
        }
        User user = userOpt.get();
        String rawPassword = request.getPassword();
        String storedHash = user.getPassword();

        if(passwordEncoder.matches(rawPassword, storedHash)) {
            String token = jwtTokenProvider.createToken(user);
            String role = user.getRole().name();
            log.info("로그인 성공 : loginId = {}", request.getUserId());
            return new UserDto.LoginResponse(true, "로그인 성공.", token, role);
        } else {
            log.warn("로그인 실패(비밀번호 불일치) : loginId = {}", request.getUserId());
            return new UserDto.LoginResponse(false, "ID또는 비밀번호가 일치하지 않습니다", null, null);
        }
    }

    // 회원가입 처리
    @Transactional
    public void register(UserDto.RegisterRequest request) {
        if(userRepository.findByUserId(request.getUserId()).isPresent()) {
            log.warn("회원가입 실패(중복 아이디) : loginId = {}", request.getUserId());
            throw new IllegalArgumentException("이미 사용 중인 ID입니다.");
        }
        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 엔티티 변환 및 저장
        User user = User.builder()
                        .userId(request.getUserId())
                        .password(encodedPassword)
                        .email(request.getEmail())
                        .role(request.getRole())
                        .build();
        userRepository.save(user);
        log.info("회원가입 성공 : loginId = {}", request.getUserId());
    }

}
