package com.swna.javafx.common.navigation;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.MenuController;
import com.swna.javafx.common.store.AuthState;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.login.LoginViewController;
import com.swna.javafx.pos.PosViewController;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxWeaver;

@Slf4j
@Component
public class NavigationService {

    private final FxWeaver fxWeaver;
    private final AuthStore authStore;

    private Stage stage;
    private Stage adminWindowStage; // 💡 서브 창(MenuController -> UnPackingController 등) 관리를 위한 스테이지 변수 추가

    // =========================
    // 창 이동 좌표
    // =========================
    private double xOffset = 0;
    private double yOffset = 0;

    public NavigationService(  FxWeaver fxWeaver, AuthStore authStore ) {

        this.fxWeaver = fxWeaver;
        this.authStore = authStore;
    }

    public void setStage(Stage stage) {

        this.stage = stage;

        // =========================
        // UNDECORATED
        // 반드시 show 전에 1회만
        // =========================
        this.stage.initStyle(StageStyle.UNDECORATED);

        stage.getIcons().add( new Image("/images/24_server.png") );

        // =========================
        // 로그인 상태 감지
        // =========================
        authStore.authStateProperty().addListener(
                (obs, oldVal, newVal) ->
                        Platform.runLater(() -> {
                            if (newVal == AuthState.AUTHENTICATED) {
                                // 💡 login 선택된 모드에 따라 실행할 Controller 분기
                                if (authStore.getSelectedMode() == AuthStore.AppMode.POS) {
                                    openWindow(PosViewController.class);
                                } else {
                                    openWindow(MenuController.class); // ADMIN 선택 시 MenuController 실행
                                }
                            } else {
                                navigate(LoginViewController.class);
                            }
                        })
        );

        // =========================
        // 현재 상태 즉시 반영
        // =========================
        AuthState current = authStore.getAuthState();

        if (current == AuthState.AUTHENTICATED) {
            if (authStore.getSelectedMode() == AuthStore.AppMode.POS) {
                openWindow(PosViewController.class);
            } else {
                openWindow(MenuController.class);
            }
        } else {
            Application.setUserAgentStylesheet( new PrimerLight().getUserAgentStylesheet() );
            navigate(LoginViewController.class);
            
        }
    }

    public <T> void openWindow(Class<T> controllerClass) {
        try {
            stage.close();

            Stage newStage = new Stage();
            newStage.getIcons().add( new Image("/images/pos_system.png") );

            Parent root = fxWeaver.loadView(controllerClass);
            
            Scene scene = new Scene(root);
            newStage.initStyle(StageStyle.DECORATED);
            newStage.setScene(scene);
            newStage.setMaximized(true);
            newStage.show();

            this.stage = newStage;
        } catch (Exception e) {
            log.error("POS Stage open error", e);
        }
    }

    // =========================
    // 모달 창 열기 (Title 지정)
    // =========================
    public <T> void openModalWindow(Class<T> controllerClass, String title) {
        try {
            Stage newStage = new Stage();
            
            // 1. 창 타이틀 설정
            newStage.setTitle(title);
            
            // 2. 모달 설정 (앱 전체에 대한 모달)
            newStage.initModality(Modality.APPLICATION_MODAL);
            
            // 3. 부모 Stage 지정 (서브 창이 켜져 있으면 서브 창을, 아니면 메인 stage를 Owner로 지정)
            Stage parentStage = (adminWindowStage != null && adminWindowStage.isShowing()) 
                                ? adminWindowStage 
                                : this.stage;

            if (parentStage != null) {
                newStage.initOwner(parentStage);
            }

            newStage.getIcons().add(new Image("/images/pos_system.png"));

            Parent root = fxWeaver.loadView(controllerClass);
            
            Scene scene = new Scene(root);
            newStage.initStyle(StageStyle.DECORATED);
            newStage.setScene(scene);
        
            // 4. showAndWait() 호출하여 대기
            newStage.showAndWait();

        } catch (Exception e) {
            log.error("POS Modal Stage open error", e);
        }
    }

    // 오버로드: title 생략 시 컨트롤러 클래스명 기반으로 자동 설정
    public <T> void openModalWindow(Class<T> controllerClass) {
        String defaultTitle = controllerClass.getSimpleName().replace("Controller", "");
        openModalWindow(controllerClass, defaultTitle);
    }
    
    // =========================
    // 기본 navigate
    // =========================
    public void navigate(Class<?> viewClass) {

        Parent root = fxWeaver.loadView(viewClass);

        if (root == null) {
            log.error("🔥 Failed to load view: {}", viewClass.getName() );
            return;
        }

        // =========================
        // Scene 생성
        // =========================
        Scene scene = new Scene(root);

        // =========================
        // 창 이동 이벤트
        // =========================
        scene.setOnMousePressed(event -> { xOffset = event.getSceneX();yOffset = event.getSceneY(); });
        scene.setOnMouseDragged(event -> { stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset); });

        stage.setScene(scene);

        // =========================
        // 로그인 화면
        // =========================
        if (viewClass.equals(LoginViewController.class)) {
            stage.setFullScreen(false);
            stage.setMaximized(false);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.centerOnScreen();

        } else {

            // =========================
            // POS 화면
            // =========================
            stage.setFullScreen(true);
            stage.initStyle(StageStyle.DECORATED);
            Object controller =  fxWeaver.getBean(viewClass);

            if (controller instanceof ViewInfo viewInfo) {
                stage.setTitle( viewInfo.getTitle() );
            }
        }

        // =========================
        // 최초 show
        // =========================
        if (!stage.isShowing()) { stage.show(); }
    }

    // =========================
    // 부분 변경
    // =========================
    public void goTo(Class<?> viewClass) {

        Platform.runLater(() -> {

            Parent root = fxWeaver.loadView(viewClass);

            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
            }
        });
    }

    /**
     * 메인 창은 유지하고 '새 창'으로 관리자 메뉴를 엽니다.
     * 이때 생성된 Stage 참조를 adminWindowStage에 저장해둡니다.
     */
    public <T> void openInNewWindow(Class<T> controllerClass, String title) {
        try {
            // 이미 서브 창이 열려있다면 해당 창을 사용, 없으면 생성
            if (adminWindowStage == null || !adminWindowStage.isShowing()) {
                adminWindowStage = new Stage();
                adminWindowStage.getIcons().add(new Image("/images/pos_system.png"));
                adminWindowStage.initStyle(StageStyle.DECORATED);
            }

            Parent root = fxWeaver.loadView(controllerClass);
            Scene scene = new Scene(root);

            adminWindowStage.setTitle(title);
            adminWindowStage.setScene(scene);
            adminWindowStage.setMaximized(true); // 새 창도 최대화

            if (!adminWindowStage.isShowing()) {
                adminWindowStage.show();
            } else {
                adminWindowStage.toFront(); // 이미 떠 있으면 앞으로 가져옴
            }

        } catch (Exception e) {
            log.error("Failed to open new window for: {}", controllerClass.getSimpleName(), e);
        }
    }

    /**
     * 💡 [핵심] '새 창(adminWindowStage)' 안에서 화면만 교체합니다.
     * MenuController -> UnPackingController 로 교체될 때 사용
     */
    public <T> void navigateInNewWindow(Class<T> controllerClass, String title) {
        try {
            // 만약 서브 창이 없다면 새로 띄우고, 있다면 화면만 교체
            if (adminWindowStage == null || !adminWindowStage.isShowing()) {
                openInNewWindow(controllerClass, title);
                return;
            }

            Parent root = fxWeaver.loadView(controllerClass);
            Scene scene = new Scene(root);

            adminWindowStage.setTitle(title);
            adminWindowStage.setScene(scene); // 👈 핵심: 기존 MenuController 창에 UnPackingController Scene 세팅
            adminWindowStage.setMaximized(true); // 새 창도 최대화

            adminWindowStage.toFront();

        } catch (Exception e) {
            log.error("Failed to navigate in new window for: {}", controllerClass.getSimpleName(), e);
        }
    }

    // 오버로드: 타이틀 자동 설정
    public <T> void openInNewWindow(Class<T> controllerClass) {
        String title = controllerClass.getSimpleName().replace("Controller", "");
        openInNewWindow(controllerClass, title);
    }
}