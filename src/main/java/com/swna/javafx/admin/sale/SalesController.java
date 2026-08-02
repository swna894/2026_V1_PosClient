package com.swna.javafx.admin.sale;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.viewmodel.SalesViewModel;
import com.swna.javafx.common.navigation.NavigationService;
import com.swna.javafx.pos.PosViewController;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SplitPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

/**
 * 판매 메인 컨트롤러 (바인딩 및 컴파일 이슈 리팩토링 버전)
 */
@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/sale/MainSalesView.fxml")
public class SalesController implements Initializable {
    
    private final SalesViewModel viewModel;
    private final NavigationService navigationService;

    // 1. 멤버 변수로 버튼 배열을 선언 (필드 레벨)
    private Button[] navButtons;

    // 버튼 상태 관리를 위한 PseudoClass
    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    
    // ========== ToolBar Controls ==========
    @FXML private Button backButton;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private Button todayBtn;
    @FXML private Button weekBtn;
    @FXML private Button monthBtn;
    @FXML private Button searchButton;
    @FXML private Button excelButton;
    @FXML private Button reloadButton;
    @FXML private Button deleteButton;

    @FXML private SplitPane splitPane;
    
    // ========== Daily Summary Labels (ToolBar 내부) ==========
    @FXML private Label summaryDateLabel;
    @FXML private Label summaryTotalLabel;
    @FXML private Label summaryDiscountLabel;
    @FXML private Label summaryCashLabel;
    @FXML private Label summaryCreditLabel;
    @FXML private Label summaryCashoutLabel;
    
    // ========== Loading & Status ==========
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label totalCountLabel;
    @FXML private Label lblStatus;
    
    // ========== Included Components ==========
    @FXML private SalesTableController salesTableController;
    @FXML private SaleItemsTableController saleItemsTableController;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 2. 초기화 시점에 배열 생성
        this.navButtons = new Button[] { todayBtn, weekBtn, monthBtn, searchButton };

        setupBindings();
        setupDatePickers();
        setupButtonActions();
        fixSplitPaneDivider();

        viewModel.loadTodaySales();
        updateButtonSelection(todayBtn);
    }
    
    private void fixSplitPaneDivider() {
        Platform.runLater(() -> splitPane.setDividerPositions(0.60));
        splitPane.sceneProperty().addListener((obs, old, newScene) -> {
            if (newScene != null) {
                Platform.runLater(() -> splitPane.setDividerPositions(0.60));
            }
        });
    }

    private void setupBindings() {
        if (startDatePicker != null) {
            startDatePicker.valueProperty().bindBidirectional(viewModel.startDateProperty());
        }
        if (endDatePicker != null) {
            endDatePicker.valueProperty().bindBidirectional(viewModel.endDateProperty());
        }
        
        if (progressIndicator != null) {
            viewModel.loadingProperty().addListener((obs, old, val) -> 
                progressIndicator.setVisible(val != null && val)
            );
        }
        
        if (lblStatus != null) {
            viewModel.errorMessageProperty().addListener((obs, old, msg) -> 
                lblStatus.setText(msg != null ? msg : "Ready")
            );
        }
        
        if (totalCountLabel != null) {
            totalCountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> String.format("Total: %d", viewModel.totalCountProperty().get()),
                    viewModel.totalCountProperty()
                )
            );
        }
        
        if (summaryDateLabel != null) {
            summaryDateLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                )
            );
        }
        
        if (summaryTotalLabel != null) {
            summaryTotalLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalSalesAmountProperty().get()),
                    viewModel.totalSalesAmountProperty()
                )
            );
        }
        
        if (summaryDiscountLabel != null) {
            summaryDiscountLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalDiscountAmountProperty().get()),
                    viewModel.totalDiscountAmountProperty()
                )
            );
        }
        
        // 💡 [교정] 복잡한 계산식 대신 정밀 집계된 현금 프로퍼티 직접 매핑
        if (summaryCashLabel != null) {
            summaryCashLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalCashAmountProperty().get()),
                    viewModel.totalCashAmountProperty()
                )
            );
        }
        
        // 💡 [교정] receivedAmount 대신 순수 카드 결제액 합계와 정확히 동기화
        if (summaryCreditLabel != null) {
            summaryCreditLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalCreditAmountProperty().get()),
                    viewModel.totalCreditAmountProperty()
                )
            );
        }
        
        if (summaryCashoutLabel != null) {
            summaryCashoutLabel.textProperty().bind(
                Bindings.createStringBinding(
                    () -> formatCurrency(viewModel.totalCashoutAmountProperty().get()),
                    viewModel.totalCashoutAmountProperty()
                )
            );
        }
    }
    
    private void setupDatePickers() {
        if (startDatePicker != null && startDatePicker.getValue() == null) {
            startDatePicker.setValue(LocalDate.now());
        }
        if (endDatePicker != null && endDatePicker.getValue() == null) {
            endDatePicker.setValue(LocalDate.now());
        }
    }
    
private void setupButtonActions() {
        // 각 버튼의 액션 설정
        if (todayBtn != null) todayBtn.setOnAction(e -> { updateButtonSelection(todayBtn); viewModel.loadTodaySales(); });
        if (weekBtn != null) weekBtn.setOnAction(e -> { updateButtonSelection(weekBtn); viewModel.loadThisWeekSales(); });
        if (monthBtn != null) monthBtn.setOnAction(e -> { updateButtonSelection(monthBtn); viewModel.loadThisMonthSales(); });
        if (searchButton != null) searchButton.setOnAction(e -> { updateButtonSelection(searchButton); viewModel.loadSalesByDateRange(); });
        
        if (backButton != null) backButton.setOnAction(e -> handleBack());
        if (excelButton != null) excelButton.setOnAction(e -> handleExportExcel());
        if (reloadButton != null) reloadButton.setOnAction(e -> viewModel.refresh());
        if (deleteButton != null) deleteButton.setOnAction(e -> handleDelete());
    }

    private void updateButtonSelection(Button selectedBtn) {
        for (Button btn : navButtons) {
            if (btn != null) {
                btn.pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, btn == selectedBtn);
            }
        }
    }
    
    private void handleBack() {
        navigationService.openWindow(PosViewController.class);
    }
    
    private void handleExportExcel() {
        log.info("Excel export button clicked");
    }
    
    private void handleDelete() {
        // 삭제 기능 공란 유지
    }
    
    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "$0.00";
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance();
        return currencyFormat.format(amount);
    }
    
    public void refresh() {
        viewModel.refresh();
    }
    
    public void setDateRange(LocalDate start, LocalDate end) {
        if (startDatePicker != null) startDatePicker.setValue(start);
        if (endDatePicker != null) endDatePicker.setValue(end);
        viewModel.loadSalesByDateRange();
    }
}