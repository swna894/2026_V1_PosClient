package com.swna.javafx.admin.shop;

import java.io.File;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.shop.api.ShopApiClient;
import com.swna.javafx.admin.shop.dto.Shop;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
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

    // --- 상단 버튼 ---
    @FXML private Button btnSave;
    @FXML private Button btnReload;
    @FXML private Button btnClean;

    // --- 입력 필드 (FXML에 정의된 순서 및 ID와 일치) ---
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
    @FXML private TextField tfBackupFolder;   // FXML의 Basic Folder 내 텍스트필드 ID 매핑[cite: 13]
    @FXML private TextField tfReportFolder;  // FXML의 Report Folder 내 텍스트필드 ID 매핑[cite: 13]

    // --- 폴더 선택 버튼 (FXML과 일치) ---
    @FXML private Button btnBackupFolder;    // FXML의 Basic Folder 내 버튼 ID 매핑[cite: 13]
    @FXML private Button btnReportFolder;    // FXML의 Report Folder 내 버튼 ID 매핑[cite: 13]

    /**
     * 초기화 메서드 (FXML이 로드된 후 자동으로 호출됨)
     */
    @FXML
    public void initialize() {
        setupEnterKeyFocusTraversal();
        loadFirstShopData(); // 화면 로딩 시 서버에서 첫 번째 샵 데이터를 가져와 세팅
    }

    /**
     * 서버에서 첫 번째 매장 데이터를 가져와 폼에 채워 넣는 메서드
     */
    private void loadFirstShopData() {
        log.info("서버에서 첫 번째 매장 정보를 불러오는 중...");
        
        shopApiClient.fetchShop()
            .subscribe(
                shop -> {
                    // JavaFX UI 스레드 안전하게 반영
                    javafx.application.Platform.runLater(() -> setShopToFields(shop));
                },
                error -> log.error("매장 정보를 불러오는데 실패했습니다: {}", error.getMessage())
            );
    }

    /**
     * Shop 객체의 데이터를 TextField에 매핑
     */
    private void setShopToFields(Shop shop) {
        if (shop == null) {
            log.warn("불러온 매장 데이터가 없습니다.");
            return;
        }

        tfCompany.setText(shop.getCompany());
        tfBusinessNo.setText(shop.getBusinessNo());
        tfName.setText(shop.getName());
        tfEmail.setText(shop.getEmail());
        pfPassword.setText(shop.getPassword());
        tfCcEmail.setText(shop.getCcEmail());
        tfmobilePhone.setText(shop.getMobilePhone()); // Shop_5.java의 필드명(mobilePhone)에 맞춤
        tfPhone.setText(shop.getPhone());
        tfStreet.setText(shop.getStreet());
        tfSurburb.setText(shop.getSuburb());
        tfCity.setText(shop.getCity());
        tfComment.setText(shop.getComment());
        tfBackupFolder.setText(shop.getBackupFolder());
        tfReportFolder.setText(shop.getReportFolder());

        log.info("매장 정보가 입력 폼에 성공적으로 세팅되었습니다.");
    }

    /**
     * SAVE 버튼 클릭 이벤트
     */
    @FXML
    private void handleSave(ActionEvent event) {
        String company = tfCompany.getText();
        String businessNo = tfBusinessNo.getText();
        String email = tfEmail.getText();
        String password = pfPassword.getText();
        
        log.info("저장 실행: Company = {}, Business No = {}, Email = {}", company, businessNo, email);
        // TODO: 서버로 Update 요청 전송 로직 구현 (shopApiClient.updateShop(...))
    }

    /**
     * RLOAD 버튼 클릭 이벤트
     */
    @FXML
    private void handleReload(ActionEvent event) {
        log.info("데이터 새로고침(Reload) 실행");
        loadFirstShopData(); // 새로고침 시 다시 데이터를 서버에서 호출
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
        log.info("입력창 초기화(Clean) 완료");
    }

    /**
     * Basic Folder 폴더 선택 버튼 이벤트 (FXML: handleChooseBackupFolder)[cite: 13]
     */
    @FXML
    private void handleChooseBackupFolder(ActionEvent event) {
        chooseDirectory(tfBackupFolder);
    }

    /**
     * Report Folder 폴더 선택 버튼 이벤트 (FXML: handleChooseReportFolder)[cite: 13]
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
        directoryChooser.setTitle("폴더 선택");
        
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
     * TextField 및 PasswordField에서 Enter 키 입력 시 
     * FXML의 GridPane 배치 순서와 동일하게 다음 필드로 포커스를 이동시키는 메서드
     */
    private void setupEnterKeyFocusTraversal() {
        TextField[] fields = {
            tfCompany,       // Row 0[cite: 13]
            tfBusinessNo,    // Row 1[cite: 13]
            tfName,          // Row 2[cite: 13]
            tfEmail,         // Row 3[cite: 13]
            pfPassword,      // Row 4[cite: 13]
            tfCcEmail,       // Row 5[cite: 13]
            tfmobilePhone,     // Row 6[cite: 13]
            tfPhone,         // Row 7[cite: 13]
            tfStreet,        // Row 8[cite: 13]
            tfSurburb,       // Row 9[cite: 13]
            tfCity,          // Row 10[cite: 13]
            tfComment,       // Row 11[cite: 13]
            tfBackupFolder,  // Row 12 (Basic Folder)[cite: 13]
            tfReportFolder   // Row 13 (Report Folder)[cite: 13]
        };

        for (int i = 0; i < fields.length - 1; i++) {
            final int nextIndex = i + 1;
            fields[i].setOnAction(event -> fields[nextIndex].requestFocus());
        }

        // 마지막 필드(tfReportFolder)에서 Enter 입력 시 동작 설정
        tfReportFolder.setOnAction(event -> {
            // 예: handleSave(event); 등 저장 로직으로 연계 가능
        });
    }
}