package com.swna.javafx.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    @Primary
    public WebClient webClient(
            AuthWebClientFilter authFilter,
            LoggingWebClientFilter loggingFilter,
            ReAuthWebClientFilter reAuthFilter
    ) {

        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(5));

        return WebClient.builder()
                .baseUrl("http://cafe7788.cafe24.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))

                // 로그
                .filter(loggingFilter.logRequest())
                .filter(loggingFilter.logResponse())

                // JWT attach
                .filter(authFilter.authFilter())

                // 401 retry
                .filter(reAuthFilter)

                .build();
    }

    @Bean
    public WebClient authWebClient() {
        return WebClient.builder()
                .baseUrl("http://cafe7788.cafe24.com")
                .build();
}
}