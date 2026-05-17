package com.swna.javafx.config.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class LoggingWebClientFilter {

    // =========================
    // 🔍 요청 로그
    // =========================
    public ExchangeFilterFunction logRequest() {
        return (request, next) -> {
       
            log.info("REQUEST: = {}", request.method() + " " + request.url());
            request.headers().forEach((name, values) -> values.forEach(v -> log.info("{} : {}", name , v)));
            return next.exchange(request);
        };
    }

    // =========================
    // 🔍 응답 로그
    // =========================
    public ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("RESPONSE: = {}", response.statusCode());
            return Mono.just(response);
        });
    }
}