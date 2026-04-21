package com.swna.javafx.common.ui.dialog;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import org.springframework.stereotype.Component;

@Component
public class DialogManager {

    // =========================
    // 1. INFO
    // =========================
    public void info(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================
    // 2. ERROR
    // =========================
    public void error(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("오류 발생");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // =========================
    // 3. CONFIRM
    // =========================
    public boolean confirm(String message) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm");
        alert.setHeaderText("확인");
        alert.setContentText(message);

        ButtonType result = alert.showAndWait()
                .orElse(ButtonType.CANCEL);

        return result == ButtonType.OK;
    }

    public void warning(String message) {
      Alert alert = new Alert(Alert.AlertType.WARNING);
      alert.setTitle("Warning");
      alert.setHeaderText("경고");
      alert.setContentText(message);
      alert.showAndWait();
   }

    // =========================
    // 4. INPUT DIALOG
    // =========================
    public String input(String title, String message) {

        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(message);

        return dialog.showAndWait().orElse(null);
    }

    public <T> T showCustom(String fxml) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
         Parent root = loader.load();

         Stage stage = new Stage();
         stage.setScene(new Scene(root));
         stage.showAndWait();

         return loader.getController();

      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }
}
