package com.swna.javafx.admin.supplier;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * 신규 거래처 입력 모달 다이얼로그 컨트롤러.
 * 다이얼로그를 열 때마다 새 인스턴스가 필요하므로 prototype 스코프로 등록한다.
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@FxmlView("/view/admin/supplier-add-dialog.fxml")
public class SupplierAddDialogController implements Initializable {

    // 서버 SupplierRequestRecord 의 abbr 검증 규칙과 동일하게 맞춘다.
    private static final String ABBR_REGEX = "[A-Z0-9\\p{Punct}]+";
    private static final Pattern ABBR_PATTERN = Pattern.compile("^" + ABBR_REGEX + "$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int MIN_ABBR = 2;   
    private static final int MAX_ABBR = 8;
    private static final int MAX_NAME = 32;
    private static final int MAX_COMPANY = 64;
    private static final int MAX_EMAIL = 64;
    private static final int MAX_PHONE = 20;
    private static final int MAX_ADDRESS = 128;

    private final SupplierViewModel viewModel;

    @FXML private TextField tfAbbr;
    @FXML private TextField tfName;
    @FXML private TextField tfCompany;
    @FXML private TextField tfEmail;
    @FXML private TextField tfPhone;
    @FXML private TextField tfCellphone;
    @FXML private TextField tfAddress;
    @FXML private CheckBox cbActive;

    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;

    private boolean saved;
    private PauseTransition errorClearTimer; // 에러 메시지 자동 삭제 타이머

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // ABBR: 입력 즉시 대문자로 변환하고, 허용되지 않는 문자(공백, 한글 등)와 8자 초과는 입력 자체를 막는다.
        tfAbbr.setTextFormatter(new TextFormatter<String>(change -> {
            change.setText(change.getText().toUpperCase());
            String next = change.getControlNewText();
            return next.length() <= MAX_ABBR && next.matches("(" + ABBR_REGEX + ")?") ? change : null;
        }));

        limitLength(tfName, MAX_NAME);
        limitLength(tfCompany, MAX_COMPANY);
        limitLength(tfEmail, MAX_EMAIL);
        limitLength(tfPhone, MAX_PHONE);
        limitLength(tfCellphone, MAX_PHONE);
        limitLength(tfAddress, MAX_ADDRESS);

        setupEnterNavigation();
        setupErrorTimer();
        setupErrorClearingOnInput();

        javafx.application.Platform.runLater(() -> tfAbbr.requestFocus());
    }

    /**
     * 일정 시간(4초) 후 에러 메시지를 자동으로 숨기는 타이머 설정
     */
    private void setupErrorTimer() {
        errorClearTimer = new PauseTransition(Duration.seconds(4));
        errorClearTimer.setOnFinished(e -> hideError());
    }

    /**
     * 모든 텍스트 필드에 변경 리스너를 달아 입력 시 기존 에러 메시지 초기화
     */
    private void setupErrorClearingOnInput() {
        TextField[] textFields = {tfAbbr, tfName, tfCompany, tfEmail, tfPhone, tfCellphone, tfAddress};
        for (TextField tf : textFields) {
            tf.textProperty().addListener((observable, oldValue, newValue) -> {
                if (lblError.isVisible() && !newValue.equals(oldValue)) {
                    hideError();
                }
            });
        }
    }

    /**
     * Enter 키를 누르면 다음 입력 필드로 포커스를 이동한다.
     */
    private void setupEnterNavigation() {
        moveFocusOnEnter(tfAbbr, tfName);
        moveFocusOnEnter(tfName, tfCompany);
        moveFocusOnEnter(tfCompany, tfEmail);
        moveFocusOnEnter(tfEmail, tfPhone);
        moveFocusOnEnter(tfPhone, tfCellphone);
        moveFocusOnEnter(tfCellphone, tfAddress);
        moveFocusOnEnter(tfAddress, cbActive);
    }

    private void moveFocusOnEnter(TextField current, javafx.scene.Node next) {
        current.setOnAction(e -> next.requestFocus());
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        hideError();

        String abbr = trim(tfAbbr.getText());
        String name = trim(tfName.getText());
        String email = trim(tfEmail.getText());

        if (abbr.isEmpty()) {
            fail("ABBR is required.", tfAbbr);
            return;
        }
        if (abbr.length() < MIN_ABBR) {
            fail("ABBR must be at least " + MIN_ABBR + " characters.", tfAbbr);
            return;
        }
        if (!ABBR_PATTERN.matcher(abbr).matches()) {
            fail("ABBR must contain only uppercase letters, numbers and special characters (no spaces).", tfAbbr);
            return;
        }
        if (name.isEmpty()) {
            fail("NAME is required.", tfName);
            return;
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            fail("E-MAIL format is invalid.", tfEmail);
            return;
        }

        Supplier supplier = new Supplier();
        supplier.setAbbr(abbr);
        supplier.setName(name);
        supplier.setCompany(trim(tfCompany.getText()));
        supplier.setEmail(email);
        supplier.setPhone(trim(tfPhone.getText()));
        supplier.setCellphone(trim(tfCellphone.getText()));
        supplier.setAddress(trim(tfAddress.getText()));
        supplier.setActive(cbActive.isSelected());

        setBusy(true);
        viewModel.addSupplier(
                supplier,
                () -> {
                    saved = true;
                    closeDialog();
                },
                message -> {
                    setBusy(false);
                    showError(message);
                });
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void limitLength(TextInputControl control, int max) {
        control.setTextFormatter(new TextFormatter<String>(
                change -> change.getControlNewText().length() <= max ? change : null));
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void fail(String message, TextField focusTarget) {
        showError(message);
        focusTarget.requestFocus();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
        errorClearTimer.playFromStart(); // 에러가 표시될 때 타이머 시작/재시작
    }

    private void hideError() {
        errorClearTimer.stop(); // 타이머 중지
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void setBusy(boolean busy) {
        btnSave.setDisable(busy);
        btnCancel.setDisable(busy);
    }

    private void closeDialog() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            btnCancel.getScene().getWindow().hide();
        }
    }
}