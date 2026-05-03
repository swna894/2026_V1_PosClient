package com.swna.javafx.config;

import java.time.Duration;

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
                 * 필터는 등록된 순서대로 요청(Request)을 처리하고, 
                 * 반대 순서로 응답(Response)을 처리합니다.
                 */

                // 1. 로그 필터: 요청의 시작과 응답의 끝을 기록
                .filter(loggingFilter.logRequest())
                .filter(loggingFilter.logResponse())

                // 2. 인증 필터: 헤더에 Authorization 토큰 등을 삽입
                .filter(authFilter.authFilter())

                // 3. 재인증 필터: 만약 401 Unauthorized가 발생하면 토큰을 갱신하고 재요청 수행
                .filter(reAuthFilter)

                // 4. 에러 표준화 필터: 각기 다른 외부 에러 규격을 우리 시스템 표준 에러로 변환
                .filter(errorFilter)

                .build();
    }

    /**
     * 인증 전용 WebClient 빈
     * 로그인이나 토큰 재발급 등 '인증' 자체를 위해 호출할 때는 
     * 무한 루프(재인증 필터의 중복 실행)를 방지하기 위해 필터가 없는 클라이언트를 사용합니다.
     */
    @Bean
    public WebClient authWebClient(ApiProperties apiProperties) {
        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .build();
    }
}