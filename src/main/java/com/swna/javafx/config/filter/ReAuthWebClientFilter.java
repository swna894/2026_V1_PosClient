package com.swna.javafx.config.filter;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import com.swna.javafx.service.auth.ReAuthService;

import reactor.core.publisher.Mono;

@Component
public class ReAuthWebClientFilter implements ExchangeFilterFunction {

    private final ReAuthService reAuthService;

    public ReAuthWebClientFilter(ReAuthService reAuthService) {
        this.reAuthService = reAuthService;
    }

   @Override
    public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {

        return next.exchange(request)
                .flatMap(response -> {

                    if (response.statusCode().value() == 401) {

                        return reAuthService.refreshToken()
                                .flatMap(token -> {

                                    ClientRequest newReq = ClientRequest.from(request)
                                            .headers(h -> h.setBearerAuth(token.accessToken()))
                                            .build();

                                    return next.exchange(newReq);
                                });
                    }

                    return Mono.just(response);
                });
    }
}
