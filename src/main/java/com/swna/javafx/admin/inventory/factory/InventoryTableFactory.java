package com.swna.javafx.admin.inventory.factory;

import static com.swna.javafx.common.tableutils.TableColumnUtils.buttonColumn;
import static com.swna.javafx.common.tableutils.TableColumnUtils.stringColumn;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.common.tableutils.TableUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Component
public class InventoryTableFactory {

    private TableView<Inventory> table;

    private TableColumn<Inventory, String> colCode;
    private TableColumn<Inventory, String> colBarcode;
    private TableColumn<Inventory, String> colDescription;
    private TableColumn<Inventory, String> colPrice;
    private TableColumn<Inventory, String> colCost;
    private TableColumn<Inventory, String> colPriceOld;
    private TableColumn<Inventory, String> colCostOld;
    private TableColumn<Inventory, String> colQuantity;
    private TableColumn<Inventory, String> colMinStock;
    private TableColumn<Inventory, String> colMinOrderQuantity;
    private TableColumn<Inventory, String> colLastOrderedAt;
    private TableColumn<Inventory, String> colComment;
    private TableColumn<Inventory, Void>   colAction;


    public TableView<Inventory> initializeTable(TableView<Inventory> existingTable) {
        this.table = existingTable;
        createColumns();
        initTableClickHandlers();
        return table;
    }

    private void createColumns() {
        TableUtil.createNumberColumn(table, "NO", 55);
        TableUtil.createCheckBoxHeaderColumn(table, Inventory::selectedProperty, "", 50);

        this.colBarcode = stringColumn(table, "BARCODE", Inventory::barcodeProperty).visible(true).alignment(TableUtil.CENTER).fixedWidth(200).build();
        this.colCode = stringColumn(table, "CODE", Inventory::codeProperty).visible(true).alignment(TableUtil.CENTER).fixedWidth(120).build();
        this.colDescription = stringColumn(table, "DESCRIPTION", Inventory::descriptionProperty).visible(true).wrapText(true).alignment(TableUtil.LEFT).fixedWidth(300).build();
        this.colPrice = stringColumn(table, "RP", item -> 
                new SimpleStringProperty(item.getPrice() != null ? item.getPrice().toString() : "0"))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colCost = stringColumn(table, "COST", item -> 
                new SimpleStringProperty(item.getCost() != null ? item.getCost().toString() : "0"))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colPriceOld = stringColumn(table, "PREVIOUS_SELLING_PRICE", item -> 
                new SimpleStringProperty(item.getPriceOld() != null ? item.getPriceOld().toString() : ""))
                .visible(false).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        

        this.colCostOld = stringColumn(table, "Old\nCost", item -> 
                new SimpleStringProperty(item.getCostOld() != null ? item.getCostOld().toString() : ""))
                .visible(false).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colQuantity = stringColumn(table, "STOCK", item -> 
                new SimpleStringProperty(String.valueOf(item.getQuantity())))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();
        
        this.colMinStock = stringColumn(table, "MIN\nSTOCK", item -> 
                new SimpleStringProperty(String.valueOf(item.getMinStock())))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();
        
        this.colMinOrderQuantity = stringColumn(table, "MIN QTY", item -> 
                new SimpleStringProperty(String.valueOf(item.getMinOrderQuantity())))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();

        this.colLastOrderedAt = stringColumn(table, "LAST\nORDERED", item -> 
                new SimpleStringProperty(item.getLastOrderedAtFormatted()))
                .visible(true).alignment(TableUtil.CENTER).fixedWidth(140).build();

        this.colAction = buttonColumn(table)
                .title("action").visible(true).fixedWidth(100)
                .bgColor("gray").textColor("green")
                .action(this::onClickAction).build();

        this.colComment = stringColumn(table, "COMMENT", Inventory::commentProperty).visible(true).alignment(TableUtil.LEFT).fixedWidth(250).build();
    }

    private void onClickAction(Inventory item, ActionEvent event) {
        log.info("Action clicked for inventory code: {}", item.getCode());
        // 필요 시 stageService 등을 통해 상세/수정 모달 팝업 호출
    }

    // =========================================================================
    // 🌟 테이블 뷰 컬럼 클릭 이벤트 핸들러
    // =========================================================================
    private void initTableClickHandlers() {
        table.setOnMouseClicked(event -> {
            Inventory selectedItem = table.getSelectionModel().getSelectedItem();
            if (selectedItem == null) return;

            var posList = table.getSelectionModel().getSelectedCells();
            if (posList.isEmpty()) return;

            TableColumn<?, ?> col = posList.get(0).getTableColumn();
            if (col == null || col.getText() == null) return;
        });
    }

    // private void handleDescriptionClick(Inventory item) {
    //     item.selectedProperty().set(true);
    //     String description = item.getDescription();

    //     if (description != null && !description.isBlank()) {
    //         AlertUtil.showInform("상품 설명", description);
    //     }
    // }

    // private void resetColumnsVisibility() {
    //     if ("guest".equalsIgnoreCase(userContext.getUser().getName())) {
    //         SecurityUtils.setColumnsVisibility(false, colPriceOld, colCostOld);
    //     } else {
    //         // 기타 권한 처리 logic
    //     }
    // }
}