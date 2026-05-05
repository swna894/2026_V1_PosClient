package com.swna.javafx.barcode.infrastructre;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductApiClient {

    private final WebClient webClient;

    /**
     * 서버에서 라벨 데이터 조회
     */
    public Mono<List<ProductLabelDto>> getLabels() {

        log.info("REQUEST LABEL DATA");

        return webClient.get()
                .uri("/products/labels")
                .retrieve()

                // HTTP 에러 처리
                .onStatus(
                        status -> status.is4xxClientError(),
                        response -> {
                            log.error( "CLIENT ERROR : {}", response.statusCode() );
                            return Mono.error( new RuntimeException(  "CLIENT ERROR" ) );
                        }
                )

                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> {
                            log.error( "SERVER ERROR : {}",  response.statusCode() );
                            return Mono.error( new RuntimeException(  "SERVER ERROR" )  );
                        }
                )

                // JSON -> DTO
                .bodyToFlux(ProductLabelDto.class)
                .collectList()
                // timeout
                .timeout(Duration.ofSeconds(5))
                // 요청 로그
                .doOnSubscribe(subscription -> {
                    log.info(  "API REQUEST START" );
                })

                // 성공 로그
                .doOnSuccess(result -> {
                    log.info( "API REQUEST SUCCESS size={}", result.size() );
                })

                // 실패 로그
                .doOnError(error -> {
                    log.error( "API REQUEST ERROR", error  );
                })

                // fallback
                .onErrorResume(error -> {
                    log.warn(    "RETURN EMPTY LABEL LIST"  );
                    return Mono.just(List.of());
                });
    }
}
