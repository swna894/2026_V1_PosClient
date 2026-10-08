package com.swna.javafx.admin.user;

import java.net.URL;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.supplier.SupplierAddDialogController;
import com.swna.javafx.admin.user.domain.User;
import com.swna.javafx.admin.user.factory.UserTableFactory;
import com.swna.javafx.admin.user.viewmodel.UserViewModel;
import com.swna.javafx.common.navigation.NavigationService;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/user-view.fxml")
public class UserController implements Initializable {

    private final UserViewModel viewModel;
    private final NavigationService navigationService;
    private final UserTableFactory userTableFactory;

    @FXML private BorderPane borderPane;
    @FXML private ToolBar mainToolBar;
    @FXML private CheckBox activeOnlyCheckBox;

    @FXML private Label countLabel;
    @FXML private Label statusLabel;

    @FXML private Button backButton;
    @FXML private Button deleteButton;
    @FXML private Button newButton;
    @FXML private Button refreshButton;
    
    @FXML private ProgressIndicator progressIndicator;
    
    @FXML private TextField searchField;
    @FXML private TableView<User> tableView; // 와일드카드에서 User 타입으로 구체화

    @Override public void initialize(URL location, ResourceBundle resources) {
        // 1. TableView 및 컬럼 초기화 (수정/삭제 액션 핸들러 연결)[cite: 3]
        userTableFactory.initializeTable(
                tableView,
                this::handleEditUser,
                this::handleDeleteUser
        );

        // 2. ViewModel 상태 속성 UI 바인딩
        if (progressIndicator != null) {
            progressIndicator.visibleProperty().bind(viewModel.getLoading());
        }
        if (statusLabel != null) {
            statusLabel.textProperty().bind(viewModel.getStatusMessage());
        }

        // 3. 버튼 이벤트 핸들러 설정 (새로고침 버튼 등)
        if (refreshButton != null) {
            refreshButton.setOnAction(event -> viewModel.loadUsers());
        }

        // 4. 컨트롤러 초기 실행(진입) 시 서버에서 사용자 데이터 로드[cite: 1]
        viewModel.loadUsers();
    }


    @FXML void handleNewUser(ActionEvent event) {
        navigationService.openModalWindow(UserAddDialogController.class, "Add User");
    }
    /**
     * 행 수정(저장) 버튼 클릭 시 동작
     */
    private void handleEditUser(User user) {
        viewModel.updateUser(user).subscribe(success -> {
            if (Boolean.TRUE.equals(success)) {
                log.info("[Controller] User successfully updated: ID={}", user.getId());
            } else {
                log.error("[Controller] Failed to update user: ID={}", user.getId());
            }
        });
    }

    /**
     * 행 삭제 버튼 클릭 시 동작
     */
    private void handleDeleteUser(User user) {
        viewModel.deleteUser(user.getId()).subscribe(success -> {
            if (Boolean.TRUE.equals(success)) {
                log.info("[Controller] User successfully deleted: ID={}", user.getId());
            } else {
                log.error("[Controller] Failed to delete user: ID={}", user.getId());
            }
        });
    }
}