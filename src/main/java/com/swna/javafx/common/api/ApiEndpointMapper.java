package com.swna.javafx.common.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.product.PageResult;
import com.swna.javafx.admin.product.Product;
import com.swna.javafx.barcode.dto.BarcodeLabelDto;
import com.swna.javafx.common.response.ApiResponse;
import com.swna.javafx.pos.dto.ProductResponseDto;

@Component
public class ApiEndpointMapper {
    public static final String PRODUCTS = "/products";
    public static final String PRODUCT_LABELS = "/products/labels";
    public static final String PRODUCT_BARCODE = "/products/barcode/{barcode}";

    // 도메인별 메타데이터 레지스트리 관리[cite: 1, 3]
    private static final Map<String, DomainMetadata> REGISTRY = Map.of(
        "product_page", new DomainMetadata(PRODUCTS, new ParameterizedTypeReference<PageResult<Product>>() {}),
        "barcode_search", new DomainMetadata(PRODUCT_BARCODE, new ParameterizedTypeReference<ApiResponse<ProductResponseDto>>() {}),
        "label_list", new DomainMetadata(PRODUCT_LABELS,  new ParameterizedTypeReference<ApiResponse<List<BarcodeLabelDto>>>() {})
    );

    public DomainMetadata getMetadata(String domain) {
        DomainMetadata metadata = REGISTRY.get(domain);
        if (metadata == null) throw new IllegalArgumentException("Unknown domain: " + domain);
        return metadata;
    }

    public Map<String, Object> createPageParams(String keyword, int page, int size) {
        return Map.of("keyword", keyword, "page", page, "size", size); //[cite: 6]
    }

    // 경로와 타입 정보를 담는 레코드[cite: 1, 3]
    public record DomainMetadata(String path, ParameterizedTypeReference<?> typeRef) {}
}