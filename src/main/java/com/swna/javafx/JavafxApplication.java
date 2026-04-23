package com.swna.javafx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.swna.javafx.view.product.ProductController;

import atlantafx.base.theme.PrimerDark;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

@SpringBootApplication
public class JavafxApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(JavafxApplication.class);

    private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        log.info("🚀 Launching JavaFX Application");
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

        log.info("🎬 Starting JavaFX Stage");

        try {
            // 🌙 Dark mode 적용
            Application.setUserAgentStylesheet( new PrimerDark().getUserAgentStylesheet() );

            FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);

            Parent view = fxWeaver.loadView(ProductController.class);

            if (view == null) {
                log.error("❌ Failed to load ProductController view");
                return;
            }

            Scene scene = new Scene(view);

            stage.setScene(scene);
            stage.setTitle("JavaFX + Spring Boot template application");
            stage.show();

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