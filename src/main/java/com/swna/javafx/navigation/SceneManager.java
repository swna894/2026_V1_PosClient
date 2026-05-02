package com.swna.javafx.navigation;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.swna.javafx.controller.base.support.DataReceiver;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SceneManager {

    private final ApplicationContext context;

    private Stage stage;

    // 🔥 뒤로가기 스택
    private final Deque<ViewState> history = new ArrayDeque<>();

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    // 🔥 기본 이동
    public void switchScene(String fxml) {
        switchScene(fxml, null);
    }

    // 🔥 DTO 포함 이동
    public void switchScene(String fxml, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(context::getBean);

            Parent root = loader.load();

            Object controller = loader.getController();

            // 🔥 데이터 전달
            if (controller instanceof DataReceiver<?> receiver) {

            if (data == null || receiver.getType().isInstance(data)) {
                    deliver(receiver, data);
                } else {
                    throw new IllegalArgumentException("DTO 타입 불일치");
                }
            }

            // 🔥 현재 상태 저장 (뒤로가기)
            history.push(new ViewState(stage.getScene(), data));

            stage.setScene(new Scene(root));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 🔥 뒤로가기
    public void goBack() {
        if (history.isEmpty()) return;

        ViewState prev = history.pop();
        stage.setScene(prev.scene());
    }

   @SuppressWarnings("unchecked")
    private <T> void deliver(DataReceiver<?> receiver, Object data) {
        DataReceiver<T> r = (DataReceiver<T>) receiver;
        r.onReceive((T) data);
    }

    // 상태 객체
    record ViewState(Scene scene, Object data) {}
}