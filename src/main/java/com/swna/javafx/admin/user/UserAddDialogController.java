package com.swna.javafx.admin.user;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.admin.user.domain.Role;
import com.swna.javafx.admin.user.domain.UserRecordDto;
import com.swna.javafx.admin.user.viewmodel.UserViewModel;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputControl;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
@FxmlView("/view/admin/user-add-dialog.fxml")
public class UserAddDialogController implements Initializable {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int MAX_NAME = 32;
    private static final int MAX_EMAIL = 64;
    private static final int MAX_PASSWORD = 64;
    private static final int MAX_PHONE = 20;
    private static final int MAX_MOBILE = 20;
    private static final int MAX_ADDRESS = 50;

    private final UserViewModel viewModel;

    @FXML private TextField tfName;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private ComboBox<String> cbRole;
    @FXML private TextField tfPhone;
    @FXML private TextField tfMobile;
    @FXML private TextField tfCity;
    @FXML private TextField tfStreet;
    @FXML private TextField tfSuburb;

    @FXML private Button btnSave;
    @FXML private Button btnCancel;
    @FXML private Label lblError;

    // 요청하신 StringProperty 속성 정의
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty street = new SimpleStringProperty();
    private final StringProperty surburb = new SimpleStringProperty();

    private boolean saved;
    private PauseTransition errorClearTimer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        limitLength(tfName, MAX_NAME);
        limitLength(tfEmail, MAX_EMAIL);
        limitLength(pfPassword, MAX_PASSWORD);
        limitLength(tfPhone, MAX_PHONE);
        limitLength(tfMobile, MAX_MOBILE);
        limitLength(tfCity, MAX_ADDRESS);
        limitLength(tfStreet, MAX_ADDRESS);
        limitLength(tfSuburb, MAX_ADDRESS);

        // TextField와 StringProperty 바인딩
        if (tfCity != null) city.bind(tfCity.textProperty());
        if (tfStreet != null) street.bind(tfStreet.textProperty());
        if (tfSuburb != null) surburb.bind(tfSuburb.textProperty());

        setupEnterNavigation();
        setupErrorTimer();
        setupErrorClearingOnInput();

        Platform.runLater(() -> tfName.requestFocus());
    }

    private void setupErrorTimer() {
        errorClearTimer = new PauseTransition(Duration.seconds(4));
        errorClearTimer.setOnFinished(e -> hideError());
    }

    private void setupErrorClearingOnInput() {
        TextInputControl[] controls = {tfName, tfEmail, pfPassword, tfPhone, tfMobile, tfCity, tfStreet, tfSuburb};
        for (TextInputControl control : controls) {
            if (control != null) {
                control.textProperty().addListener((observable, oldValue, newValue) -> {
                    if (lblError.isVisible() && !newValue.equals(oldValue)) {
                        hideError();
                    }
                });
            }
        }
        if (cbRole != null) {
            cbRole.valueProperty().addListener((observable, oldValue, newValue) -> {
                if (lblError.isVisible()) {
                    hideError();
                }
            });
        }
    }

    private void setupEnterNavigation() {
        moveFocusOnEnter(tfName, tfEmail);
        moveFocusOnEnter(tfEmail, pfPassword);
        moveFocusOnEnter(pfPassword, tfMobile);
        moveFocusOnEnter(tfMobile, tfPhone);
        moveFocusOnEnter(tfPhone, tfCity);
        moveFocusOnEnter(tfCity, tfStreet);
        moveFocusOnEnter(tfStreet, tfSuburb);
    }

    private void moveFocusOnEnter(TextField current, javafx.scene.Node next) {
        if (current != null) {
            current.setOnAction(e -> {
                if (next != null) next.requestFocus();
            });
        }
    }

    public boolean isSaved() {
        return saved;
    }

    @FXML
    private void handleSave() {
        hideError();

        String name = trim(tfName.getText());
        String email = trim(tfEmail.getText());
        String password = pfPassword.getText();
        
        String roleStr = cbRole != null ? cbRole.getValue() : "USER";
        Role role = Role.valueOf(roleStr != null ? roleStr : "USER");

        String phone = trim(tfPhone.getText());
        String mobile = trim(tfMobile.getText());
        String cityVal = trim(city.get());
        String streetVal = trim(street.get());
        String suburbVal = trim(surburb.get());

        if (name.isEmpty()) {
            fail("NAME is required.", tfName);
            return;
        }
        if (email.isEmpty()) {
            fail("E-MAIL is required.", tfEmail);
            return;
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            fail("E-MAIL format is invalid.", tfEmail);
            return;
        }
        if (password == null || password.isEmpty()) {
            fail("PASSWORD is required.", pfPassword);
            return;
        }

        UserRecordDto request = new UserRecordDto(
                null,           // id
                email,          // email
                name,           // name
                role,           // role
                cityVal,        // city
                streetVal,      // street
                suburbVal,      // zipcode (또는 surburb 매핑 필드)
                phone,          // phone
                mobile          // mobile
        );

        setBusy(true);
        log.info("user request = {}", request);

        viewModel.createUser(request)
                .subscribe(success -> Platform.runLater(() -> {
                    setBusy(false);
                    if (Boolean.TRUE.equals(success)) {
                        saved = true;
                        closeDialog();
                    } else {
                        showError("Failed to create user.");
                    }
                }));
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void limitLength(TextInputControl control, int max) {
        if (control != null) {
            control.setTextFormatter(new TextFormatter<String>(
                    change -> change.getControlNewText().length() <= max ? change : null));
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private void fail(String message, TextInputControl focusTarget) {
        showError(message);
        if (focusTarget != null) {
            focusTarget.requestFocus();
        }
    }

    private void showError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
            errorClearTimer.playFromStart();
        }
    }

    private void hideError() {
        if (errorClearTimer != null) {
            errorClearTimer.stop();
        }
        if (lblError != null) {
            lblError.setText("");
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    private void setBusy(boolean busy) {
        if (btnSave != null) btnSave.setDisable(busy);
        if (btnCancel != null) btnCancel.setDisable(busy);
    }

    private void closeDialog() {
        if (btnCancel != null && btnCancel.getScene() != null) {
            btnCancel.getScene().getWindow().hide();
        }
    }
}