package com.swna.javafx.config.filter;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClientRequestException;

import com.swna.javafx.common.exception.ApiException;
import com.swna.javafx.common.exception.NetworkException;
import com.swna.javafx.common.response.ApiResponse;

import reactor.core.publisher.Mono;

@Component
public class ErrorHandlingWebClientFilter implements ExchangeFilterFunction {

    @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        return next.exchange(request)

                // 🔥 네트워크 에러 변환
                .onErrorMap(
                        WebClientRequestException.class,
                        ex -> new NetworkException("서버에 연결할 수 없습니다.")
                )

                .flatMap(response -> {

                    // 🔥 2xx → 그대로 통과
                    if (response.statusCode().is2xxSuccessful()) {
                        return Mono.just(response);
                    }

                    // 🔥 서버 에러(ApiResponse 기반 파싱)
                    return response.bodyToMono(new ParameterizedTypeReference<ApiResponse<Object>>() {})
                            .flatMap(body -> Mono.error(
                                    new ApiException(body.code(), body.message())
                            ));
                });
    }
}
