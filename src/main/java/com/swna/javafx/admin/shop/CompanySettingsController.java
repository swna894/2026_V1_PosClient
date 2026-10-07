package com.swna.javafx.admin.shop;

import java.io.File;

import org.springframework.stereotype.Component;

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
    @FXML private TextField tfMobilePhone;
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
        // TODO: 데이터 베이스 저장 또는 파일 저장 로직 구현
    }

    /**
     * RLOAD 버튼 클릭 이벤트
     */
    @FXML
    private void handleReload(ActionEvent event) {
        log.info("데이터 새로고침(Reload) 실행");
        // TODO: 데이터를 다시 불러와 TextField에 세팅하는 로직 구현
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
        tfMobilePhone.clear();
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
            tfMobilePhone,   // Row 6[cite: 13]
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