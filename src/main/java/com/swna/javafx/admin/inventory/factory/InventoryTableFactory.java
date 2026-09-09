package com.swna.javafx.admin.inventory.factory;

import static com.swna.javafx.common.tableutils.TableColumnUtils.buttonColumn;
import static com.swna.javafx.common.tableutils.TableColumnUtils.stringColumn;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.swna.javafx.admin.inventory.model.Inventory;
import com.swna.javafx.admin.inventory.viewmodel.InventoryViewModel;
import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.tableutils.TableUtil;

import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryTableFactory {

    private final InventoryViewModel viewModel;

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
    private TableColumn<Inventory, Void>   colButtonSave;
    private TableColumn<Inventory, Void>   colButtonOrderHistory;


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
        this.colDescription = stringColumn(table, "DESCRIPTION", Inventory::descriptionProperty).visible(true).wrapText(true).alignment(TableUtil.LEFT).fixedWidth(350).build();
        this.colPrice = stringColumn(table, "RETAIL\nPRICE", item -> 
                new SimpleStringProperty(item.getPrice() != null ? item.getPrice().toString() : "0"))
                .setter((item, newVal) -> item.setPrice(new BigDecimal(newVal)))
                .dirtyConsumer(item -> item.setSelected(true))
                .editable(true).visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colCost = stringColumn(table, "COST", item -> 
                new SimpleStringProperty(item.getCost() != null ? item.getCost().toString() : "0"))
                .setter((item, newVal) -> item.setCost(new BigDecimal(newVal)))
                .dirtyConsumer(item -> item.setSelected(true))
                .editable(true).visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colPriceOld = stringColumn(table, "OLD\nPRICE", item -> 
                new SimpleStringProperty(item.getPriceOld() != null ? item.getPriceOld().toString() : ""))
                .setter((item, newVal) -> item.setPrice(new BigDecimal(newVal)))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        

        this.colCostOld = stringColumn(table, "OLD\nCOST", item -> 
                new SimpleStringProperty(item.getCostOld() != null ? item.getCostOld().toString() : ""))
                .visible(true).alignment(TableUtil.RIGHT).fixedWidth(90).build();
        
        this.colQuantity = stringColumn(table, "STOCK", item -> 
                new SimpleStringProperty(String.valueOf(item.getQuantity())))
                .setter((item, newVal) -> item.setQuantity(Integer.parseInt(newVal)))
                .dirtyConsumer(item -> item.setSelected(true))
                .editable(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();
        
        this.colMinStock = stringColumn(table, "MIN\nSTOCK", item -> 
                new SimpleStringProperty(String.valueOf(item.getMinStock())))
                .setter((item, newVal) -> item.setMinStock(Integer.parseInt(newVal)))
                .dirtyConsumer(item -> item.setSelected(true))
                .editable(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();
        
        this.colMinOrderQuantity = stringColumn(table, "MIN QTY", item -> 
                new SimpleStringProperty(String.valueOf(item.getMinOrderQuantity())))
                 .setter((item, newVal) -> item.setMinOrderQuantity(Integer.parseInt(newVal)))
                 .dirtyConsumer(item -> item.setSelected(true))
                .editable(true).alignment(TableUtil.RIGHT).fixedWidth(100).build();

        this.colLastOrderedAt = stringColumn(table, "LAST\nORDERED", item -> 
                new SimpleStringProperty(item.getLastOrderedAtFormatted()))
                .visible(true).alignment(TableUtil.CENTER).fixedWidth(140).build();

        this.colButtonOrderHistory = buttonColumn(table).title("").iconPath(IconPaths.BARGRAPH_32).width(IconPaths.BUTTOM_WIDTH).build();
        this.colButtonSave = buttonColumn(table).title("").iconPath(IconPaths.SAVE).width(IconPaths.BUTTOM_WIDTH).action(viewModel::updateProduct).build();

        this.colComment = stringColumn(table, "COMMENT", Inventory::commentProperty).visible(true).alignment(TableUtil.LEFT).fixedWidth(250).build();
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