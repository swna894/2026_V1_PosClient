package com.swna.javafx.config;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import com.swna.javafx.common.store.TokenStore;

@Component
public class AuthWebClientFilter {

    private final TokenStore tokenStore;

    public AuthWebClientFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    public ExchangeFilterFunction authFilter() {
        return (request, next) -> {

            String path = request.url().getPath();

            // 🔥 auth API는 제외
            if (path.startsWith("/auth")) {
                return next.exchange(request);
            }

            String token = tokenStore.getAccessToken();

            ClientRequest authRequest = ClientRequest.from(request)
                    .headers(h -> {
                        if (token != null) {
                            h.setBearerAuth(token);
                        }
                    })
                    .build();

            return next.exchange(authRequest);
        };
    }
}
