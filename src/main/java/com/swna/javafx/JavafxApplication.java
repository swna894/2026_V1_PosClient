package com.swna.javafx;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import com.swna.javafx.view.product.ProductController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import net.rgielen.fxweaver.core.FxWeaver;

@SpringBootApplication
public class JavafxApplication extends Application {

      private ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        launch(args); // ⭐ JavaFX 실행
    }

    @Override
    public void init() {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        this.applicationContext = new SpringApplicationBuilder()
                .sources(StartApplication.class)
                .run(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FxWeaver fxWeaver = applicationContext.getBean(FxWeaver.class);
        Parent view = fxWeaver.loadView(ProductController.class);

        if (view == null) {
            System.err.println("Can't load main view.");
            return;
        }
        //stage.getIcons().add(IconUtils.getPosIcon());
        stage.setScene(new Scene(view));
        stage.setTitle("JavaFX + Spring Boot template application");
        //stage.initStyle(StageStyle.TRANSPARENT);
        //StageUtil.makeDraggable(stage, view);
        stage.show();
    }

    @Override
    public void stop() {
        this.applicationContext.close();
        Platform.exit();
    }
}
