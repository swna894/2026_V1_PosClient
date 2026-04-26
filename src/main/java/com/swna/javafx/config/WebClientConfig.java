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

@Configuration
public class WebClientConfig {

    @Bean
    @Primary
    public WebClient webClient(
            ApiProperties apiProperties,
            AuthWebClientFilter authFilter,
            LoggingWebClientFilter loggingFilter,
            ReAuthWebClientFilter reAuthFilter,
            ErrorHandlingWebClientFilter errorFilter
        ) {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                // 1. 로그 (맨 처음, 맨 마지막) - 순서 중요!
                .filter(loggingFilter.logRequest())
                .filter(loggingFilter.logResponse())

                // 2. 인증
                .filter(authFilter.authFilter())

                // 3. 401 재발급
                .filter(reAuthFilter)

                // 4. 🔥 에러 표준화 (맨 마지막)
                .filter(errorFilter)

                .build();
    }

    @Bean
    public WebClient authWebClient(ApiProperties apiProperties) {
        return WebClient.builder()
                .baseUrl(apiProperties.getBaseUrl())
                .build();
    }
}