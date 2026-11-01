package com.swna.javafx.common.util;

import java.net.URL;
import java.util.Locale;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.Effect;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 공통 알림/확인 다이얼로그.
 *
 * <ul>
 *   <li>정보 알림(dialogString / dialogSource)은 non-blocking({@code show()})으로 표시하고,
 *       닫힌 뒤 실행할 {@code onClosed} 콜백을 지원합니다. (포커스 복원, 스캔 잠금 해제 등)</li>
 *   <li>확인(Decision) 다이얼로그는 값을 반환해야 하므로 blocking({@code showAndWait()})이며
 *       FX Application Thread에서 호출해야 합니다.</li>
 *   <li>다이얼로그마다 상태를 로컬로 가지므로 동시에 여러 개가 떠도 서로 간섭하지 않습니다.</li>
 *   <li>부모 창 블러는 닫힐 때 "이전 효과"로 되돌립니다.</li>
 *   <li>표시 직후 짧은 시간 동안 키 입력을 무시합니다. (키보드 방식 스캐너의 Enter가 즉시 닫는 문제 방지)</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertDialog {

	private static final String CSS_PATH = "/styles/alert_dialog.css";
	private static final String STYLE_CLASS = "box-content";
	private static final long INPUT_GUARD_MILLIS = 400;
	private static final Effect PARENT_BLUR = new BoxBlur();

	private final MessageSource messageSource;

	// ------------------------------------------------------------------
	// Information
	// ------------------------------------------------------------------

	public void dialogString(String message) {
		showInfo(message, null, null);
	}

	public void dialogString(String message, Stage parent) {
		showInfo(message, parent, null);
	}

	/**
	 * 알림을 표시하고, 닫힌 뒤 {@code onClosed}를 FX 스레드에서 실행합니다.
	 */
	public void dialogString(String message, Stage parent, Runnable onClosed) {
		showInfo(message, parent, onClosed);
	}

	public void dialogSource(String source, Stage parent) {
		dialogSource(source, parent, null);
	}

	public void dialogSource(String source, Stage parent, Runnable onClosed) {
		showInfo(resolveMessage(source), parent, onClosed);
	}

	/**
	 * 경고 아이콘 없이 WARNING 타입으로 표시하고 싶을 때 사용. (CSS에서 타입별 스타일 분기 가능)
	 */
	public void dialogWarning(String message, Stage parent, Runnable onClosed) {
		runOnFx(() -> show(createAlert(AlertType.WARNING, parent, message), parent, onClosed));
	}

	// ------------------------------------------------------------------
	// Decision (blocking) — FX Application Thread에서만 호출
	// ------------------------------------------------------------------

	public Optional<ButtonType> dialogDecisionString(String message) {
		return showDecision(message, null);
	}

	public Optional<ButtonType> dialogDecisionString(String message, Stage parent) {
		return showDecision(message, parent);
	}

	public Optional<ButtonType> dialogDecision(String messageKey) {
		return showDecision(resolveMessage(messageKey), null);
	}

	public Optional<ButtonType> dialogDecision(String messageKey, Stage parent) {
		return showDecision(resolveMessage(messageKey), parent);
	}

	// ------------------------------------------------------------------
	// Internals
	// ------------------------------------------------------------------

	private void showInfo(String message, Stage parent, Runnable onClosed) {
		runOnFx(() -> show(createAlert(AlertType.INFORMATION, parent, message), parent, onClosed));
	}

	/** non-blocking 표시 + 닫힘 콜백 */
	private void show(Alert alert, Stage parent, Runnable onClosed) {
		Runnable restoreParent = applyParentBlur(parent);

		alert.setOnHidden(e -> {
			restoreParent.run();
			if (onClosed != null) {
				Platform.runLater(onClosed);
			}
		});
		alert.show();
	}

	/** blocking 표시 */
	private Optional<ButtonType> showDecision(String message, Stage parent) {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("dialogDecision must be called on the FX Application Thread");
		}
		Alert alert = createAlert(AlertType.CONFIRMATION, parent, message);
		Runnable restoreParent = applyParentBlur(parent);
		try {
			return alert.showAndWait();
		} finally {
			restoreParent.run();
		}
	}

	private Alert createAlert(AlertType type, Stage parent, String message) {
		Alert alert = new Alert(type);
		alert.setHeaderText(null);
		alert.setGraphic(null);

		if (parent != null) {
			alert.initOwner(parent);
		}
		alert.initStyle(StageStyle.TRANSPARENT);

		DialogPane pane = alert.getDialogPane();

		Label label = new Label(message == null ? "" : message);
		label.setWrapText(true);
		pane.setContent(label);

		URL css = getClass().getResource(CSS_PATH);
		if (css != null) {
			pane.getStylesheets().add(css.toExternalForm());
		} else {
			log.warn("Alert stylesheet not found: {}", CSS_PATH);
		}
		pane.getStyleClass().add(STYLE_CLASS);

		if (pane.getScene() != null) {
			pane.getScene().setFill(Color.TRANSPARENT);
			if (parent == null && pane.getScene().getWindow() instanceof Stage stage) {
				stage.setAlwaysOnTop(true);
			}
		}

		installInputGuard(alert);
		return alert;
	}

	/**
	 * 표시 직후 INPUT_GUARD_MILLIS 동안 키 이벤트를 소비합니다.
	 * 키보드 웨지 방식 스캐너의 나머지 문자/Enter가 알림을 즉시 닫아버리는 것을 막습니다.
	 */
	private void installInputGuard(Alert alert) {
		final long[] shownAt = { Long.MAX_VALUE };
		alert.setOnShown(e -> shownAt[0] = System.nanoTime());

		alert.getDialogPane().addEventFilter(KeyEvent.ANY, e -> {
			long elapsedMillis = (System.nanoTime() - shownAt[0]) / 1_000_000;
			if (elapsedMillis < INPUT_GUARD_MILLIS) {
				e.consume();
			}
		});
	}

	/**
	 * 부모 창 루트에 블러를 적용하고, 원래 효과로 되돌리는 Runnable을 반환합니다.
	 */
	private Runnable applyParentBlur(Stage parent) {
		if (parent == null || parent.getScene() == null) {
			return () -> { };
		}
		Node root = parent.getScene().getRoot();
		Effect previous = root.getEffect();
		root.setEffect(PARENT_BLUR);
		return () -> root.setEffect(previous);
	}

	private String resolveMessage(String key) {
		try {
			return messageSource.getMessage(key, null, Locale.getDefault());
		} catch (Exception ex) {
			log.warn("Message key not found: {}", key);
			return key;
		}
	}

	private static void runOnFx(Runnable action) {
		if (Platform.isFxApplicationThread()) {
			action.run();
		} else {
			Platform.runLater(action);
		}
	}
}
