package com.swna.javafx.admin.user.api;

import java.util.List;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.user.domain.UserRecordDto;
import com.swna.javafx.common.api.SimpleApiClient;
import com.swna.javafx.common.api.TypeReferences;
import com.swna.javafx.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserApiClient {

    private final SimpleApiClient webClientCommon;

    private static final String API_USERS = "/users"; // 서버의 유저 API 경로 (테이블명 users와 매칭)

    // ==========================================
    // Create
    // ==========================================

    /**
     * 사용자 신규 등록
     */
    public Mono<ApiResponse<UserRecordDto>> createUser(UserRecordDto request) {
        log.info("[API] POST {} - body: {}", API_USERS, request);
        return webClientCommon.post(API_USERS, request, TypeReferences.single(UserRecordDto.class));
    }

    // ==========================================
    // Read
    // ==========================================

    /**
     * 사용자 단건 조회 (ID)
     */
    public Mono<ApiResponse<UserRecordDto>> getUserById(Long id) {
        String url = API_USERS + "/" + id;
        log.info("[API] GET {} ", url);
        return webClientCommon.get(url, TypeReferences.single(UserRecordDto.class));
    }

    /**
     * 사용자 단건 조회 (이메일)
     */
    public Mono<ApiResponse<UserRecordDto>> getUserByEmail(String email) {
        String url = API_USERS + "/email/" + email;
        log.info("[API] GET {} ", url);
        return webClientCommon.get(url, TypeReferences.single(UserRecordDto.class));
    }

    /**
     * 전체 사용자 목록 조회
     */
    public Mono<ApiResponse<List<UserRecordDto>>> getAllUsers() {
        log.info("[API] GET {}", API_USERS);
        return webClientCommon.get(API_USERS, TypeReferences.list(UserRecordDto.class));
    }

    /**
     * 권한(Role)별 사용자 목록 조회
     */
    public Mono<ApiResponse<List<UserRecordDto>>> getUsersByRole(String role) {
        String url = API_USERS + "/role/" + role;
        log.info("[API] GET {}", url);
        return webClientCommon.get(url, TypeReferences.list(UserRecordDto.class));
    }

    // ==========================================
    // Update
    // ==========================================

    /**
     * 사용자 정보 수정 (주소, 연락처 등)
     */
    public Mono<ApiResponse<UserRecordDto>> updateUser(Long id, UserRecordDto request) {
        String url = API_USERS + "/" + id;
        log.info("[API] PUT {} - body: {}", url, request);
        return webClientCommon.put(url, request, TypeReferences.single(UserRecordDto.class));
    }

    /**
     * 사용자 일괄 수정
     */
    public Mono<ApiResponse<List<UserRecordDto>>> updateUsersBulk(List<UserRecordDto> requests) {
        String url = API_USERS + "/bulk";
        log.info("[API] PUT {} - {} items", url, requests.size());
        return webClientCommon.put(url, requests, TypeReferences.list(UserRecordDto.class));
    }

    // ==========================================
    // Delete
    // ==========================================

    /**
     * 사용자 삭제
     */
    public Mono<ApiResponse<Void>> deleteUser(Long id) {
        String url = API_USERS + "/" + id;
        log.info("[API] DELETE {}", url);
        return webClientCommon.delete(url, TypeReferences.VOID_TYPE);
    }

    /**
     * 사용자 일괄 삭제
     */
    public Mono<ApiResponse<Integer>> deleteUsersBulk(List<Long> ids) {
        String url = API_USERS + "/bulk";
        log.info("[API] DELETE {} - {} ids", url, ids.size());
        return webClientCommon.delete(url, ids, TypeReferences.single(Integer.class));
    }
}