package com.swna.javafx.common.api;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;

import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommonApiClient {
    
    private final WebClient webClient;
    
    // Default retry settings
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final Duration INITIAL_RETRY_DELAY = Duration.ofSeconds(1);
    
    // Timeout settings
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(60);
    
    // =========================
    // GET Request - Single Object (Mono)
    // =========================
    
    /**
     * GET request - Automatically unwraps ApiResponse wrapper (single object)
     */
    public <T> Mono<T> getForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getForData(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Automatically unwraps ApiResponse wrapper (single object, custom timeout)
     */
    public <T> Mono<T> getForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API GET] Request started - Path: {}, Query: {}", metadata.path(), queryParams);
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(timeout)
                .doOnSubscribe(sub -> log.debug("[API GET] Subscription started - Path: {}", metadata.path()))
                .doOnSuccess(response -> log.debug("[API GET] Response received - Path: {}, Success: {}", 
                    metadata.path(), response != null && response.isSuccess()))
                .doOnError(error -> log.error("[API GET] Request failed - Path: {}, Error: {}", 
                    metadata.path(), error.getMessage(), error))
                .flatMap(this::unwrapResponseWithDetails);
    }
    
    /**
     * GET request - Returns ApiResponse as is (single object)
     */
    public <T> Mono<ApiResponse<T>> getForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getForResponse(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Returns ApiResponse as is (single object, custom timeout)
     */
    public <T> Mono<ApiResponse<T>> getForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(timeout)
                .doOnSuccess(response -> logApiResponse(metadata.path(), response))
                .doOnError(error -> logApiError(metadata.path(), error))
                .retryWhen(getRetrySpec());
    }
    
    /**
     * GET request - Paginated response (without ApiResponse wrapper)
     */
    public <T> Mono<T> getForPage(
            ApiEndpointMapper.DomainMetadata<T> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getForPage(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Paginated response (without ApiResponse wrapper, custom timeout)
     */
    public <T> Mono<T> getForPage(
            ApiEndpointMapper.DomainMetadata<T> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        String uri = buildUriString(metadata.path(), pathVars, queryParams);
        log.debug("[API GET Page] Request - URI: {}", uri);
        
        return webClient.method(metadata.method())
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientErrorResponse)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerErrorResponse)
                .bodyToMono(metadata.typeRef())
                .timeout(timeout)
                .doOnSuccess(data -> log.debug("[API GET Page] Success - Path: {}", metadata.path()))
                .doOnError(error -> log.error("[API GET Page Error] Path: {}, URI: {}", 
                    metadata.path(), uri, error))
                .retryWhen(getRetrySpec());
    }

    // =========================
    // GET Request - List/Stream (Flux)
    // =========================
    
    /**
     * GET request - Automatically unwraps ApiResponse<List<T>> to Flux<T>
     */
    public <T> Flux<T> getFluxForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<List<T>>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getFluxForData(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Automatically unwraps ApiResponse<List<T>> to Flux<T> (custom timeout)
     */
    public <T> Flux<T> getFluxForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<List<T>>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API GET Flux] Request - Path: {}", metadata.path());
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(timeout)
                .doOnSuccess(response -> log.debug("[API GET Flux] Response received - Path: {}, Data size: {}", 
                    metadata.path(), response != null && response.hasData() ? response.data().size() : 0))
                .flatMapMany(this::unwrapListToFluxWithDetails);
    }
    
    /**
     * GET request - Direct Flux response (without ApiResponse wrapper)
     */
    public <T> Flux<T> getFluxDirect(
            ApiEndpointMapper.DomainMetadata<T> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getFluxDirect(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Direct Flux response (without ApiResponse wrapper, custom timeout)
     */
    public <T> Flux<T> getFluxDirect(
            ApiEndpointMapper.DomainMetadata<T> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        String uri = buildUriString(metadata.path(), pathVars, queryParams);
        
        return webClient.method(metadata.method())
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientErrorResponse)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerErrorResponse)
                .bodyToFlux(metadata.typeRef())
                .timeout(timeout)
                .doOnSubscribe(sub -> log.debug("[API GET Flux Direct] Subscription started - URI: {}", uri))
                .doOnNext(item -> log.debug("[API GET Flux Direct] Item received - Path: {}", metadata.path()))
                .doOnComplete(() -> log.debug("[API GET Flux Direct] Completed - Path: {}", metadata.path()))
                .doOnError(error -> log.error("[API GET Flux Direct Error] Path: {}, URI: {}", 
                    metadata.path(), uri, error))
                .retryWhen(getRetrySpec());
    }
    
    /**
     * GET request - Converts ApiResponse<List<T>> to Flux<ApiResponse<T>>
     */
    public <T> Flux<ApiResponse<T>> getFluxForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<List<T>>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return getFluxForResponse(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * GET request - Converts ApiResponse<List<T>> to Flux<ApiResponse<T>> (custom timeout)
     */
    public <T> Flux<ApiResponse<T>> getFluxForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<List<T>>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(timeout)
                .flatMapMany(response -> {
                    if (response.isSuccess() && response.hasData()) {
                        log.debug("[API GET Flux Response] Success - Path: {}, Item count: {}", 
                            metadata.path(), response.data().size());
                        return Flux.fromIterable(response.data())
                                .map(item -> ApiResponse.success(item, response.message()));
                    } else {
                        log.warn("[API GET Flux Response] Failure - Path: {}, Code: {}, Message: {}", 
                            metadata.path(), response.code(), response.message());
                        return Flux.just(ApiResponse.<T>error(response.code(), response.message()));
                    }
                });
    }

    // =========================
    // POST Request
    // =========================
    
    /**
     * POST request - Automatically unwraps ApiResponse
     */
    public <T, R> Mono<R> postForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return postForData(metadata, requestBody, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * POST request - Automatically unwraps ApiResponse (custom timeout)
     */
    public <T, R> Mono<R> postForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API POST] Request - Path: {}, Body: {}", metadata.path(), requestBody);
        
        return requestMono(metadata, requestBody, pathVars, queryParams)
                .timeout(timeout)
                .doOnSuccess(response -> log.debug("[API POST] Response received - Path: {}, Success: {}", 
                    metadata.path(), response != null && response.isSuccess()))
                .doOnError(error -> log.error("[API POST] Request failed - Path: {}, Error: {}", 
                    metadata.path(), error.getMessage(), error))
                .flatMap(this::unwrapResponseWithDetails);
    }
    
    /**
     * POST request - Returns ApiResponse as is
     */
    public <T, R> Mono<ApiResponse<R>> postForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return postForResponse(metadata, requestBody, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * POST request - Returns ApiResponse as is (custom timeout)
     */
    public <T, R> Mono<ApiResponse<R>> postForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        return requestMono(metadata, requestBody, pathVars, queryParams)
                .timeout(timeout)
                .doOnSuccess(response -> logApiResponse(metadata.path(), response))
                .doOnError(error -> logApiError(metadata.path(), error))
                .retryWhen(getRetrySpec());
    }

    // =========================
    // PUT Request (Added)
    // =========================
    
    /**
     * PUT request - Automatically unwraps ApiResponse
     */
    public <T, R> Mono<R> putForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return putForData(metadata, requestBody, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * PUT request - Automatically unwraps ApiResponse (custom timeout)
     */
    public <T, R> Mono<R> putForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API PUT] Request - Path: {}", metadata.path());
        
        return requestMono(metadata, requestBody, pathVars, queryParams)
                .timeout(timeout)
                .flatMap(this::unwrapResponseWithDetails);
    }
    
    /**
     * PUT request - Returns ApiResponse as is
     */
    public <T, R> Mono<ApiResponse<R>> putForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return requestMono(metadata, requestBody, pathVars, queryParams)
                .timeout(DEFAULT_TIMEOUT)
                .doOnSuccess(response -> logApiResponse(metadata.path(), response))
                .doOnError(error -> logApiError(metadata.path(), error))
                .retryWhen(getRetrySpec());
    }

    // =========================
    // DELETE Request (Added)
    // =========================
    
    /**
     * DELETE request - Automatically unwraps ApiResponse
     */
    public <R> Mono<R> deleteForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return deleteForData(metadata, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * DELETE request - Automatically unwraps ApiResponse (custom timeout)
     */
    public <R> Mono<R> deleteForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API DELETE] Request - Path: {}", metadata.path());
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(timeout)
                .flatMap(this::unwrapResponseWithDetails);
    }
    
    /**
     * DELETE request - Returns ApiResponse as is
     */
    public <R> Mono<ApiResponse<R>> deleteForResponse(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return requestMono(metadata, pathVars, queryParams)
                .timeout(DEFAULT_TIMEOUT)
                .doOnSuccess(response -> logApiResponse(metadata.path(), response))
                .doOnError(error -> logApiError(metadata.path(), error))
                .retryWhen(getRetrySpec());
    }

    // =========================
    // PATCH Request (Added)
    // =========================
    
    /**
     * PATCH request - Automatically unwraps ApiResponse
     */
    public <T, R> Mono<R> patchForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        return patchForData(metadata, requestBody, pathVars, queryParams, DEFAULT_TIMEOUT);
    }
    
    /**
     * PATCH request - Automatically unwraps ApiResponse (custom timeout)
     */
    public <T, R> Mono<R> patchForData(
            ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams,
            Duration timeout) {
        
        log.debug("[API PATCH] Request - Path: {}", metadata.path());
        
        return requestMono(metadata, requestBody, pathVars, queryParams)
                .timeout(timeout)
                .flatMap(this::unwrapResponseWithDetails);
    }

    // =========================
    // Core Request Methods (Internal Use)
    // =========================
    
    private <T> Mono<T> requestMono(
            ApiEndpointMapper.DomainMetadata<T> metadata,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        String uri = buildUriString(metadata.path(), pathVars, queryParams);
        
        return webClient.method(metadata.method())
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientErrorResponse)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerErrorResponse)
                .bodyToMono(metadata.typeRef())
                .doOnSubscribe(sub -> log.debug("[API Request] Subscribed - URI: {}, Method: {}", uri, metadata.method()))
                .doOnError(error -> log.error("[API Request] Failed - URI: {}, Method: {}, Error: {}", 
                    uri, metadata.method(), error.getMessage(), error));
    }
    
    private <T, R> Mono<R> requestMono(
            ApiEndpointMapper.DomainMetadata<R> metadata,
            T requestBody,
            Map<String, Object> pathVars,
            Map<String, Object> queryParams) {
        
        String uri = buildUriString(metadata.path(), pathVars, queryParams);
        
        return webClient.method(metadata.method())
                .uri(uriBuilder -> buildUri(uriBuilder, metadata.path(), pathVars, queryParams))
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, this::handleClientErrorResponse)
                .onStatus(HttpStatusCode::is5xxServerError, this::handleServerErrorResponse)
                .bodyToMono(metadata.typeRef())
                .doOnSubscribe(sub -> log.debug("[API Request Body] Subscribed - URI: {}, Method: {}", uri, metadata.method()))
                .doOnError(error -> log.error("[API Request Body] Failed - URI: {}, Method: {}, Error: {}", 
                    uri, metadata.method(), error.getMessage(), error));
    }

    // =========================
    // Status Handler Methods (Fixed signatures)
    // =========================
    
    private Mono<? extends Throwable> handleClientErrorResponse(ClientResponse response) {
        HttpStatusCode statusCode = response.statusCode();
        log.warn("[Client Error] Status: {}", statusCode);
        
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(errorBody -> {
                    log.warn("[Client Error] Response body: {}", errorBody);
                    
                    // Try to extract error code from response body
                    String errorCode = extractErrorCodeFromBody(errorBody);
                    String suggestion = getSuggestionForHttpStatus(statusCode);
                    
                    return Mono.error(new ApiResponseException(
                        errorCode != null ? errorCode : ApiResponse.ERROR_CODE_DEFAULT,
                        "Client error: " + statusCode,
                        Map.of(
                            "statusCode", statusCode.value(),
                            "responseBody", errorBody,
                            "suggestion", suggestion
                        ),
                        suggestion
                    ));
                });
    }
    
    private Mono<? extends Throwable> handleServerErrorResponse(ClientResponse response) {
        HttpStatusCode statusCode = response.statusCode();
        log.error("[Server Error] Status: {}", statusCode);
        
        return response.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(errorBody -> {
                    log.error("[Server Error] Response body: {}", errorBody);
                    
                    return Mono.error(new ApiResponseException(
                        ApiResponse.ERROR_CODE_SERVER_ERROR,
                        "Server error occurred: " + statusCode,
                        Map.of(
                            "statusCode", statusCode.value(),
                            "responseBody", errorBody,
                            "suggestion", "Please try again later. If the problem persists, contact administrator."
                        ),
                        "Please try again later"
                    ));
                });
    }
    
    private String extractErrorCodeFromBody(String responseBody) {
        if (responseBody != null && responseBody.contains("\"code\"")) {
            // Simple JSON parsing - can use regex or more sophisticated parsing
            try {
                int codeIndex = responseBody.indexOf("\"code\"");
                if (codeIndex != -1) {
                    int colonIndex = responseBody.indexOf(":", codeIndex);
                    if (colonIndex != -1) {
                        int startQuote = responseBody.indexOf("\"", colonIndex);
                        int endQuote = responseBody.indexOf("\"", startQuote + 1);
                        if (startQuote != -1 && endQuote != -1) {
                            return responseBody.substring(startQuote + 1, endQuote);
                        }
                    }
                }
            } catch (Exception ex) {
                log.debug("Error extracting error code: {}", ex.getMessage());
            }
        }
        return null;
    }
    
    private String getSuggestionForHttpStatus(HttpStatusCode statusCode) {
        if (statusCode.is4xxClientError()) {
            if (statusCode.value() == 400) return "Please check your input values.";
            if (statusCode.value() == 401) return "Authentication required. Please log in again.";
            if (statusCode.value() == 403) return "You don't have permission to access this resource.";
            if (statusCode.value() == 404) return "Requested resource was not found.";
            if (statusCode.value() == 409) return "Data conflict occurred. Please refresh and try again.";
            if (statusCode.value() == 422) return "Invalid input data. Please check your input.";
            return "Invalid request. Please check your input values.";
        }
        return null;
    }

    // =========================
    // Response Unwrapping Helper Methods (with details)
    // =========================
    
    private <T> Mono<T> unwrapResponseWithDetails(ApiResponse<T> response) {
        if (response == null) {
            return Mono.error(new ApiResponseException(
                ApiResponse.ERROR_CODE_SERVER_ERROR,
                "Received empty response from server",
                null,
                null
            ));
        }
        
        if (response.isSuccess() && response.hasData()) {
            log.debug("[API Unwrap] Success - Data: {}", response.data());
            return Mono.just(response.data());
        } else if (response.isSuccess() && !response.hasData()) {
            // Success but no data (e.g., DELETE request)
            log.debug("[API Unwrap] Success (no data) - Message: {}", response.message());
            return Mono.empty();
        } else {
            // Failure case - includes details
            log.warn("[API Unwrap] Failure - Code: {}, Message: {}, Details: {}", 
                response.code(), response.message(), response.details());
            
            return Mono.error(new ApiResponseException(
                response.code(),
                response.message(),
                response.details(),
                createSuggestionFromErrorCode(response.code())
            ));
        }
    }
    
    private <T> Flux<T> unwrapListToFluxWithDetails(ApiResponse<List<T>> response) {
        if (response == null) {
            return Flux.error(new ApiResponseException(
                ApiResponse.ERROR_CODE_SERVER_ERROR,
                "Received empty response from server",
                null,
                null
            ));
        }
        
        if (response.isSuccess() && response.hasData()) {
            List<T> data = response.data();
            log.debug("[API Unwrap List] Success - Data size: {}", data.size());
            if (data.isEmpty()) {
                log.debug("[API Unwrap List] Returning empty list");
            }
            return Flux.fromIterable(data);
        } else {
            log.warn("[API Unwrap List] Failure - Code: {}, Message: {}", 
                response.code(), response.message());
            
            return Flux.error(new ApiResponseException(
                response.code(),
                response.message(),
                response.details(),
                createSuggestionFromErrorCode(response.code())
            ));
        }
    }
    
    private String createSuggestionFromErrorCode(String errorCode) {
        if (errorCode == null) return null;
        
        return switch (errorCode) {
            case ApiResponse.ERROR_CODE_NOT_FOUND -> "The requested data does not exist. Please check the ID or path.";
            case ApiResponse.ERROR_CODE_INVALID_INPUT -> "Invalid input. Please check your input values.";
            case ApiResponse.ERROR_CODE_UNAUTHORIZED -> "Authentication required. Please log in again.";
            case ApiResponse.ERROR_CODE_FORBIDDEN -> "Access denied. Please contact your administrator if you need access.";
            case ApiResponse.ERROR_CODE_SERVER_ERROR -> "Server processing error occurred. Please try again later.";
            case ApiResponse.ERROR_CODE_NETWORK_ERROR -> "Please check your network connection. Verify your internet connectivity.";
            case ApiResponse.ERROR_CODE_TIMEOUT -> "Request timeout. Please check your network status or try again later.";
            default -> "An error occurred. Please try again.";
        };
    }
    
    // =========================
    // Logging Helper Methods
    // =========================
    
    private void logApiResponse(String path, ApiResponse<?> response) {
        if (response == null) {
            log.warn("[API Response] Path: {} - Response is null", path);
            return;
        }
        
        if (response.isSuccess()) {
            if (response.hasData()) {
                log.debug("[API Response] Success - Path: {}, Code: {}, Message: {}", 
                    path, response.code(), response.message());
            } else {
                log.debug("[API Response] Success (no data) - Path: {}, Code: {}, Message: {}", 
                    path, response.code(), response.message());
            }
        } else {
            log.warn("[API Response] Failure - Path: {}, Code: {}, Message: {}, Details: {}", 
                path, response.code(), response.message(), response.details());
        }
    }
    
    private void logApiError(String path, Throwable error) {
        switch (error) {
            // ApiResponseException 케이스
            case ApiResponseException are -> 
                log.error("[API Error] Path: {}, Code: {}, Message: {}, Suggestion: {}", 
                    path, are.getCode(), are.getMessage(), are.getSuggestion());

            // WebClientResponseException 케이스
            case WebClientResponseException wce -> 
                log.error("[API HTTP Error] Path: {}, Status: {}, Body: {}", 
                    path, wce.getStatusCode(), wce.getResponseBodyAsString());

            // 그 외 모든 예외 (default)
            default -> 
                log.error("[API Error] Path: {}, Error: {}", path, error.getMessage(), error);
        }
    }
    
    // =========================
    // Retry Configuration
    // =========================
    
    private Retry getRetrySpec() {
        return Retry.backoff(MAX_RETRY_ATTEMPTS, INITIAL_RETRY_DELAY)
                .maxBackoff(Duration.ofSeconds(10))
                .filter(throwable -> {
                    // Check if exception is retryable
                    if (throwable instanceof WebClientResponseException wcre) {
                        int statusCode = wcre.getStatusCode().value();
                        // Don't retry 4xx errors (client errors)
                        // Retry 5xx errors (server errors)
                        return statusCode >= 500;
                    }
                    // Retry network errors
                    return throwable instanceof java.net.SocketException ||
                           throwable instanceof java.net.ConnectException ||
                           throwable instanceof java.net.SocketTimeoutException;
                })
                .doBeforeRetry(rs -> log.warn("[API Retry] Retry {}/{} - Failure cause: {}", 
                    rs.totalRetries() + 1, MAX_RETRY_ATTEMPTS, rs.failure().getMessage()));
    }
    
    // =========================
    // Utility Methods
    // =========================
    
    private URI buildUri(UriBuilder uriBuilder, String path, 
                         Map<String, Object> pathVars, 
                         Map<String, Object> queryParams) {
        uriBuilder.path(path);
        
        if (queryParams != null && !queryParams.isEmpty()) {
            queryParams.forEach((key, value) -> {
                if (value != null) {
                    if (value instanceof Iterable) {
                        // Handle list type parameters
                        for (Object item : (Iterable<?>) value) {
                            uriBuilder.queryParam(key, item);
                        }
                    } else {
                        uriBuilder.queryParam(key, value);
                    }
                }
            });
        }
        
        return uriBuilder.build(pathVars != null ? pathVars : Map.of());
    }
    
    private String buildUriString(String path, Map<String, Object> pathVars, Map<String, Object> queryParams) {
        // Path variables substitution (for display purposes)
        String resolvedPath = path;
        if (pathVars != null && !pathVars.isEmpty()) {
            for (Map.Entry<String, Object> entry : pathVars.entrySet()) {
                resolvedPath = resolvedPath.replace("{" + entry.getKey() + "}", 
                    entry.getValue() != null ? entry.getValue().toString() : "");
            }
        }
        
        // Query parameters addition (for display purposes)
        if (queryParams != null && !queryParams.isEmpty()) {
            StringBuilder uriBuilder = new StringBuilder(resolvedPath);
            uriBuilder.append("?");
            
            // Use stream with collector for cleaner approach
            String queryString = queryParams.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(java.util.stream.Collectors.joining("&"));
            
            uriBuilder.append(queryString);
            return uriBuilder.toString();
        }
        
        return resolvedPath;
    }
    
    // =========================
    // Convenience Methods
    // =========================
    
    /**
     * GET request - Path variables only
     */
    public <T> Mono<T> getForData(ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata, 
                                   Map<String, Object> pathVars) {
        return getForData(metadata, pathVars, null);
    }
    
    /**
     * GET request - With builder pattern support
     */
    public <T> RequestBuilder<T> get(ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata) {
        return new RequestBuilder<>(this, metadata);
    }
    
    /**
     * POST request builder
     */
    public <T, R> PostRequestBuilder<T, R> post(ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata) {
        return new PostRequestBuilder<>(this, metadata);
    }
    
    // =========================
    // Request Builder Inner Classes
    // =========================
    
    public static class RequestBuilder<T> {
        private final CommonApiClient client;
        private final ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata;
        private Map<String, Object> pathVars;
        private Map<String, Object> queryParams;
        private Duration timeout;
        
        RequestBuilder(CommonApiClient client, ApiEndpointMapper.DomainMetadata<ApiResponse<T>> metadata) {
            this.client = client;
            this.metadata = metadata;
            this.timeout = DEFAULT_TIMEOUT;
        }
        
        public RequestBuilder<T> pathVars(Map<String, Object> pathVars) {
            this.pathVars = pathVars;
            return this;
        }
        
        public RequestBuilder<T> queryParams(Map<String, Object> queryParams) {
            this.queryParams = queryParams;
            return this;
        }
        
        public RequestBuilder<T> timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Mono<T> asData() {
            return client.getForData(metadata, pathVars, queryParams, timeout);
        }
        
        public Mono<ApiResponse<T>> asResponse() {
            return client.getForResponse(metadata, pathVars, queryParams, timeout);
        }
    }
    
    public static class PostRequestBuilder<T, R> {
        private final CommonApiClient client;
        private final ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata;
        private T requestBody;
        private Map<String, Object> pathVars;
        private Map<String, Object> queryParams;
        private Duration timeout;
        
        PostRequestBuilder(CommonApiClient client, ApiEndpointMapper.DomainMetadata<ApiResponse<R>> metadata) {
            this.client = client;
            this.metadata = metadata;
            this.timeout = DEFAULT_TIMEOUT;
        }
        
        public PostRequestBuilder<T, R> body(T requestBody) {
            this.requestBody = requestBody;
            return this;
        }
        
        public PostRequestBuilder<T, R> pathVars(Map<String, Object> pathVars) {
            this.pathVars = pathVars;
            return this;
        }
        
        public PostRequestBuilder<T, R> queryParams(Map<String, Object> queryParams) {
            this.queryParams = queryParams;
            return this;
        }
        
        public PostRequestBuilder<T, R> timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }
        
        public Mono<R> asData() {
            return client.postForData(metadata, requestBody, pathVars, queryParams, timeout);
        }
        
        public Mono<ApiResponse<R>> asResponse() {
            return client.postForResponse(metadata, requestBody, pathVars, queryParams, timeout);
        }
    }
}