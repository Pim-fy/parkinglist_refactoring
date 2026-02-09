package com.parkinglist.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient() {
        // ▼▼▼ [수정] 버퍼 사이즈를 10MB로 늘리는 설정 추가 ▼▼▼
        final int bufferSize = 10 * 1024 * 1024; // 10MB

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(bufferSize)) // (기본 256KB -> 10MB)
                .build();
        
        return WebClient.builder()
                .exchangeStrategies(strategies) // (위에서 만든 설정을 적용)
                .build();
        // ▲▲▲ [수정] 완료 ▲▲▲
    }
}
