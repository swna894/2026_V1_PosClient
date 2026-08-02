package com.swna.javafx.admin;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class MenuViewModel {
    

    // Sales & Reports
    public void openDailySales() { log.info("Execute: Daily Sales"); }
    public void openSalesHistory() { log.info("Execute: Sales History"); }
    public void openProductSalesRanking() { log.info("Execute: Product Sales Ranking"); }
    public void openSalesStatistics() { log.info("Execute: Sales Statistics"); }
    public void openAnnualReport() { log.info("Execute: Annual Report"); }
    public void openDailyCashReconciliation() { log.info("Execute: Daily Cash Reconciliation"); }
    public void openDeletedItemLogs() { log.info("Execute: Deleted Item Logs"); }

    // Orders
    public void openOrdering() { log.info("Execute: Ordering"); }
    public void openUnpacking() { log.info("Execute: Unpacking Center View Opened"); }

    // Inventory
    public void openInventoryStatus() { log.info("Execute: Inventory Status"); }
    public void openInventoryAdjustment() { log.info("Execute: Inventory Adjustment"); }
    public void openPurchaseOrdering() { log.info("Execute: Purchase Ordering"); }
    public void openGoodsReceiving() { log.info("Execute: Goods Receiving"); }
    public void openStockBySupplier() { log.info("Execute: Stock by Supplier"); }
    public void openSupplierManagement() { log.info("Execute: Supplier Management"); }

    // Management
    public void openStaffManagement() { log.info("Execute: Staff Management"); }
    public void openShopManagement() { log.info("Execute: Shop Management"); }
    public void openPosQuickButtons() { log.info("Execute: POS Quick Buttons"); }
    public void openExcelTemplates() { log.info("Execute: Excel Templates"); }

    // Settings
    public void openPrinterSettings() { log.info("Execute: Printer Settings"); }
    public void openUiThemeSettings() { log.info("Execute: UI Theme Settings"); }
    public void openVaultConnection() { log.info("Execute: Vault Connection"); }
    public void openVxLinkConnection() { log.info("Execute: VxLink Connection"); }
    public void disconnectEftpos() { log.info("Execute: EFTPOS Disconnect"); }
    public void resetNetwork() { log.info("Execute: Reset Network"); }
    public void setBackupDaily() { log.info("Execute: Set Daily Backup"); }
    public void setBackupWeekly() { log.info("Execute: Set Weekly Backup"); }
    public void setBackupMonthly() { log.info("Execute: Set Monthly Backup"); }
    public void openTerminalIpSetup() { log.info("Execute: Terminal IP Setup"); }
    public void openBackupFilePath() { log.info("Execute: Backup File Path"); }
    public void openReceiptMessageSetup() { log.info("Execute: Receipt Message Setup"); }
    public void openEmailNotificationSettings() { log.info("Execute: Email Notification Settings"); }

    // System
    public void openChangePassword() { log.info("Execute: Change Password"); }
    public void executeDatabaseBackup() { log.info("Execute: Database Backup"); }
    public void exitApplication() {
        log.info("Execute: Exit Application");
        System.exit(0);
    }
}