package com.swna.javafx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.swna.javafx.common.store.AuthState;
import com.swna.javafx.common.store.AuthStore;
import com.swna.javafx.common.store.TokenStore;
import com.swna.javafx.ui.NavigationService;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/*
    AuthStore 도입 후, 로그인 상태에 따라 초기 화면을 결정하는 구조로 변경
     - AuthStore: 로그인 상태 관리
     - NavigationService: 로그인 상태 감지 → 화면 전환
     - JavafxApplication: 초기 로그인 상태 설정

    - APP START -> start() ->  AuthStore 상태 설정 -> AuthStore 감지 -> NavigationService 감지 -> 화면 전환
*/
@SpringBootApplication
public class JavafxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavafxApplication.class);

    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        this.applicationContext = new SpringApplicationBuilder()
                .sources(StartApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage stage) {

        try {
            // 🌙 Dark mode 적용
            Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());

            // 🔥 Bean 가져오기
            NavigationService navigationService = applicationContext.getBean(NavigationService.class);
            AuthStore authStore = applicationContext.getBean(AuthStore.class);
            TokenStore tokenStore = applicationContext.getBean(TokenStore.class);

            // 🔥 Stage 주입
            navigationService.setStage(stage);

            // =========================
            // 🔥 초기 상태 결정 (핵심)
            // =========================
            if (tokenStore.hasToken()) {
                authStore.setAuthState(AuthState.AUTHENTICATED);
            } else {
                authStore.setAuthState(AuthState.UNAUTHENTICATED);
            }

            // 👉 화면 전환은 NavigationService가 자동 처리

        } catch (Exception e) {
            log.error("🔥 Error during application start", e);
        }
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }
}