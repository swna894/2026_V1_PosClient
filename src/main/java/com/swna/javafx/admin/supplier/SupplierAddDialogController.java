package com.swna.javafx.admin.supplier;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.domain.Supplier;
import com.swna.javafx.admin.supplier.viewmodel.SupplierViewModel;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
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
    // 대문자, 숫자, 특수문자(ASCII 기호: !"#$%&'()*+,-./:;<=>?@[\]^_`{|}~)만 허용. 공백/한글은 불가.
    private static final String ABBR_REGEX = "[A-Z0-9\\p{Punct}]+";
    private static final Pattern ABBR_PATTERN = Pattern.compile("^" + ABBR_REGEX + "$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    // 서버 엔티티(Supplier)의 컬럼 길이와 동일
    private static final int MIN_ABBR = 2;   // 서버 @Size(min = 2, max = 8)
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

        javafx.application.Platform.runLater(() -> tfAbbr.requestFocus());
    }

    /**
     * Enter 키를 누르면 다음 입력 필드로 포커스를 이동한다.
     * ABBR -> NAME -> COMPANY -> E-MAIL -> PHONE -> MOBILE -> ADDRESS -> ACTIVE
     * 마지막 ACTIVE 체크박스에서 Enter 를 누르면 기본 버튼(SAVE)이 실행된다.
     *
     * TextField 는 onAction 핸들러가 있으면 Enter 이벤트를 소비하므로,
     * 중간 필드에서 Enter 를 눌러도 기본 버튼(SAVE)이 실행되지 않는다.
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

    // =================================================
    // 외부에서 사용하는 메서드
    // =================================================

    /** 저장에 성공해서 닫혔는지 여부 */
    public boolean isSaved() {
        return saved;
    }

    // =================================================
    // 버튼 핸들러
    // =================================================

    @FXML
    private void handleSave() {
        hideError();

        String abbr = trim(tfAbbr.getText());
        String name = trim(tfName.getText());
        String email = trim(tfEmail.getText());

        // 1. 클라이언트 사전 검증
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

        // 2. 도메인 객체 생성
        Supplier supplier = new Supplier();
        supplier.setAbbr(abbr);
        supplier.setName(name);
        supplier.setCompany(trim(tfCompany.getText()));
        supplier.setEmail(email);
        supplier.setPhone(trim(tfPhone.getText()));
        supplier.setCellphone(trim(tfCellphone.getText()));
        supplier.setAddress(trim(tfAddress.getText()));
        supplier.setActive(cbActive.isSelected());

        // 3. 서버 저장 (성공 시 닫고, 실패 시 열어둔 채 메시지 표시)
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

    // =================================================
    // 내부 헬퍼
    // =================================================

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
    }

    private void hideError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void setBusy(boolean busy) {
        btnSave.setDisable(busy);
        btnCancel.setDisable(busy);
    }

    /** NavigationService 가 만든 Stage 를 직접 알 필요 없이 Scene 에서 창을 찾아 닫는다. */
    private void closeDialog() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            btnCancel.getScene().getWindow().hide();
        }
    }
}
