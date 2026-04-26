package com.swna.javafx.ui;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.store.AuthState;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.view_ui.login.LoginViewController;
import com.swna.javafx.view_ui.pos.PosViewController;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.rgielen.fxweaver.core.FxWeaver;

@Component
public class NavigationService {

    private static final Logger log = LoggerFactory.getLogger(NavigationService.class);

    private final FxWeaver fxWeaver;
    private final AuthStore authStore;   

    private Stage stage;

    public NavigationService(FxWeaver fxWeaver,  AuthStore authStore) {
        this.fxWeaver = fxWeaver;
        this.authStore = authStore;
    }

    public void setStage(Stage stage) {
        this.stage = stage;

        stage.getIcons().add(new Image("/images/24_server.png"));

        // 🔥 핵심: 로그인 상태 감지
        authStore.authStateProperty().addListener((obs, oldVal, newVal) -> 
        Platform.runLater(() -> {
                if (newVal == AuthState.AUTHENTICATED) {
                    navigate(PosViewController.class);
                } else {
                    navigate(LoginViewController.class);
                }
            })
        );

            // 🔥 핵심: 현재 상태 즉시 반영
        AuthState current = authStore.getAuthState();
        if (current == AuthState.AUTHENTICATED) {
            navigate(PosViewController.class);
        } else {
            Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
            navigate(LoginViewController.class);
        }
    }

    // =========================
    // 기본 navigate
    // =========================
    public void navigate(Class<?> viewClass) {

        Parent root = fxWeaver.loadView(viewClass);

        if (root == null) {
            log.error("🔥 Failed to load view: {}", viewClass.getName());
            return;
        }

        stage.setScene(new Scene(root));


        if (viewClass.equals(LoginViewController.class)) {
           stage.initStyle(StageStyle.UNDECORATED); 
        } else {
            Object controller = fxWeaver.getBean(viewClass);
            if (controller instanceof ViewInfo viewInfo) {
                stage.setTitle(viewInfo.getTitle());
            }
        }
        stage.show();
    }

    // =========================
    // 부분 변경 (옵션)
    // =========================
    public void goTo(Class<?> viewClass) {
        Platform.runLater(() -> {
            Parent root = fxWeaver.loadView(viewClass);
            stage.getScene().setRoot(root);
        });
    }
}
