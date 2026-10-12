package com.swna.javafx.backup;

import java.io.File;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.backup.viewmodel.BackupEnvViewModel;
import com.swna.javafx.common.util.AlertDialog;

import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.RequiredArgsConstructor;
import net.rgielen.fxweaver.core.FxmlView;

@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/admin/backup-env-view.fxml")
public class BackupEnvController {

    private final BackupEnvViewModel viewModel;
    private final AlertDialog alertDialog;

    // ----- FXML 주입 -----
    @FXML private TextField tfBackupFolder;
    @FXML private TextField tfReportFolder;
    @FXML private TextField tfBackExe;
    @FXML private Button btnBackupFolder;
    @FXML private Button btnReportFolder;
    @FXML private Button btnBackExe;
    @FXML private Button btnSave;
    @FXML private Button btnClose;

    @FXML private void initialize() {
        // ViewModel 프로퍼티와 UI 컴포넌트 양방향 바인딩
        tfBackupFolder.textProperty().bindBidirectional(viewModel.getBackupFolder());
        tfReportFolder.textProperty().bindBidirectional(viewModel.getReportFolder());
        tfBackExe.textProperty().bindBidirectional(viewModel.getBackupExe());

        // 버튼 비활성화 상태 바인딩
        btnSave.disableProperty().bind(viewModel.getSaveDisabled());

        // Busy 상태 변화에 따른 커서 및 버튼 제어 리스너
        viewModel.getBusy().addListener((obs, oldVal, busy) -> {
            Scene scene = btnClose.getScene();
            if (scene != null) {
                scene.getRoot().setCursor(busy ? Cursor.WAIT : Cursor.DEFAULT);
            }
            btnBackupFolder.setDisable(busy);
            btnReportFolder.setDisable(busy);
            btnBackExe.setDisable(busy);
        });

        // 초기 데이터 로드
        viewModel.loadSettings(msg -> alertDialog.dialogString(msg));
    }

    @FXML private void handleChooseBackupFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select backup folder");

        String currentText = tfBackupFolder.getText();
        if (currentText != null && !currentText.trim().isEmpty()) {
            File current = new File(currentText.trim());
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
        }
        
        File selected = chooser.showDialog(window());
        if (selected != null) {
            String path = selected.toString();
            tfBackupFolder.setText(path);
            // 바인딩 누락 방지를 위해 뷰모델 프로퍼티에 직접 값 동기화
            viewModel.getBackupFolder().set(path);
        }
    }

    @FXML private void handleChooseReportFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select report folder");

        String currentText = tfReportFolder.getText();
        if (currentText != null && !currentText.trim().isEmpty()) {
            File current = new File(currentText.trim());
            if (current.isDirectory()) {
                chooser.setInitialDirectory(current);
            }
        }
        
        File selected = chooser.showDialog(window());
        if (selected != null) {
            String path = selected.toString();
            tfReportFolder.setText(path);
            // 바인딩 누락 방지를 위해 뷰모델 프로퍼티에 직접 값 동기화
            viewModel.getReportFolder().set(path);
        }
    }

    // 파일 선택(FileChooser) 및 뷰모델 동기화 보완
    @FXML private void handleBackExeFolder() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select MySQL executable (mysqldump / mysql)");

        String currentText = tfBackExe.getText();
        if (currentText != null && !currentText.trim().isEmpty()) {
            File current = new File(currentText.trim());
            if (current.exists()) {
                if (current.isDirectory()) {
                    fileChooser.setInitialDirectory(current);
                } else if (current.getParentFile() != null && current.getParentFile().isDirectory()) {
                    fileChooser.setInitialDirectory(current.getParentFile());
                }
            }
        }

        // 실행 파일 확장자 필터 (Windows 환경 고려)
        boolean isWindows = System.getProperty("os.name", "").toLowerCase().contains("win");
        if (isWindows) {
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Executable Files", "*.exe")
            );
        }
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selected = fileChooser.showOpenDialog(window());
        if (selected != null) {
            String path = selected.toString();
            tfBackExe.setText(path);
            // 바인딩 누락 방지를 위해 뷰모델 프로퍼티에 직접 값 동기화
            viewModel.getBackupExe().set(path);
        }
    }

    @FXML private void handleSave() {
        viewModel.saveSettings(msg -> alertDialog.dialogString(msg));
    }

    @FXML private void handleClose() {
        if (window() instanceof Stage stage) {
            stage.close();
        }
    }

    private Window window() {
        Scene scene = btnClose.getScene();
        return scene == null ? null : scene.getWindow();
    }
}