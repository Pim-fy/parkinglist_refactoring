package com.parkinglist.backend.config.jwt;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import com.parkinglist.backend.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;


@Component
@Slf4j
public class JwtTokenProvider {
    private final SecretKey key;
    private final long expirationMs;
    private final UserRepository userRepository;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secretKey,
        @Value("${jwt.expiration-ms}") long expirationMs,
        UserRepository userRepository) {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            this.key = Keys.hmacShaKeyFor(keyBytes);
            this.expirationMs = expirationMs;
            this.userRepository = userRepository;
        }
    public String createToken(com.parkinglist.backend.entity.User user) {
        String username = user.getUserId();
        String authorities = user.getRole().name();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
            .subject(username)
            .claim("auth", authorities)
            .issuedAt(now)
            .expiration(expiryDate)
            .signWith(key)
            .compact();
    }
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        if(claims.get("auth") == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(claims.get("auth").toString()));

        // 토큰에서 userId (Subject)를 가져옵니다.
        String userId = claims.getSubject();

        // userId를 사용해 DB에서 실제 User 엔티티를 조회합니다.
        com.parkinglist.backend.entity.User userEntity = userRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("토큰에 해당하는 사용자를 찾을 수 없습니다. ID: " + userId));

        // Spring Security의 User 객체 대신, DB에서 가져온 'userEntity'를 principal로 사용합니다.
        return new UsernamePasswordAuthenticationToken(userEntity, "", authorities);
    }
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (io.jsonwebtoken.security.SecurityException | MalformedJwtException e) {
            log.info("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.info("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 잘못되었습니다.");
        }
        return false;
    }
    private Claims parseClaims(String accessToken) {
        try {
            return Jwts.parser().verifyWith(key).build().parseSignedClaims(accessToken).getPayload();
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이라도 Claims는 필요하므로 반환
            return e.getClaims();
        }
    }    
}
