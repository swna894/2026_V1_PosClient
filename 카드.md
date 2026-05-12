# WebFlux 기반 API 클라이언트 구조
    ProductIntegratedService (비즈니스 계층)
         ↓
    CommonApiClient (HTTP 통신 계층)
         ↓
    ApiEndpointMapper (메타데이터 관리 계층)
         ↓
    WebClient (리액티브 HTTP 클라이언트)

# 리액티브 이점 적용

    특성	            적용 방식
    논블로킹 I/O	WebClient가 Netty 기반으로 non-blocking HTTP 요청
    백프레셔	    Flux/Subscriber가 데이터 소비 속도 조절
    조합성	        Mono.zip(), Mono.flatMap() 등으로 여러 API 호출 병렬/순차 처리
    에러 처리	    doOnError(), onErrorResume() 등 풍부한 에러 핸들링

# 흐름 예시: 바코드 조회

    1. ProductIntegratedService.findByBarcode("88012345")
    ↓
    2. mapper.getMetadata("barcode_search")
        → DomainMetadata(path="/products/barcode/{barcode}", typeRef=ApiResponse<ProductResponseDto>)
    ↓
    3. apiClient.requestMono(metadata, {barcode:"88012345"}, null)
    ↓
    4. WebClient GET /products/barcode/88012345
    ↓
    5. 응답 바디 → Mono<ApiResponse<ProductResponseDto>> 변환
    ↓
    6. 서비스 계층으로 전달