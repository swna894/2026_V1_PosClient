package com.swna.javafx.common.navigation;

import org.springframework.stereotype.Component;

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
                                navigateStage(PosViewController.class);
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
            navigate(PosViewController.class);
        } else {
            Application.setUserAgentStylesheet( new PrimerLight().getUserAgentStylesheet() );
            navigate(LoginViewController.class);
            
        }
    }

    public <T> void navigateStage(Class<T> controllerClass) {
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
     * 현재 창은 유지하고 별도의 새 창으로 뷰를 엽니다
     */
    public <T> void openInNewWindow(Class<T> controllerClass, String title) {
        try {
            Stage newStage = new Stage();
            newStage.getIcons().add(new Image("/images/pos_system.png"));
            
            Parent root = fxWeaver.loadView(controllerClass);
            Scene scene = new Scene(root);
            
            newStage.setTitle(title);
            newStage.setScene(scene);
            newStage.initStyle(StageStyle.DECORATED);
            newStage.show();
            
        } catch (Exception e) {
            log.error("Failed to open new window for: {}", controllerClass.getSimpleName(), e);
        }
    }

    // 오버로드: 타이틀 자동 설정
    public <T> void openInNewWindow(Class<T> controllerClass) {
        String title = controllerClass.getSimpleName().replace("Controller", "");
        openInNewWindow(controllerClass, title);
    }
}