package com.swna.javafx.backup.viewmodel;

import java.io.File;
import java.util.function.Consumer;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.backup.api.EnvironmentApiClient;
import com.swna.javafx.backup.domain.Environment;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope("prototype")
@Getter
public class BackupEnvViewModel {

    private final EnvironmentApiClient environmentApiClient;

    // ----- UI 바인딩용 프로퍼티 -----
    private final StringProperty backupFolder = new SimpleStringProperty("");
    private final StringProperty reportFolder = new SimpleStringProperty("");
    private final StringProperty backupExe = new SimpleStringProperty("");
    
    private final BooleanProperty saveDisabled = new SimpleBooleanProperty(true);
    private final BooleanProperty busy = new SimpleBooleanProperty(false);

    // ----- 내부 상태 -----
    private Environment currentEnvironment;
    private boolean loaded = false;

    public BackupEnvViewModel(EnvironmentApiClient environmentApiClient) {
        this.environmentApiClient = environmentApiClient;
    }

    /** 환경 설정 데이터 로드 */
    public void loadSettings(Consumer<String> onError) {
        setBusy(true);
        
        // 예시로 단건 조회 (ID 1번 기준 또는 적절한 조회 로직 적용)
        environmentApiClient.getEnvironmentById(1L).subscribe(
            response -> {
                setBusy(false);
                // Record 규격에 맞추어 response.data() 사용 및 성공 여부 확인
                if (response != null && response.isSuccess() && response.data() != null) {
                    currentEnvironment = response.data();
                    backupFolder.set(nullToEmpty(currentEnvironment.getBackupFolder()));
                    reportFolder.set(nullToEmpty(currentEnvironment.getReportFolder()));
                    backupExe.set(nullToEmpty(currentEnvironment.getBackupExe()));
                    loaded = true;
                    saveDisabled.set(false);
                } else {
                    currentEnvironment = Environment.create("", "", "");
                    loaded = true;
                    saveDisabled.set(false);
                }
            },
            error -> {
                setBusy(false);
                log.error("Failed to load the settings.", error);
                onError.accept("Failed to load the settings.\n" + error.getMessage());
            }
        );
    }

    /** 환경 설정 저장 */
    public void saveSettings(Consumer<String> onMessage) {
        if (!loaded || currentEnvironment == null) {
            onMessage.accept("Settings are not loaded yet.");
            return;
        }

        String bFolder = backupFolder.get().trim();
        String rFolder = reportFolder.get().trim();
        String eFolder = backupExe.get().trim();

        String error = validate(bFolder, rFolder, eFolder);
        if (error != null) {
            onMessage.accept(error);
            return;
        }

        Environment updated = Environment.create(bFolder, eFolder, rFolder);
        updated.setId(currentEnvironment.getId());

        setBusy(true);
        var requestMono = (updated.getId() == null) 
            ? environmentApiClient.createEnvironment(updated)
            : environmentApiClient.updateEnvironment(updated.getId(), updated);

        requestMono.subscribe(
            response -> {
                setBusy(false);
                // Record 규격에 맞추어 response.data() 사용
                if (response == null || !response.isSuccess() || response.data() == null) {
                    String errorMsg = (response != null && response.message() != null) 
                            ? response.message() : "Save failed.";
                    onMessage.accept(errorMsg);
                    return;
                }
                currentEnvironment = response.data();
                onMessage.accept("Saved.");
            },
            ex -> {
                setBusy(false);
                log.error("Save failed.", ex);
                onMessage.accept("Save failed.\n" + ex.getMessage());
            }
        );
    }

    /** 입력값 검증 */
    private String validate(String backupFolder, String reportFolder, String exeFolder) {
        if (backupFolder.isEmpty()) {
            return "Backup Folder is required.";
        }
        File backupDir = new File(backupFolder);
        if (backupDir.exists() && !backupDir.isDirectory()) {
            return "Backup Folder is not a folder.";
        }
        if (!reportFolder.isEmpty() && !new File(reportFolder).isDirectory()) {
            return "Report Folder does not exist.";
        }
        if (!exeFolder.isEmpty()) {
            File exeDir = new File(exeFolder);
            if (!exeDir.isDirectory()) {
                return "Backup Exe folder does not exist.";
            }
            if (!hasExecutable(exeDir, "mysqldump") || !hasExecutable(exeDir, "mysql")) {
                return "mysqldump / mysql was not found in the Backup Exe folder.";
            }
        }
        return null;
    }

    private boolean hasExecutable(File dir, String name) {
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        return new File(dir, windows ? name + ".exe" : name).isFile();
    }

    private void setBusy(boolean value) {
        busy.set(value);
        saveDisabled.set(value || !loaded);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}