package com.swna.javafx.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.swna.javafx.config.filter.AuthWebClientFilter;
import com.swna.javafx.config.filter.ErrorHandlingWebClientFilter;
import com.swna.javafx.config.filter.LoggingWebClientFilter;
import com.swna.javafx.config.filter.ReAuthWebClientFilter;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

/**
 * 외부 API 통신을 위한 WebClient 설정 클래스
 */
@Configuration
public class WebClientConfig {

    /**
     * 애플리케이션에서 주로 사용할 메인 WebClient 빈
     * 다양한 필터(로깅, 인증, 에러 핸들링)가 적용되어 있습니다.
     */
    @Bean
    @Primary
    public WebClient webClient(
            ApiProperties apiProperties,
            AuthWebClientFilter authFilter,
            LoggingWebClientFilter loggingFilter,
            ReAuthWebClientFilter reAuthFilter,
            ErrorHandlingWebClientFilter errorFilter
        ) {

        // 하위 Netty HttpClient 설정 (타임아웃, 버퍼 등 네트워크 레벨 설정)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(30))  // 응답 타임아웃 5초 → 30초로 증가
                .doOnConnected(conn -> 
                    conn.addHandlerLast(new ReadTimeoutHandler(30))
                       .addHandlerLast(new WriteTimeoutHandler(30))
                );

        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // 🔥 중요: 버퍼 크기를 20MB로 증가 (기본값 256KB)
                    configurer.defaultCodecs().maxInMemorySize(20 * 1024 * 1024);
                })
                /* 
                 * 필터 적용 순서 (중요): 
                 * --- 요청(Request) 시 실행 순서 (위 -> 아래) --- 
                 * --- 응답(Response) 시 실행 순서 (아래 -> 위) ---
                 */
                .filter(loggingFilter.logRequest())
                .filter(loggingFilter.logResponse())
                
                .filter(authFilter.authFilter())

                // 🔴 중요: ErrorFilter를 먼저 등록
                .filter(errorFilter)

                // 🔴 중요: ReAuthFilter를 나중에 등록
                .filter(reAuthFilter)
                
                .build();
    }

    /**
     * 인증 전용 WebClient 빈 (필터 없음)
     * 로그인, 토큰 재발급 등 인증 관련 API 호출 전용
     */
    @Bean
    public WebClient authWebClient(ApiProperties apiProperties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10));

        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> {
                    // 인증 응답도 버퍼 크기 증가
                    configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024);
                })
                .build();
    }
}