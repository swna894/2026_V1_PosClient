package com.swna.javafx.admin.shop;

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.dto.Shop;
import com.swna.javafx.admin.shop.viewmodel.ShopViewModel;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/shop-view.fxml")
public class ShopSettingsController {

    private final ShopViewModel shopViewModel;

    private Long currentShopId;

    // 이메일 유효성 검증을 위한 표준 정규식 패턴
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    );

    // --- 상단 버튼 ---
    @FXML private Button btnSave;
    @FXML private Button btnReload;
    @FXML private Button btnClean;

    // --- 입력 필드 ---
    @FXML private TextField tfCompany;
    @FXML private TextField tfBusinessNo;
    @FXML private TextField tfName;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private TextField tfCcEmail;
    @FXML private TextField tfmobilePhone;
    @FXML private TextField tfPhone;
    @FXML private TextField tfStreet;
    @FXML private TextField tfSurburb;
    @FXML private TextField tfCity;
    @FXML private TextField tfComment;
    @FXML private TextField tfBackupFolder;
    @FXML private TextField tfReportFolder;

    // --- 폴더 선택 버튼 ---
    @FXML private Button btnBackupFolder;
    @FXML private Button btnReportFolder;

    /**
     * FXML 로드 후 자동 호출
     */
    @FXML
    public void initialize() {
        setupEnterKeyFocusTraversal();
        loadShopData(shopViewModel.getShop()); // 캐시 우선
    }

    // ==========================================
    // Load
    // ==========================================

    /**
     * 주어진 Mono<Shop>을 구독하여 폼에 채워 넣는다.
     */
    private void loadShopData(Mono<Shop> source) {
        log.info("Fetching shop information...");
        source.subscribe(
            shop -> Platform.runLater(() -> setShopToFields(shop)),
            error -> log.error("Failed to fetch shop information: {}", error.getMessage())
        );
    }

    private void setShopToFields(Shop shop) {
        if (shop == null) {
            log.warn("No shop data retrieved.");
            return;
        }

        currentShopId = shop.getId();

        tfCompany.setText(shop.getCompany());
        tfBusinessNo.setText(shop.getBusinessNo());
        tfName.setText(shop.getName());
        tfEmail.setText(shop.getEmail());
        pfPassword.setText(shop.getPassword());
        tfCcEmail.setText(shop.getCcEmail());
        tfmobilePhone.setText(shop.getMobilePhone());
        tfPhone.setText(shop.getPhone());
        tfStreet.setText(shop.getStreet());
        tfSurburb.setText(shop.getSuburb());
        tfCity.setText(shop.getCity());
        tfComment.setText(shop.getComment());
        tfBackupFolder.setText(shop.getBackupFolder());
        tfReportFolder.setText(shop.getReportFolder());

        log.info("Shop information successfully loaded into the form. (ID: {})", currentShopId);
    }

    // ==========================================
    // Button handlers
    // ==========================================

    /**
     * SAVE 버튼 - 이메일 검증 후 서버로 수정 내역 전송
     */
    @FXML
    private void handleSave(ActionEvent event) {
        if (currentShopId == null) {
            log.warn("Shop ID not found. Please load data first.");
            showAlert(AlertType.WARNING, "Warning", "No Shop ID found. Please reload data.");
            return;
        }

        String emailText = tfEmail.getText() != null ? tfEmail.getText().trim() : "";
        if (!emailText.isEmpty() && !EMAIL_PATTERN.matcher(emailText).matches()) {
            log.warn("Invalid email format entered: {}", emailText);
            showAlert(AlertType.ERROR, "Validation Error", "Please enter a valid email address.");
            tfEmail.requestFocus();
            return;
        }

        Shop updatedShop = Shop.create(
            tfCompany.getText(),
            tfBusinessNo.getText(),
            tfName.getText(),
            emailText,
            pfPassword.getText(),
            tfCcEmail.getText(),
            tfmobilePhone.getText(),
            tfPhone.getText(),
            tfStreet.getText(),
            tfSurburb.getText(),
            tfCity.getText(),
            tfComment.getText(),
            tfBackupFolder.getText(),
            tfReportFolder.getText()
        );
        updatedShop.setId(currentShopId);

        log.info("Starting update request for shop ID: {}", currentShopId);

        shopViewModel.saveShop(currentShopId, updatedShop)
            .subscribe(
                saved -> Platform.runLater(() -> {
                    log.info("Shop information saved successfully.");
                    showAlert(AlertType.INFORMATION, "Success", "Shop information has been saved successfully.");
                }),
                error -> Platform.runLater(() -> {
                    String msg = error.getMessage() != null ? error.getMessage() : "Please try again.";
                    log.error("Failed to save shop information: {}", msg);
                    showAlert(AlertType.ERROR, "Save Failed", "Failed to save shop information: " + msg);
                })
            );
    }

    /**
     * RELOAD 버튼 - 캐시를 무시하고 서버에서 다시 조회
     */
    @FXML
    private void handleReload(ActionEvent event) {
        log.info("Reloading data...");
        loadShopData(shopViewModel.reload());
    }

    /**
     * CLEAN 버튼 - 입력 필드 초기화
     */
    @FXML
    private void handleClean(ActionEvent event) {
        for (TextField field : allFields()) {
            field.clear();
        }
        log.info("Input fields cleared.");
    }

    @FXML
    private void handleChooseBackupFolder(ActionEvent event) {
        chooseDirectory(tfBackupFolder);
    }

    @FXML
    private void handleChooseReportFolder(ActionEvent event) {
        chooseDirectory(tfReportFolder);
    }

    // ==========================================
    // Helpers
    // ==========================================

    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void chooseDirectory(TextField targetTextField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");

        String current = targetTextField.getText();
        if (current != null && !current.isEmpty()) {
            File initialDir = new File(current);
            if (initialDir.exists()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }

        File selected = directoryChooser.showDialog(targetTextField.getScene().getWindow());
        if (selected != null) {
            targetTextField.setText(selected.getAbsolutePath());
        }
    }

    /**
     * Enter 키 입력 시 다음 필드로 포커스 이동
     */
    private void setupEnterKeyFocusTraversal() {
        TextField[] fields = allFields();
        for (int i = 0; i < fields.length - 1; i++) {
            final TextField next = fields[i + 1];
            fields[i].setOnAction(e -> next.requestFocus());
        }
    }

    /**
     * 폼 입력 필드 목록 (순서 = 포커스 이동 순서)
     */
    private TextField[] allFields() {
        return new TextField[] {
            tfCompany, tfBusinessNo, tfName, tfEmail, pfPassword, tfCcEmail,
            tfmobilePhone, tfPhone, tfStreet, tfSurburb, tfCity, tfComment,
            tfBackupFolder, tfReportFolder
        };
    }
}
