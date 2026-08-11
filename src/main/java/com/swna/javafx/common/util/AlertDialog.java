package com.swna.javafx.common.util;

import java.util.Locale;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Component
public class AlertDialog {

	MessageSource messageSource;

	private Stage stage;
	private static final Effect parentEffect = new BoxBlur();
	
	private void initParentEffects(final Stage parent) {
		stage.showingProperty().addListener(obs -> 
			parent.getScene().getRoot().setEffect(stage.isShowing() ? parentEffect : null)
		);
	}
	
	public void dialogString(String message) {
		Platform.runLater(() -> {
			Alert alert = createAlert(AlertType.INFORMATION);
			Label label = new Label(message);
			label.setWrapText(true);
			alert.getDialogPane().setContent(label);
			alert.showAndWait();
		});
	}

	public void dialogString(String message, Stage parent) {
		Platform.runLater(() -> {		
			Alert alert = createAlert(AlertType.INFORMATION, parent);
			Label label = new Label(message);
			label.setWrapText(true);
			alert.getDialogPane().setContent(label);
			alert.showAndWait();

		});
	}
	
	public void dialogSource(String source, Stage parent) {
	
		Platform.runLater(() -> {
			String message = messageSource.getMessage(source, null, Locale.US);		
			Alert alert = createAlert(AlertType.INFORMATION, parent);
			Label label = new Label(message);
			label.setWrapText(true);
			alert.getDialogPane().setContent(label);
			alert.showAndWait();
		});
	}

	public Optional<ButtonType> dialogDecisionString(String message) {
		Alert alert = createAlert(AlertType.CONFIRMATION);
		Label label = new Label(message);
		label.setWrapText(true);
		alert.getDialogPane().setContent(label);
			
		return alert.showAndWait();
	}
	
	public Optional<ButtonType> dialogDecisionString(String message, Stage parent) {
		Alert alert = createAlert(AlertType.CONFIRMATION, parent);
		Label label = new Label(message);
		label.setWrapText(true);
		alert.getDialogPane().setContent(label);
	
		return alert.showAndWait();
	}

	public Optional<ButtonType> dialogDecision(String error) {
		String message = messageSource.getMessage(error, null, Locale.KOREA);
		Alert alert = createAlert(AlertType.CONFIRMATION);
		alert.setContentText(message);

		return alert.showAndWait();
	}

	private Alert createAlert(AlertType type , Stage parent) {
		Alert alert = createAlert(type);
		initParentEffects(parent);
		return alert;
	}
	
	private Alert createAlert(AlertType type ) {
		Alert alert = new Alert(type);
		DialogPane dialogPane = alert.getDialogPane();
		stage = (Stage) dialogPane.getScene().getWindow();
		stage.setAlwaysOnTop(true);
		alert.setHeaderText(null);
		alert.setGraphic(null);
		stage.initStyle(StageStyle.TRANSPARENT);
		dialogPane.getStylesheets().add(getClass().getResource("/styles/alert_dialog.css").toExternalForm());
		dialogPane.getStyleClass().add("box-content");
		return alert;
	}
}
