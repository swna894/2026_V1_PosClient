package com.swna.javafx.pos.dialog;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component 
@Scope("prototype")
@FxmlView("/view/pos/dialog/ConfirmDialog.fxml")
public class ConfirmDialogController extends BasePaymentDialog {
    
    @FXML
    private StackPane paneReceiptRow;  // 포커스를 받을 영역 (선택사항)
    
    @FXML
    private TextField txtReceiptNumber;  // 숨겨진 필드로 사용 가능 (선택사항)
    
    @FXML
    public void initialize() {
        // 전체 창 드래그 기능 활성화
        enableFullWindowDrag();
        
        // ESC 키 전역 처리 (선택사항)
        setupGlobalKeyEvents();
    }
    
    /**
     * 다이얼로그 전체에서 ESC 키 처리
     */
    private void setupGlobalKeyEvents() {
        TextField focusField = getFocusField();
        if (focusField != null && focusField.getScene() != null) {
            focusField.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    handleCancel();
                    event.consume();
                } else if (event.getCode() == KeyCode.ENTER) {
                    handleConfirm();
                    event.consume();
                }
            });
        }
    }
    
    @FXML
    @Override
    protected void handleConfirm() {
        log.info("Confirm button clicked - Deleting all items");
        
        // 서버에 DELETE 요청 로직 호출
        deleteAllItemsToServer();
        
        // 다이얼로그 닫기
        closeDialog();
    }
    
    @FXML
    @Override
    protected void handleCancel() {
        log.info("Cancel button clicked - Delete operation cancelled");
        closeDialog();
    }
    
    @Override
    protected TextField getFocusField() {
        // 포커스를 받을 TextField 반환 (없으면 null)
        // 드래그 기능을 위해 필요하지만, TextField가 없으면 StackPane이나 다른 노드 사용 가능
        return txtReceiptNumber;
    }
    
    private void deleteAllItemsToServer() {
        try {
            log.info("Sending DELETE request to server for all items...");
            
            // REST API 호출 예시
            // ResponseEntity<Void> response = restTemplate.exchange(
            //     "/api/items/all",
            //     HttpMethod.DELETE,
            //     null,
            //     Void.class
            // );
            
            // if (response.getStatusCode().is2xxSuccessful()) {
            //     log.info("All items deleted successfully from server");
            // } else {
            //     log.error("Failed to delete items from server");
            // }
            
            // 임시 로그
            log.info("All items deleted from server successfully");
            
        } catch (Exception e) {
            log.error("Error while deleting items from server", e);
        }
    }
}