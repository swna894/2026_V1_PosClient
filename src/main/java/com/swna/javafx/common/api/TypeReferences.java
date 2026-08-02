package com.swna.javafx.common.api;

import com.swna.javafx.common.response.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;

import java.util.List;

public final class TypeReferences {

    private TypeReferences() {} // 인스턴스화 방지

    // 공통 Void 타입 (재사용)
    public static final ParameterizedTypeReference<ApiResponse<Void>> VOID_TYPE =
            new ParameterizedTypeReference<>() {};

    /**
     * ApiResponse<T> 타입 참조 생성
     * 예: TypeReferences.single(Shop.class) -> ParameterizedTypeReference<ApiResponse<Shop>>
     */
    public static <T> ParameterizedTypeReference<ApiResponse<T>> single(Class<T> clazz) {
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(ApiResponse.class, clazz).getType()
        );
    }

    /**
     * ApiResponse<List<T>> 타입 참조 생성
     * 예: TypeReferences.list(SaleDto.class) -> ParameterizedTypeReference<ApiResponse<List<SaleDto>>>
     */
    public static <T> ParameterizedTypeReference<ApiResponse<List<T>>> list(Class<T> clazz) {
        ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, clazz);
        return ParameterizedTypeReference.forType(
                ResolvableType.forClassWithGenerics(ApiResponse.class, listType).getType()
        );
    }
}
