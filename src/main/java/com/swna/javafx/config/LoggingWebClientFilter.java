package com.swna.javafx.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import reactor.core.publisher.Mono;

@Component
public class LoggingWebClientFilter {

    // =========================
    // 🔍 요청 로그
    // =========================
    public ExchangeFilterFunction logRequest() {
        return (request, next) -> {

            System.out.println("➡️ REQUEST: " + request.method() + " " + request.url());

            request.headers().forEach((name, values) ->
                    values.forEach(v -> System.out.println(name + ": " + v))
            );

            return next.exchange(request);
        };
    }

    // =========================
    // 🔍 응답 로그
    // =========================
    public ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {

            System.out.println("⬅️ RESPONSE: " + response.statusCode());

            return Mono.just(response);
        });
    }
}