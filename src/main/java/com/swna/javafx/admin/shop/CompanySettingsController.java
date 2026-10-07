package com.swna.javafx.admin.shop;

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.api.ShopApiClient;
import com.swna.javafx.admin.shop.dto.Shop;

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

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/shop-view.fxml")
public class CompanySettingsController {

    private final ShopApiClient shopApiClient;

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
     * 초기화 메서드 (FXML이 로드된 후 자동으로 호출됨)
     */
    @FXML
    public void initialize() {
        setupEnterKeyFocusTraversal();
        loadFirstShopData(); 
    }

    /**
     * 서버에서 첫 번째 매장 데이터를 가져와 폼에 채워 넣는 메서드
     */
    private void loadFirstShopData() {
        log.info("Fetching the first shop information from the server...");
        
        shopApiClient.fetchShop()
            .subscribe(
                shop -> {
                    javafx.application.Platform.runLater(() -> setShopToFields(shop));
                },
                error -> log.error("Failed to fetch shop information: {}", error.getMessage())
            );
    }

    /**
     * Shop 객체의 데이터를 TextField에 매핑 및 현재 ID 기억
     */
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

    /**
     * SAVE 버튼 클릭 이벤트 - 서버로 수정 내역 전송 (이메일 검증 및 Alert 알림 포함)
     */
    @FXML
    private void handleSave(ActionEvent event) {
        if (currentShopId == null) {
            log.warn("Shop ID not found. Please load data first.");
            showAlert(AlertType.WARNING, "Warning", "No Shop ID found. Please reload data.");
            return;
        }

        // 이메일 형식 검증
        String emailText = tfEmail.getText() != null ? tfEmail.getText().trim() : "";
        if (!emailText.isEmpty() && !EMAIL_PATTERN.matcher(emailText).matches()) {
            log.warn("Invalid email format entered: {}", emailText);
            showAlert(AlertType.ERROR, "Validation Error", "Please enter a valid email address.");
            tfEmail.requestFocus();
            return;
        }

        // 입력된 필드 값들로 Shop 객체 생성
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

        // 서버로 비동기 업데이트 요청 전송
        shopApiClient.updateShop(currentShopId, updatedShop)
                    .subscribe(
                        unused -> {
                            // onNext: Not called for Mono<Void>
                        },
                        error -> {
                            // onError: Triggered when API fails or exception occurs
                            javafx.application.Platform.runLater(() -> {
                                String errorMsg = error.getMessage() != null ? error.getMessage() : "Please try again.";
                                log.error("Failed to save shop information: {}", errorMsg);
                                showAlert(AlertType.ERROR, "Save Failed", "Failed to save shop information: " + errorMsg);
                            });
                        },
                        () -> {
                            // onComplete: Triggered when the update successfully completes
                            javafx.application.Platform.runLater(() -> {
                                log.info("Shop information saved successfully.");
                                showAlert(AlertType.INFORMATION, "Success", "Shop information has been saved successfully.");
                            });
                        }
                    );
            }

    /**
     * 사용자에게 팝업 알림을 보여주는 공통 메서드
     */
    private void showAlert(AlertType alertType, String title, String message) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * RLOAD 버튼 클릭 이벤트
     */
    @FXML
    private void handleReload(ActionEvent event) {
        log.info("Reloading data...");
        loadFirstShopData(); 
    }

    /**
     * CLEAN 버튼 클릭 이벤트
     */
    @FXML
    private void handleClean(ActionEvent event) {
        tfCompany.clear();
        tfBusinessNo.clear();
        tfName.clear();
        tfEmail.clear();
        pfPassword.clear();
        tfCcEmail.clear();
        tfmobilePhone.clear();
        tfPhone.clear();
        tfStreet.clear();
        tfSurburb.clear();
        tfCity.clear();
        tfComment.clear();
        tfBackupFolder.clear();
        tfReportFolder.clear();
        log.info("Input fields cleared.");
    }

    /**
     * Basic Folder 폴더 선택 버튼 이벤트
     */
    @FXML
    private void handleChooseBackupFolder(ActionEvent event) {
        chooseDirectory(tfBackupFolder);
    }

    /**
     * Report Folder 폴더 선택 버튼 이벤트
     */
    @FXML
    private void handleChooseReportFolder(ActionEvent event) {
        chooseDirectory(tfReportFolder);
    }

    /**
     * 공통 디렉토리 선택 다이얼로그 메서드
     */
    private void chooseDirectory(TextField targetTextField) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Directory");
        
        if (!targetTextField.getText().isEmpty()) {
            File initialDir = new File(targetTextField.getText());
            if (initialDir.exists()) {
                directoryChooser.setInitialDirectory(initialDir);
            }
        }
        
        File selectedDirectory = directoryChooser.showDialog(targetTextField.getScene().getWindow());
        if (selectedDirectory != null) {
            targetTextField.setText(selectedDirectory.getAbsolutePath());
        }
    }

    /**
     * TextField 및 PasswordField에서 Enter 키 입력 시 포커스 이동
     */
    private void setupEnterKeyFocusTraversal() {
        TextField[] fields = {
            tfCompany,      
            tfBusinessNo,   
            tfName,         
            tfEmail,        
            pfPassword,     
            tfCcEmail,      
            tfmobilePhone,  
            tfPhone,        
            tfStreet,       
            tfSurburb,      
            tfCity,         
            tfComment,      
            tfBackupFolder, 
            tfReportFolder  
        };

        for (int i = 0; i < fields.length - 1; i++) {
            final int nextIndex = i + 1;
            fields[i].setOnAction(event -> fields[nextIndex].requestFocus());
        }
    }
}