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
    @Primary // 동일한 타입의 빈이 여러 개일 때 우선적으로 주입되도록 설정
    public WebClient webClient(
            ApiProperties apiProperties,         // API 기본 정보 (URL 등)
            AuthWebClientFilter authFilter,      // 인증 토큰 추가 필터
            LoggingWebClientFilter loggingFilter, // 요청/응답 로깅 필터
            ReAuthWebClientFilter reAuthFilter,  // 401 에러 시 토큰 재발급 및 재시도 필터
            ErrorHandlingWebClientFilter errorFilter // 에러 응답 표준화 필터
        ) {

        // 하위 Netty HttpClient 설정 (타임아웃 등 네트워크 레벨 설정)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5)); // 응답 지연 5초 제한

        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl()) // 기본 호스트 주소 설정
                .clientConnector(new ReactorClientHttpConnector(httpClient)) // 생성한 HttpClient 연결

                /* 
                 * 필터 적용 순서 (중요): 
                 * --- 요청(Request) 시 실행 순서 (위 -> 아래) --- 
                 * --- 응답(Response) 시 실행 순서 (아래 -> 위) ---
                 */
                .filter(loggingFilter.logRequest())
                .filter(loggingFilter.logResponse())
                
                .filter(authFilter.authFilter()) // 3. (응답) 마지막 처리

                // 🔴 중요: ErrorFilter를 먼저 등록
                .filter(errorFilter)            // 2. (응답) ReAuth 실패 시 여기서 에러 변환

                // 🔴 중요: ReAuthFilter를 나중에 등록
                .filter(reAuthFilter)           // 1. (응답) 가장 먼저 401인지 확인하고 재시도
                
                .build();
    }

    /**
     * 인증 전용 WebClient 빈 (필터 없음)
     * 로그인, 토큰 재발급 등 인증 관련 API 호출 전용
     */
    @Bean
    public WebClient authWebClient(ApiProperties apiProperties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10)); // 인증은 좀 더 여유있게

        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}