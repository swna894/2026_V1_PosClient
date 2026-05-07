package com.swna.javafx.common.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.product.PageResult;
import com.swna.javafx.admin.product.Product;
import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.response.ProductResponse;
import com.swna.javafx.pos.dto.response.SaleResponse;

@Component
public class ApiEndpointMapper {

  // =========================
    // 엔드포인트 상수
    // =========================
    private static final String API_BASE = "";
    
    // GET 엔드포인트
    public static final String PRODUCTS = API_BASE + "/products";
    public static final String PRODUCT_LABELS = API_BASE + "/products/labels";
    public static final String PRODUCT_BARCODE = API_BASE + "/products/barcode/{barcode}";
    
    // POST 엔드포인트
    public static final String SALE_CREATE = API_BASE + "/sales";

    // =========================
    // 타입 안전한 메타데이터 레지스트리 (제네릭 적용)
    // =========================
    private static final Map<String, DomainMetadata<?>> REGISTRY = Map.of(
        // GET - PageResult<Product> (페이징 응답 - ApiResponse 래퍼 없음)
        "product_page", new DomainMetadata<PageResult<Product>>( PRODUCTS,  HttpMethod.GET,  new ParameterizedTypeReference<PageResult<Product>>() {}
        ),
        
        // GET - ApiResponse<ProductResponse> (단일 상품 조회)
        "barcode_search", new DomainMetadata<ApiResponse<ProductResponse>>(  PRODUCT_BARCODE,  HttpMethod.GET,  new ParameterizedTypeReference<ApiResponse<ProductResponse>>() {}
        ),
        
        // GET - ApiResponse<List<BarcodeLabelDto>> (라벨 목록)
        "label_list", new DomainMetadata<ApiResponse<List<BarcodeLabelDto>>>( PRODUCT_LABELS,  HttpMethod.GET,  new ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>>() {} ),
        
        // POST - ApiResponse<SaleResponse> (판매 저장)
        "sale_create", new DomainMetadata<ApiResponse<SaleResponse>>( SALE_CREATE,  HttpMethod.POST,  new ParameterizedTypeReference<ApiResponse<SaleResponse>>() {} )
    );

   /**
     * 메타데이터 조회 (타입 안전)
     */
    @SuppressWarnings("unchecked")
    public <T> DomainMetadata<T> getMetadata(String domain) {
        DomainMetadata<?> metadata = REGISTRY.get(domain);
        if (metadata == null) {
            throw new IllegalArgumentException("Unknown domain: " + domain);
        }
        return (DomainMetadata<T>) metadata;
    }

    public Map<String, Object> createPageParams(String keyword, int page, int size) {
        return Map.of("keyword", keyword, "page", page, "size", size);
    }

    // =========================
    // 내부 레코드 정의
    // =========================
    
    /**
     * API 메타데이터를 담는 제네릭 레코드
     * @param <T> 응답 타입 (ApiResponse<T> 또는 직접 응답 타입)
     */
    public record DomainMetadata<T>( String path,  HttpMethod method,  ParameterizedTypeReference<T> typeRef) {}
}