package com.swna.javafx.pos.manager;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.pos.domain.PosItem;
import com.swna.javafx.pos.viewmodel.PosViewModel;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.springframework.stereotype.Component;
import java.util.function.Consumer;

@Component
public class PosTableSetup {

    private static final int BUTTON_COLUMN_WIDTH = 50;
    private static final String STYLE_BASE_CELL = "-fx-background-color: transparent; -fx-alignment: CENTER;";
    private static final String STYLE_LOW_STOCK = "-fx-text-fill: red; -fx-font-weight: bold;";
    private static final String STYLE_NORMAL_STOCK = "-fx-text-fill: black;";

    public void setup(TableView<PosItem> table,
                      PosViewModel viewModel,
                      TableColumn<?, ?> colNo,
                      TableColumn<PosItem, String> colBarcode,
                      TableColumn<PosItem, String> colDesc,
                      TableColumn<PosItem, String> colComment,
                      TableColumn<PosItem, Integer> colQty,
                      TableColumn<PosItem, Integer> colStock,
                      TableColumn<PosItem, Double> colPrice,
                      TableColumn<PosItem, Double> colTotal,
                      TableColumn<PosItem, Double> colDiscount,
                      TableColumn<PosItem, Void> colDelete,
                      TableColumn<PosItem, Void> colMinus,
                      TableColumn<PosItem, Void> colPlus,
                      TableColumn<PosItem, Void> colDiscountPrice,
                      TableColumn<PosItem, Void> colChangePrice,
                      Consumer<PosItem> onDiscount,
                      Consumer<PosItem> onChangePrice) {

        table.setEditable(true);
        table.setItems(viewModel.getPosItems());

        // 번호 컬럼
        TableColumnUtil.createNumberColumn(table, (TableColumn<PosItem, String>) colNo, 70);

        // 액션 버튼

        TableColumnUtil.makeButtonColumn(colDelete, null, IconPaths.DELETE, BUTTON_COLUMN_WIDTH, viewModel::removeItem);
        TableColumnUtil.makeButtonColumn(colMinus, null, IconPaths.MINUS, BUTTON_COLUMN_WIDTH, viewModel::decreaseQty);
        TableColumnUtil.makeButtonColumn(colPlus, null, IconPaths.PLUS, BUTTON_COLUMN_WIDTH, viewModel::increaseQty);
        TableColumnUtil.makeButtonColumn(colDiscountPrice, null, IconPaths.DISCOUNT, BUTTON_COLUMN_WIDTH, onDiscount);
        TableColumnUtil.makeButtonColumn(colChangePrice, null, IconPaths.PRICE_22, BUTTON_COLUMN_WIDTH, onChangePrice);

        // 데이터 컬럼
        TableColumnUtil.makeStringColumn(colBarcode, PosItem::barcodeProperty, PosItem::setBarcode, false, "CENTER", null);
        TableColumnUtil.makeStringColumn(colDesc, PosItem::descriptionProperty, PosItem::setDescription, false, "LEFT", null);
        TableColumnUtil.makeStringColumn(colComment, PosItem::commentProperty, PosItem::setComment, false, "LEFT", null);
        TableColumnUtil.makeIntegerColumn(colQty, PosItem::qtyProperty, PosItem::setQty, true, "CENTER", null);
        TableColumnUtil.makeIntegerColumn(colStock, PosItem::stockProperty, PosItem::setStock, false, "CENTER", null);
        TableColumnUtil.makeCurrencyColumn(colPrice, PosItem::sellingPriceProperty, false, "RIGHT", null);
        TableColumnUtil.makeCurrencyColumn(colTotal, PosItem::finalAmountProperty, false, "RIGHT", null);
        TableColumnUtil.makeCurrencyColumn(colDiscount, PosItem::discountTotalProperty, false, "RIGHT", null);

        // 재고 스타일
        setupStockCellStyle(colStock);

        // 선택 동기화
        setupSelectionSync(table, viewModel);
    }

    private void setupStockCellStyle(TableColumn<PosItem, Integer> colStock) {
        colStock.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(value.toString());
                setStyle(STYLE_BASE_CELL + (value < 0 ? STYLE_LOW_STOCK : STYLE_NORMAL_STOCK));
            }
        });
    }

    private void setupSelectionSync(TableView<PosItem> table, PosViewModel viewModel) {
        viewModel.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                table.getSelectionModel().select(newVal);
                table.scrollTo(newVal);
            } else {
                table.getSelectionModel().clearSelection();
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) ->
            viewModel.selectedItemProperty().set(newVal)
        );
    }
}
