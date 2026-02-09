package com.parkinglist.backend.config;

import java.util.Arrays;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.parkinglist.backend.config.jwt.JwtAuthenticationFilter;
import com.parkinglist.backend.config.jwt.JwtTokenProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // SecurityFilterChain에 CORS 설정을 추가합니다.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            //  CSRF 비활성화 (기존)
            .csrf(csrf -> csrf.disable())

            //  세션 비활성화 (기존)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 전에 추가
            .addFilterBefore(
                new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class
            )

            //  HTTP 요청 권한 설정 (기존)
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parking/log").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/parking/recommend").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/parking/details/**").permitAll()
                .anyRequest().authenticated()
            );
        
        return http.build();
    }

    //  CORS 설정을 정의하는 Bean을 추가합니다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // React 앱의 주소(http://localhost:5173)를 허용
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        
        // 허용할 HTTP 메서드 (GET, POST, PUT, DELETE, OPTIONS 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 허용할 HTTP 헤더 (모든 헤더 허용)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // 자격 증명(쿠키 등)을 허용 (JWT 사용 시에도 필요할 수 있음)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 모든 경로("/**")에 대해 위에서 정의한 CORS 설정을 적용
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
