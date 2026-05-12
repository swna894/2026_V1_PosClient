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

    // ========== Layout Constants ==========
    private static final int BUTTON_COLUMN_WIDTH = 50;
    private static final int NUMBER_COLUMN_WIDTH = 70;
    
    // ========== Alignment Constants ==========
    private static final String ALIGN_CENTER = "CENTER";
    private static final String ALIGN_LEFT = "LEFT";
    private static final String ALIGN_RIGHT = "RIGHT";
    
    // ========== Style Constants ==========
    private static final String STYLE_BASE_CELL = "-fx-background-color: transparent; -fx-alignment: CENTER;";
    private static final String STYLE_LOW_STOCK = "-fx-text-fill: red; -fx-font-weight: bold;";
    private static final String STYLE_NORMAL_STOCK = "-fx-text-fill: black;";
    
    // ========== Editability Constants ==========
    private static final boolean EDITABLE = true;
    private static final boolean READ_ONLY = false;

    // ========== Inner Class for Column Holder ==========
    
    /**
     * 테이블 컬럼 홀더 (매개변수 개수 문제 해결)
     */
    public static class TableColumns {
        public final TableColumn<?, ?> colNo;
        public final TableColumn<PosItem, String> colBarcode;
        public final TableColumn<PosItem, String> colDesc;
        public final TableColumn<PosItem, String> colComment;
        public final TableColumn<PosItem, Integer> colQty;
        public final TableColumn<PosItem, Integer> colStock;
        public final TableColumn<PosItem, Double> colPrice;
        public final TableColumn<PosItem, Double> colTotal;
        public final TableColumn<PosItem, Double> colDiscount;
        public final TableColumn<PosItem, Void> colDelete;
        public final TableColumn<PosItem, Void> colMinus;
        public final TableColumn<PosItem, Void> colPlus;
        public final TableColumn<PosItem, Void> colDiscountPrice;
        public final TableColumn<PosItem, Void> colChangePrice;
        
        private TableColumns(Builder builder) {
            this.colNo = builder.colNo;
            this.colBarcode = builder.colBarcode;
            this.colDesc = builder.colDesc;
            this.colComment = builder.colComment;
            this.colQty = builder.colQty;
            this.colStock = builder.colStock;
            this.colPrice = builder.colPrice;
            this.colTotal = builder.colTotal;
            this.colDiscount = builder.colDiscount;
            this.colDelete = builder.colDelete;
            this.colMinus = builder.colMinus;
            this.colPlus = builder.colPlus;
            this.colDiscountPrice = builder.colDiscountPrice;
            this.colChangePrice = builder.colChangePrice;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private TableColumn<?, ?> colNo;
            private TableColumn<PosItem, String> colBarcode;
            private TableColumn<PosItem, String> colDesc;
            private TableColumn<PosItem, String> colComment;
            private TableColumn<PosItem, Integer> colQty;
            private TableColumn<PosItem, Integer> colStock;
            private TableColumn<PosItem, Double> colPrice;
            private TableColumn<PosItem, Double> colTotal;
            private TableColumn<PosItem, Double> colDiscount;
            private TableColumn<PosItem, Void> colDelete;
            private TableColumn<PosItem, Void> colMinus;
            private TableColumn<PosItem, Void> colPlus;
            private TableColumn<PosItem, Void> colDiscountPrice;
            private TableColumn<PosItem, Void> colChangePrice;
            
            public Builder colNo(TableColumn<?, ?> colNo) { this.colNo = colNo; return this; }
            public Builder colBarcode(TableColumn<PosItem, String> colBarcode) { this.colBarcode = colBarcode; return this; }
            public Builder colDesc(TableColumn<PosItem, String> colDesc) { this.colDesc = colDesc; return this; }
            public Builder colComment(TableColumn<PosItem, String> colComment) { this.colComment = colComment; return this; }
            public Builder colQty(TableColumn<PosItem, Integer> colQty) { this.colQty = colQty; return this; }
            public Builder colStock(TableColumn<PosItem, Integer> colStock) { this.colStock = colStock; return this; }
            public Builder colPrice(TableColumn<PosItem, Double> colPrice) { this.colPrice = colPrice; return this; }
            public Builder colTotal(TableColumn<PosItem, Double> colTotal) { this.colTotal = colTotal; return this; }
            public Builder colDiscount(TableColumn<PosItem, Double> colDiscount) { this.colDiscount = colDiscount; return this; }
            public Builder colDelete(TableColumn<PosItem, Void> colDelete) { this.colDelete = colDelete; return this; }
            public Builder colMinus(TableColumn<PosItem, Void> colMinus) { this.colMinus = colMinus; return this; }
            public Builder colPlus(TableColumn<PosItem, Void> colPlus) { this.colPlus = colPlus; return this; }
            public Builder colDiscountPrice(TableColumn<PosItem, Void> colDiscountPrice) { this.colDiscountPrice = colDiscountPrice; return this; }
            public Builder colChangePrice(TableColumn<PosItem, Void> colChangePrice) { this.colChangePrice = colChangePrice; return this; }
            
            public TableColumns build() {
                return new TableColumns(this);
            }
        }
    }

    // ========== Inner Class for Callbacks ==========
    
    /**
     * 콜백 홀더 (매개변수 개수 문제 해결)
     */
    public static class Callbacks {
        public final Consumer<PosItem> onDiscount;
        public final Consumer<PosItem> onChangePrice;
        
        private Callbacks(Builder builder) {
            this.onDiscount = builder.onDiscount;
            this.onChangePrice = builder.onChangePrice;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        public static class Builder {
            private Consumer<PosItem> onDiscount;
            private Consumer<PosItem> onChangePrice;
            
            public Builder onDiscount(Consumer<PosItem> onDiscount) { this.onDiscount = onDiscount; return this; }
            public Builder onChangePrice(Consumer<PosItem> onChangePrice) { this.onChangePrice = onChangePrice; return this; }
            
            public Callbacks build() {
                return new Callbacks(this);
            }
        }
    }

    // ========== Main Setup Method ==========
    
    public void setup(TableView<PosItem> table,
                      PosViewModel viewModel,
                      TableColumns columns,
                      Callbacks callbacks) {
        
        table.setEditable(true);
        table.setItems(viewModel.getPosItems());

        // 번호 컬럼
        setupNumberColumn(table, columns.colNo);
        
        // 액션 버튼 컬럼
        setupActionButtons(columns, viewModel, callbacks);
        
        // 데이터 컬럼 (상수 사용)
        setupDataColumns(columns);
        
        // 재고 스타일
        setupStockCellStyle(columns.colStock);
        
        // 선택 동기화
        setupSelectionSync(table, viewModel);
    }
    
    // ========== Private Setup Methods ==========
    
    /**
     * 번호 컬럼 설정
     */
    @SuppressWarnings("unchecked")
    private void setupNumberColumn(TableView<PosItem> table, TableColumn<?, ?> colNo) {
        TableColumn<PosItem, String> typedColumn = (TableColumn<PosItem, String>) colNo;
        TableColumnUtil.createNumberColumn(table, typedColumn, NUMBER_COLUMN_WIDTH);
    }
    
    /**
     * 액션 버튼 컬럼 설정
     */
    private void setupActionButtons(TableColumns columns, PosViewModel viewModel, Callbacks callbacks) {
        TableColumnUtil.makeButtonColumn(columns.colDelete, null, IconPaths.DELETE, BUTTON_COLUMN_WIDTH, viewModel::removeItem);
        TableColumnUtil.makeButtonColumn(columns.colMinus, null, IconPaths.MINUS, BUTTON_COLUMN_WIDTH, viewModel::decreaseQty);
        TableColumnUtil.makeButtonColumn(columns.colPlus, null, IconPaths.PLUS, BUTTON_COLUMN_WIDTH, viewModel::increaseQty);
        TableColumnUtil.makeButtonColumn(columns.colDiscountPrice, null, IconPaths.DISCOUNT, BUTTON_COLUMN_WIDTH, callbacks.onDiscount);
        TableColumnUtil.makeButtonColumn(columns.colChangePrice, null, IconPaths.PRICE_22, BUTTON_COLUMN_WIDTH, callbacks.onChangePrice);
    }
    
    /**
     * 데이터 컬럼 설정 (상수 사용으로 중복 제거)
     */
    private void setupDataColumns(TableColumns columns) {
        // 바코드 - 중앙 정렬, 읽기 전용
        TableColumnUtil.makeStringColumn(columns.colBarcode, PosItem::barcodeProperty, PosItem::setBarcode, READ_ONLY, ALIGN_CENTER, null);
        
        // 상품명 - 왼쪽 정렬, 읽기 전용
        TableColumnUtil.makeStringColumn(columns.colDesc, PosItem::descriptionProperty, PosItem::setDescription, READ_ONLY, ALIGN_LEFT, null);
        
        // 코멘트 - 왼쪽 정렬, 읽기 전용
        TableColumnUtil.makeStringColumn(columns.colComment, PosItem::commentProperty, PosItem::setComment, READ_ONLY, ALIGN_LEFT, null);
        
        // 수량 - 중앙 정렬, 편집 가능
        TableColumnUtil.makeIntegerColumn(columns.colQty,  PosItem::qtyProperty, PosItem::setQty, EDITABLE, ALIGN_CENTER, null);
        
        // 재고 - 중앙 정렬, 읽기 전용
        TableColumnUtil.makeIntegerColumn(columns.colStock, PosItem::stockProperty, PosItem::setStock, READ_ONLY, ALIGN_CENTER, null);
        
        // 단가 - 오른쪽 정렬, 읽기 전용
        TableColumnUtil.makeCurrencyColumn(columns.colPrice, 
            PosItem::sellingPriceProperty, READ_ONLY, ALIGN_RIGHT, null);
        
        // 총액 - 오른쪽 정렬, 읽기 전용
        TableColumnUtil.makeCurrencyColumn(columns.colTotal, 
            PosItem::finalAmountProperty, READ_ONLY, ALIGN_RIGHT, null);
        
        // 할인 - 오른쪽 정렬, 읽기 전용
        TableColumnUtil.makeCurrencyColumn(columns.colDiscount, 
            PosItem::discountTotalProperty, READ_ONLY, ALIGN_RIGHT, null);
    }
    
    /**
     * 재고 스타일 설정
     */
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
    
    /**
     * 선택 동기화 설정
     */
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