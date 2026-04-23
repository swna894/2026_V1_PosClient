package com.swna.javafx.view.login;

import org.springframework.stereotype.Component;

import com.swna.javafx.ui.ViewInfo;
import com.swna.javafx.viewmodel.login.LoginViewModel;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@FxmlView("/view/login/loginView.fxml")
public class LoginView implements ViewInfo{

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Circle onClose;

    private final LoginViewModel vm;

    public LoginView(LoginViewModel vm) {
        this.vm = vm;
    }

    @Override
    public String getTitle() {
        return "메인 화면";
    }

    @FXML
    public void initialize() {

        // 🔹 입력 바인딩
        usernameField.textProperty().bindBidirectional(vm.usernameProperty());
        passwordField.textProperty().bindBidirectional(vm.passwordProperty());

        // 🔹 상태 바인딩
        statusLabel.textProperty().bind(vm.statusProperty());
        errorLabel.textProperty().bind(vm.errorProperty());

        // 🔹 로딩 상태 → 버튼 disable
        loginButton.disableProperty().bind(vm.loadingProperty());

    }


    @FXML
    void onCancel(ActionEvent event) {

    }

    @FXML
    private void onClose(MouseEvent event) { 
        System.exit(0); 
    }

    @FXML
    void onLogin(ActionEvent event) {
    	vm.login();
    }

    @FXML
    void onName(ActionEvent event) {

    }

    @FXML
    void onPassword(ActionEvent event) {

    }

}
