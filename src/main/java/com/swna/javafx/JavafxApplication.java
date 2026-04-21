package com.swna.javafx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

@SpringBootApplication
public class JavafxApplication extends Application {

    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        launch(args); // ⭐ JavaFX 실행
    }

    @Override
    public void init() {
        context = SpringApplication.run(JavafxApplication.class);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/product/ProductView.fxml")
        );

        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();

        stage.setScene(new Scene(root));
        stage.setTitle("Product");
        stage.show();
    }

    @Override
    public void stop() {
        context.close();
    }
}
