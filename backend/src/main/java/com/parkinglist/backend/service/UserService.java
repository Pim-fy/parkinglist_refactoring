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

@Slf4j
@Service
@RequiredArgsConstructor        // final 키워드 붙은 필드를 인자로 받는 생성자 자동 생성
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 로그인 처리
    public Optional<User> login(UserDto.LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUserId(request.getUserId());

        if(userOpt.isEmpty()) {
            log.debug("로그인 시도 : loginId = {} -> 사용자 없음", request.getUserId());
            return Optional.empty();
        }
        User user = userOpt.get();
        String rawPassword = request.getPassword();
        String storedHash = user.getPassword();

        if(passwordEncoder.matches(rawPassword, storedHash)) {
            log.info("로그인 성공 : loginId = {}", request.getUserId());
            return Optional.of(user);       // 성공 -> User 객체 반환
        } else {
            log.warn("로그인 실패(비밀번호 불일치) : loginId = {}", request.getUserId());
            return  Optional.empty();       // 실패 -> 빈 Optional 반환
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
        User user = request.toUserEntity(encodedPassword);
        userRepository.save(user);
        log.info("회원가입 성공 : loginId = {}", request.getUserId());
    }

}
