package com.swna.javafx.admin;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.sale.SalesController;
import com.swna.javafx.admin.supplier.SupplierController;
import com.swna.javafx.admin.unpacking.UnPackingController;
import com.swna.javafx.barcode.LabelController;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxWeaver;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/Management.fxml")
public class MenuController {

    private final MenuViewModel viewModel;
    private final FxWeaver fxWeaver; // 👈 FxWeaver 주입받기

    @FXML private BorderPane mainBorderPane; // 👈 Management.fxml의 BorderPane 바인딩

    @FXML
    public void initialize() {
        log.info("MenuController initialized.");
    }

    /**
     * Center 영역의 화면(Node)을 다른 Controller의 View로 교체
     */
    public <T> void setCenterView(Class<T> controllerClass) {
        try {
            Parent view = fxWeaver.loadView(controllerClass);
            mainBorderPane.setCenter(view);
        } catch (Exception e) {
            log.error("Failed to load view for center area: {}", controllerClass.getSimpleName(), e);
      
        }
    }
    // ==========================================
    // Sales & Reports Handlers
    // ==========================================
    @FXML public void handleDailySales(ActionEvent event) { viewModel.openDailySales(); }
    @FXML public void handleSalesHistory(ActionEvent event) { 
        setCenterView(SalesController.class); 
    } 
    @FXML public void handleProductSalesRanking(ActionEvent event) { viewModel.openProductSalesRanking(); }
    @FXML public void handleSalesStatistics(ActionEvent event) { viewModel.openSalesStatistics(); }
    @FXML public void handleAnnualReport(ActionEvent event) { viewModel.openAnnualReport(); }
    @FXML public void handleDailyCashReconciliation(ActionEvent event) { viewModel.openDailyCashReconciliation(); }
    @FXML public void handleDeletedItemLogs(ActionEvent event) { viewModel.openDeletedItemLogs(); }

    // ==========================================
    // Inventory Handlers
    // ==========================================
    @FXML public void handleInventoryStatus(ActionEvent event) { viewModel.openInventoryStatus(); }
    @FXML public void handleInventoryAdjustment(ActionEvent event) { viewModel.openInventoryAdjustment(); }
    @FXML public void handlePurchaseOrdering(ActionEvent event) { viewModel.openPurchaseOrdering(); }
    @FXML public void handleGoodsUnpacking(ActionEvent event) { setCenterView(UnPackingController.class); }
    @FXML public void handleStockBySupplier(ActionEvent event) { viewModel.openStockBySupplier(); }
    @FXML public void handleSuppliersManagement(ActionEvent event) { setCenterView(SupplierController.class); }

    // ==========================================
    // Management Handlers
    // ==========================================
    @FXML public void handleStaffManagement(ActionEvent event) { viewModel.openStaffManagement(); }
    @FXML public void handleShopManagement(ActionEvent event) { viewModel.openShopManagement(); }
    @FXML public void handlePosQuickButtons(ActionEvent event) { viewModel.openPosQuickButtons(); }
    @FXML public void handleExcelTemplates(ActionEvent event) { viewModel.openExcelTemplates(); }

    // ==========================================
    // Settings Handlers
    // ==========================================
    @FXML public void handleGenerateBarcode(ActionEvent event) { setCenterView(LabelController.class); }
    @FXML public void handlePrinterSettings(ActionEvent event) { viewModel.openPrinterSettings(); }
    @FXML public void handleUiThemeSettings(ActionEvent event) { viewModel.openUiThemeSettings(); }
    @FXML public void handleVaultConnection(ActionEvent event) { viewModel.openVaultConnection(); }
    @FXML public void handleVxLinkConnection(ActionEvent event) { viewModel.openVxLinkConnection(); }
    @FXML public void handleEftposDisconnect(ActionEvent event) { viewModel.disconnectEftpos(); }
    @FXML public void handleResetNetwork(ActionEvent event) { viewModel.resetNetwork(); }
    @FXML public void handleBackupDaily(ActionEvent event) { viewModel.setBackupDaily(); }
    @FXML public void handleBackupWeekly(ActionEvent event) { viewModel.setBackupWeekly(); }
    @FXML public void handleBackupMonthly(ActionEvent event) { viewModel.setBackupMonthly(); }
    @FXML public void handleTerminalIpSetup(ActionEvent event) { viewModel.openTerminalIpSetup(); }
    @FXML public void handleBackupFilePath(ActionEvent event) { viewModel.openBackupFilePath(); }
    @FXML public void handleReceiptMessageSetup(ActionEvent event) { viewModel.openReceiptMessageSetup(); }
    @FXML public void handleEmailNotificationSettings(ActionEvent event) { viewModel.openEmailNotificationSettings(); }

    // ==========================================
    // System Handlers
    // ==========================================
    @FXML public void handleChangePassword(ActionEvent event) { viewModel.openChangePassword(); }
    @FXML public void handleDatabaseBackup(ActionEvent event) { viewModel.executeDatabaseBackup(); }
    @FXML public void handleExitApplication(ActionEvent event) { viewModel.exitApplication(); }
}