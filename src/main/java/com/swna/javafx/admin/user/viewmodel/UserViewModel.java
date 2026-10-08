package com.swna.javafx.admin.user.viewmodel;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.user.api.UserApiClient;
import com.swna.javafx.admin.user.domain.User;
import com.swna.javafx.admin.user.domain.UserRecordDto;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class UserViewModel {

    private final UserApiClient userApiClient;

    // UI 테이블에 바인딩될 유저 관찰 가능 리스트
    private final ObservableList<User> users = FXCollections.observableArrayList();

    // 상태 및 로딩 관리 프로퍼티
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private final StringProperty statusMessage = new SimpleStringProperty("");

    /**
     * 전체 사용자 목록 로드
     */
    public void loadUsers() {
        // 네트워크 요청 직전 상태 변경은 스레드 안전할 수 있으나 안전하게 묶어줍니다.
        loading.set(true);
        statusMessage.set("Loading user list...");

        userApiClient.getAllUsers()
                .subscribe(response -> {
                    // JavaFX Application Thread에서 UI 상태 및 리스트 갱신
                    Platform.runLater(() -> {
                        loading.set(false);
                        if (response.isSuccess() && response.data() != null) {
                            List<User> userList = response.data().stream()
                                    .map(User::from)
                                    .toList();
                            
                            users.setAll(userList); // JavaFX 스레드에서 안전하게 리스트 갱신
                            statusMessage.set("Successfully loaded user list. (Total: " + userList.size() + ")");
                            dirty.set(false); 
                        } else {
                            statusMessage.set("Failed to load user list");
                            log.error("[ViewModel] Failed to load users");
                        }
                    });
                }, error -> {
                    Platform.runLater(() -> {
                        loading.set(false);
                        statusMessage.set("Error occurred while loading user list");
                        log.error("[ViewModel] Error loading users", error);
                    });
                });
    }

    /**
     * 신규 사용자 등록 요청
     */
    public Mono<Boolean> createUser(UserRecordDto request) {
        log.info("[ViewModel] Creating new user: {}", request.email());

        return userApiClient.createUser(request)
                .map(response -> {
                    boolean success = response.isSuccess() && response.data() != null;
                    
                    // 백그라운드 스레드에서 발생하는 UI 변경을 방지하기 위해 Platform.runLater 사용
                    Platform.runLater(() -> {
                        if (success) {
                            User newUser = User.from(response.data());
                            users.add(newUser);
                            statusMessage.set("User [" + newUser.getName() + "] has been successfully registered.");
                        } else {
                            statusMessage.set("Failed to register user");
                        }
                    });
                    
                    return success;
                })
                .onErrorReturn(false);
    }
    
    /**
     * 특정 사용자 정보 수정 요청 (단건 저장)
     */
    public Mono<Boolean> updateUser(User user) {
        log.info("[ViewModel] Updating user ID: {}", user.getId());

        UserRecordDto request = UserRecordDto.from(user);

        return userApiClient.updateUser(user.getId(), request)
                .map(response -> {
                    boolean success = response.isSuccess();
                    
                    Platform.runLater(() -> {
                        if (success) {
                            statusMessage.set("User [" + user.getName() + "] information has been updated.");
                        } else {
                            statusMessage.set("Failed to update user.");
                        }
                    });
                    
                    return success;
                }) 
                .onErrorReturn(false);
    }

    /**
     * 선택된 사용자 삭제
     */
    public Mono<Boolean> deleteUser(Long id) {
        log.info("[ViewModel] Deleting user ID: {}", id);

        return userApiClient.deleteUser(id)
                .map(response -> {
                    boolean success = response.isSuccess();
                    
                    Platform.runLater(() -> {
                        if (success) {
                            users.removeIf(u -> u.getId().equals(id));
                            statusMessage.set("User has been deleted.");
                        } else {
                            statusMessage.set("Failed to delete user.");
                        }
                    });
                    
                    return success;
                })
                .onErrorReturn(false);
    }

    /**
     * 테이블 내 편집 발생 시 호출하여 뷰모델을 Dirty(수정됨) 상태로 마킹
     */
    public void markAsDirty(User item) {
        item.setSelected(true);
        dirty.set(true);
        statusMessage.set("There are unsaved changes. Please click Save.");
    }

    /**
     * 상태 초기화
     */
    public void clearStatus() {
        statusMessage.set("");
    }
}