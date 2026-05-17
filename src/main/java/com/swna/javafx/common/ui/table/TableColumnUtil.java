package com.swna.javafx.common.ui.table;

import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.UnaryOperator;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.converter.BigDecimalStringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableColumnUtil {

    // ========== Alignment Constants ==========
    public static final String CENTER = "CENTER";
    public static final String RIGHT = "RIGHT";
    public static final String LEFT = "LEFT";

    // ========== CSS Style Constants ==========
    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";
    private static final String BUTTON_STYLE_DEFAULT = "-fx-background-color:transparent; -fx-alignment: center;";
    private static final String BUTTON_STYLE_HOVER = "-fx-background-color:#6F4CBB;";


    // ========== Public API - Number Column ==========
    
    /**
     * 테이블 행 번호를 표시하는 컬럼을 생성합니다.
     * 각 행의 순번(1부터 시작)을 자동으로 표시하며, 정렬 및 편집이 불가능합니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param tableView 컬럼을 추가할 TableView
     * @param column    생성할 TableColumn (미리 생성된 객체)
     * @param width     컬럼의 고정 너비 (0일 경우 기본값 사용)
     * @return 설정이 완료된 TableColumn
     */
    public static <T> TableColumn<T, String> createNumberColumn(TableView<T> tableView, TableColumn<T, String> column, int width) {
        if (width != 0) column.setPrefWidth(width);
        column.setText("NO");
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(" " + (tableView.getItems().indexOf(p.getValue()) + 1) + " "));
        return column;
    }

    // ========== Public API - String Column ==========
    
    /**
     * 문자열 값을 표시하는 컬럼을 생성합니다.
     * StringProperty에 바인딩되며, 편집 가능 여부와 정렬 방식을 설정할 수 있습니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn
     * @param propertyGetter 모델에서 StringProperty를 가져오는 함수
     * @param setter        편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable      편집 가능 여부
     * @param alignment     텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeStringColumn(
            TableColumn<T, String> column,
            Function<T, StringProperty> propertyGetter,
            BiConsumer<T, String> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setSortable(false);

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn());
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Currency Column ==========
    
    /**
     * 통화(Currency) 형식으로 값을 표시하는 컬럼을 생성합니다.
     * 시스템 기본 로케일에 맞춰 통화 형식(예: ₩1,234.00)으로 표시됩니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn (Double 타입)
     * @param propertyGetter 모델에서 DoubleProperty를 가져오는 함수
     * @param editable      편집 가능 여부
     * @param alignment     텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeCurrencyColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(tc -> new CurrencyCell<>());

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                DoubleProperty prop = propertyGetter.apply(row);
                prop.set(event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Integer Column ==========
    
    /**
     * 정수(Integer) 값을 표시하는 컬럼을 생성합니다.
     * 숫자만 입력 가능한 편집 셀을 제공하며, 음수 입력도 지원합니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param column    설정할 TableColumn (Integer 타입)
     * @param propertyGetter 모델에서 IntegerProperty를 가져오는 함수
     * @param setter    편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable  편집 가능 여부
     * @param alignment 텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeIntegerColumn(
            TableColumn<T, Integer> column,
            Function<T, IntegerProperty> propertyGetter,
            ObjIntConsumer<T> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(col -> new IntegerEditingCell<>(alignment));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                if (event.getNewValue() != null) {
                    setter.accept(row, event.getNewValue());
                }
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - Long Column ==========
    
    /**
     * 읽기 전용 Long 값을 표시하는 컬럼을 생성합니다.
     * 편집이 불가능하며, 단순히 값을 표시하는 용도로 사용됩니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn (Long 타입)
     * @param propertyGetter 모델에서 LongProperty를 가져오는 함수
     * @param alignment     텍스트 정렬 (CENTER, RIGHT, LEFT)
     */
    public static <T> void makeReadOnlyLongColumn(TableColumn<T, Long> column, Function<T, LongProperty> propertyGetter, String alignment) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setEditable(false);
    }

    // ========== Public API - Double Column ==========
    
    /**
     * 실수(Double) 값을 표시하는 컬럼을 생성합니다.
     * 소수점을 포함한 숫자 값을 표시하며, 편집 시 기본 DoubleStringConverter를 사용합니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn (Double 타입)
     * @param propertyGetter 모델에서 DoubleProperty를 가져오는 함수
     * @param setter        편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable      편집 가능 여부
     * @param alignment     텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeDoubleColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            ObjDoubleConsumer<T> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    // ========== Public API - DateTime Column ==========
    
    /**
     * 날짜/시간(LocalDateTime) 값을 표시하는 컬럼을 생성합니다.
     * 기본 toString() 형식으로 표시되며, 편집 가능 여부를 설정할 수 있습니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param column    설정할 TableColumn (LocalDateTime 타입)
     * @param getter    모델에서 LocalDateTime 값을 가져오는 함수
     * @param setter    편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable  편집 가능 여부
     * @param alignment 텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DateTimeCell<>());

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        }
    }

    // ========== Public API - DateTime Column with Format ==========

    /**
     * 날짜/시간(LocalDateTime) 값을 지정된 형식으로 표시하는 컬럼을 생성합니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param column    설정할 TableColumn (LocalDateTime 타입)
     * @param getter    모델에서 LocalDateTime 값을 가져오는 함수
     * @param setter    편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable  편집 가능 여부
     * @param alignment 텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param formatter 날짜/시간 포맷터 (예: DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            String alignment,
            DateTimeFormatter formatter,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new FormattedDateTimeCell<>(formatter));

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        }
    }

    /**
     * 형식화된 날짜/시간 표시 셀 내부 클래스
     */
    private static class FormattedDateTimeCell<T> extends TableCell<T, LocalDateTime> {
        private final DateTimeFormatter formatter;
        
        public FormattedDateTimeCell(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }
        
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.format(formatter));
            }
        }
    }
    
    // ========== Public API - DatePicker Column ==========
    
    /**
     * DatePicker 팝업을 통해 날짜를 선택할 수 있는 컬럼을 생성합니다.
     * 셀을 클릭하면 DatePicker가 표시되어 날짜를 선택할 수 있습니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param column    설정할 TableColumn (LocalDateTime 타입)
     * @param getter    모델에서 ObjectProperty&lt;LocalDateTime&gt;를 가져오는 함수
     * @param setter    편집 시 값을 저장하는 함수 (row, newValue)
     * @param alignment 텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeDatePickerColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<T, LocalDateTime> setter,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> getter.apply(cell.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DatePickerTableCell<>(setter, dirtyConsumer));
        column.setEditable(true);
    }


    /**
     * BigDecimal 값을 통화 형식으로 표시하는 컬럼을 생성합니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn (BigDecimal 타입)
     * @param propertyGetter 모델에서 ObjectProperty&lt;BigDecimal&gt;를 가져오는 함수
     * @param editable      편집 가능 여부
     * @param alignment     텍스트 정렬 (CENTER, RIGHT, LEFT)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeBigDecimalCurrencyColumn(
            TableColumn<T, BigDecimal> column,
            Function<T, ObjectProperty<BigDecimal>> propertyGetter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(tc -> new BigDecimalCurrencyCell<>());

        if (editable) {
            column.setCellFactory(TextFieldTableCell.forTableColumn(new BigDecimalStringConverter()));
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                ObjectProperty<BigDecimal> prop = propertyGetter.apply(row);
                prop.set(event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    /**
     * BigDecimal 통화 표시 셀 내부 클래스
     */
    private static class BigDecimalCurrencyCell<T> extends TableCell<T, BigDecimal> {
        private final NumberFormat currencyFormat;
        
        public BigDecimalCurrencyCell() {
            this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setMinimumFractionDigits(2);
            currencyFormat.setMaximumFractionDigits(2);
        }
        
        @Override
        protected void updateItem(BigDecimal price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
            } else if (price.compareTo(BigDecimal.ZERO) == 0) {
                setText("-"); 
            } else {
                setText(currencyFormat.format(price));
            }
        }
    }
    // ========== Public API - Boolean Column ==========
    
    /**
     * 불리언(Boolean) 값을 체크박스로 표시하는 컬럼을 생성합니다.
     * 체크박스를 클릭하여 값을 변경할 수 있으며, 변경 시 dirtyConsumer가 호출됩니다.
     *
     * @param <T>           테이블 뷰의 모델 타입
     * @param column        설정할 TableColumn (Boolean 타입)
     * @param propertyGetter 모델에서 BooleanProperty를 가져오는 함수
     * @param setter        편집 시 값을 저장하는 함수 (row, newValue)
     * @param editable      편집 가능 여부 (체크박스 클릭 가능 여부)
     * @param dirtyConsumer 변경 사항 발생 시 호출될 콜백 (nullable)
     */
    public static <T> void makeBooleanColumn(
            TableColumn<T, Boolean> column,
            Function<T, BooleanProperty> propertyGetter,
            BiConsumer<T, Boolean> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> {
            BooleanProperty prop = propertyGetter.apply(cellData.getValue());
            if (editable) {
                prop.addListener((obs, oldVal, newVal) -> {
                    setter.accept(cellData.getValue(), newVal);
                    if (dirtyConsumer != null) dirtyConsumer.accept(cellData.getValue());
                });
            }
            return prop;
        });
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setEditable(editable);
    }

    // ========== Public API - Button Column ==========
    
    /**
     * 버튼이 포함된 액션 컬럼을 생성합니다.
     * 각 행에 버튼을 표시하며, 버튼 클릭 시 지정된 액션을 실행합니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param column    설정할 TableColumn (Void 타입)
     * @param title     컬럼 헤더에 표시될 제목 (nullable)
     * @param iconPath  버튼에 표시될 아이콘 이미지 경로 (nullable)
     * @param width     컬럼의 고정 너비 (nullable)
     * @param action    버튼 클릭 시 실행될 액션 (현재 행의 데이터 전달)
     */
    public static <T> void makeButtonColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            Consumer<T> action
    ) {
        setupStaticColumnProps(column, title, iconPath, width);
        column.setCellFactory(param -> new ButtonCell<>(iconPath, action));
    }

    // ========== Public API - Label Column ==========
    
    /**
     * 클릭 가능한 라벨이 포함된 액션 컬럼을 생성합니다.
     * 각 행에 라벨을 표시하며, 라벨 클릭 시 지정된 마우스 이벤트를 실행합니다.
     *
     * @param <T>         테이블 뷰의 모델 타입
     * @param column      설정할 TableColumn (Void 타입)
     * @param title       컬럼 헤더에 표시될 제목 (nullable)
     * @param iconPath    라벨에 표시될 아이콘 이미지 경로 (nullable)
     * @param width       컬럼의 고정 너비 (nullable)
     * @param actionEvent 라벨 클릭 시 실행될 마우스 이벤트 핸들러
     */
    public static <T> void makeLabelColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {
        setupStaticColumnProps(column, title, iconPath, width);
        column.setCellFactory(param -> new LabelCell<>(iconPath, actionEvent));
    }

    // ========== Public API - CheckBox Header Column ==========
    
    /**
     * 헤더에 전체 선택/해제용 체크박스가 있는 컬럼을 생성합니다.
     * 헤더의 체크박스를 통해 테이블의 모든 행을 일괄 선택하거나 해제할 수 있습니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param tableView 컬럼을 추가할 TableView
     * @param property  체크박스 상태에 매핑될 BooleanProperty를 가져오는 함수
     * @return 생성되어 TableView에 추가된 TableColumn
     */
    public static <T> TableColumn<T, Boolean> createCheckBoxHeaderColumn(
            TableView<T> tableView, 
            Function<T, BooleanProperty> property
    ) {
        CheckBox headerCheckBox = createHeaderCheckBox(tableView, property);
        TableColumn<T, Boolean> column = new TableColumn<>();
        column.setGraphic(headerCheckBox);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setSortable(false);
        column.setPrefWidth(50);
        column.setCellValueFactory(cellData -> property.apply(cellData.getValue()));
        column.setCellFactory(CheckBoxTableCell.forTableColumn(column));
        tableView.getColumns().add(column);
        return column;
    }

    /**
     * 헤더에 전체 선택/해제용 체크박스가 있는 컬럼을 생성합니다.
     * 컬럼 제목과 너비를 추가로 지정할 수 있습니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param tableView 컬럼을 추가할 TableView
     * @param property  체크박스 상태에 매핑될 BooleanProperty를 가져오는 함수
     * @param title     컬럼 헤더에 표시될 제목 (nullable)
     * @param width     컬럼의 고정 너비 (0일 경우 기본값 50 사용)
     * @return 생성되어 TableView에 추가된 TableColumn
     */
    public static <T> TableColumn<T, Boolean> createCheckBoxHeaderColumn(
            TableView<T> tableView,
            TableColumn<T, Boolean> column,
            Function<T, BooleanProperty> property,
            String title,
            int width
    ) {
        CheckBox headerCheckBox = createHeaderCheckBox(tableView, property);
        configureColumn(column, headerCheckBox, title, width);
        column.setCellValueFactory(cellData -> property.apply(cellData.getValue()));
        column.setCellFactory(tc -> createCheckBoxCell(tableView, headerCheckBox, property));
        column.setEditable(true);
        
        return column;
    }

    private static <T> void configureColumn(
            TableColumn<T, Boolean> column,
            CheckBox headerCheckBox,
            String title,
            int width
    ) {
        column.setGraphic(headerCheckBox);
        if (title != null) column.setText(title);
        column.setStyle(STYLE_CENTER);
        column.setSortable(false);
        column.setPrefWidth(width > 0 ? width : 50);
    }

    private static <T> TableCell<T, Boolean> createCheckBoxCell(
            TableView<T> tableView,
            CheckBox headerCheckBox,
            Function<T, BooleanProperty> property
    ) {
        return new TableCell<T, Boolean>() {
            private final CheckBox checkBox = createCheckBox();
            private final HBox container = createContainer(checkBox);

            {
                setupCheckBoxListener(tableView, headerCheckBox, property);
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                updateCellContent(empty);
            }
            
            private void updateCellContent(boolean empty) {
                if (isInvalidCell(empty)) {
                    setGraphic(null);
                    return;
                }
                
                T rowItem = getTableRow().getItem();
                BooleanProperty prop = property.apply(rowItem);
                
                if (prop != null) {
                    checkBox.setSelected(prop.get());
                    setGraphic(container);
                    setAlignment(Pos.CENTER);
                } else {
                    setGraphic(null);
                }
            }
            
            private boolean isInvalidCell(boolean empty) {
                return empty || getTableRow() == null || getTableRow().getItem() == null;
            }
            
            private void setupCheckBoxListener(
                    TableView<T> tableView,
                    CheckBox headerCheckBox,
                    Function<T, BooleanProperty> property
            ) {
                checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                    T item = getTableRow().getItem();
                    if (item == null) return;
                    
                    BooleanProperty prop = property.apply(item);
                    if (prop == null || prop.get() == isSelected) return;
                    
                    prop.set(isSelected);
                    updateHeaderCheckBoxState(tableView, headerCheckBox, property);
                });
            }
        };
    }

    private static CheckBox createCheckBox() {
        CheckBox checkBox = new CheckBox();
        checkBox.setStyle(STYLE_TRANSPARENT);
        return checkBox;
    }

    private static HBox createContainer(CheckBox checkBox) {
        HBox container = new HBox(checkBox);
        container.setAlignment(Pos.CENTER);
        container.setPadding(new Insets(0, 0, 0, 6));
        return container;
    }

    // ========== Private Helper Methods for CheckBox Header ==========
    
    /**
     * 헤더에 표시될 전체 선택/해제용 체크박스를 생성합니다.
     * 헤더 체크박스의 상태는 모든 행의 체크박스 상태에 따라 자동으로 업데이트되며,
     * 헤더 체크박스를 클릭하면 모든 행의 체크박스 상태가 일괄 변경됩니다.
     *
     * @param <T>       테이블 뷰의 모델 타입
     * @param tableView 체크박스가 속한 TableView
     * @param property  체크박스 상태에 매핑될 BooleanProperty를 가져오는 함수
     * @return 설정이 완료된 헤더용 CheckBox
     */
    private static <T> CheckBox createHeaderCheckBox(TableView<T> tableView, Function<T, BooleanProperty> property) {
        CheckBox headerCheckBox = new CheckBox();
        headerCheckBox.setStyle(STYLE_TRANSPARENT);
        
        // 모든 행의 선택 상태를 감시하여 헤더 체크박스 상태 업데이트
        headerCheckBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for (T item : tableView.getItems()) {
                BooleanProperty prop = property.apply(item);
                if (prop != null) {
                    prop.set(newVal);
                }
            }
        });
        
        // 테이블 데이터 변경 시 헤더 체크박스 상태 갱신
        tableView.itemsProperty().addListener((obs, oldList, newList) -> {
            updateHeaderCheckBoxState(tableView, headerCheckBox, property);
        });
        
        // 각 행의 체크박스 상태 변경을 감시하여 헤더 체크박스 상태 갱신
        tableView.getItems().addListener((ListChangeListener<T>) c -> {
            updateHeaderCheckBoxState(tableView, headerCheckBox, property);
            while (c.next()) {
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    updateHeaderCheckBoxState(tableView, headerCheckBox, property);
                    break;
                }
            }
        });
        
        return headerCheckBox;
    }
    
    /**
     * 모든 행의 체크 상태를 확인하여 헤더 체크박스의 상태를 업데이트합니다.
     * 모든 행이 선택됨 → 헤더 체크박스 선택됨
     * 일부 행만 선택됨 → 헤더 체크박스 INDETERMINATE 상태
     * 모든 행이 선택 해제됨 → 헤더 체크박스 선택 해제됨
     *
     * @param <T>            테이블 뷰의 모델 타입
     * @param tableView      대상 TableView
     * @param headerCheckBox 업데이트할 헤더 체크박스
     * @param property       체크박스 상태에 매핑될 BooleanProperty를 가져오는 함수
     */
    private static <T> void updateHeaderCheckBoxState(
            TableView<T> tableView, 
            CheckBox headerCheckBox, 
            Function<T, BooleanProperty> property
    ) {
        int totalCount = tableView.getItems().size();
        if (totalCount == 0) {
            headerCheckBox.setSelected(false);
            headerCheckBox.setIndeterminate(false);
            return;
        }
        
        int selectedCount = 0;
        for (T item : tableView.getItems()) {
            BooleanProperty prop = property.apply(item);
            if (prop != null && prop.get()) {
                selectedCount++;
            }
        }
        
        if (selectedCount == totalCount) {
            headerCheckBox.setSelected(true);
            headerCheckBox.setIndeterminate(false);
        } else if (selectedCount == 0) {
            headerCheckBox.setSelected(false);
            headerCheckBox.setIndeterminate(false);
        } else {
            headerCheckBox.setIndeterminate(true);
        }
    }
    // ========== Inner Classes ==========
    
    /**
     * Integer 편집 셀 내부 클래스
     */
    private static class IntegerEditingCell<T> extends TextFieldTableCell<T, Integer> {
        private final String alignment;
        private final UnaryOperator<TextFormatter.Change> numberFilter;
        
        public IntegerEditingCell(String alignment) {
            super(new IntegerStringConverter());
            this.alignment = alignment;
            this.numberFilter = change -> {
                String newText = change.getControlNewText();
                if (newText.isEmpty()) return change;
                return newText.matches("-?\\d*") ? change : null;
            };
            applyStyle();
        }
        
        private void applyStyle() {
            setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        }
        
        @Override
        public void startEdit() {
            super.startEdit();
            applyStyle();
            configureTextField();
        }
        
        @Override
        public void updateItem(Integer item, boolean empty) {
            super.updateItem(item, empty);
            applyStyle();
        }
        
        private void configureTextField() {
            TextField textField = (TextField) getGraphic();
            if (textField == null) return;
            
            textField.setTextFormatter(new TextFormatter<>(
                new IntegerStringConverter(), null, numberFilter
            ));
            textField.setStyle(getAlignmentStyle(alignment));
        }
    }
    
    /**
     * 통화 표시 셀 내부 클래스
     */
    private static class CurrencyCell<T> extends TableCell<T, Double> {
        private final NumberFormat currencyFormat;
        
        public CurrencyCell() {
            this.currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
            currencyFormat.setMinimumFractionDigits(2);
            currencyFormat.setMaximumFractionDigits(2);
        }
        
        @Override
        protected void updateItem(Double price, boolean empty) {
            super.updateItem(price, empty);
            if (empty || price == null) {
                setText(null);
            } else {
                setText(currencyFormat.format(price));
            }
        }
    }
    
    /**
     * 날짜/시간 표시 셀 내부 클래스
     */
    private static class DateTimeCell<T> extends TableCell<T, LocalDateTime> {
        @Override
        protected void updateItem(LocalDateTime item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setText(null);
            } else {
                setText(item.toString());
            }
        }
    }
    
    /**
     * 버튼 컨테이너 셀 내부 클래스
     */
    private static class ButtonCell<T> extends TableCell<T, Void> {
        private final Button button;
        
        public ButtonCell(String iconPath, Consumer<T> action) {
            this.button = createButton(iconPath);
            configureButton(action);
        }
        
        private Button createButton(String iconPath) {
            Button btn = new Button();
            if (iconPath != null) {
                btn.setGraphic(loadIconView(iconPath));
            }
            btn.setAlignment(Pos.CENTER);
            btn.setMinWidth(45);
            btn.setPrefWidth(45);
            btn.setMinHeight(34);
            btn.setPrefHeight(34);
            btn.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            btn.setPadding(Insets.EMPTY);
            btn.setStyle(BUTTON_STYLE_DEFAULT);
            return btn;
        }
        
        private void configureButton(Consumer<T> action) {
            button.setOnMouseEntered(e -> {
                getTableView().getSelectionModel().select(getIndex());
                button.setStyle(BUTTON_STYLE_HOVER);
            });
            button.setOnMouseExited(e -> button.setStyle(BUTTON_STYLE_DEFAULT));
            button.setOnAction(e -> {
                T item = getTableRow().getItem();
                if (item != null) action.accept(item);
            });
        }
        
        @Override
        public void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty ? null : button);
            setAlignment(Pos.CENTER);
        }
    }
    
    /**
     * 라벨 컨테이너 셀 내부 클래스
     */
    private static class LabelCell<T> extends TableCell<T, Void> {
        private final Label label;
        
        public LabelCell(String iconPath, EventHandler<MouseEvent> actionEvent) {
            this.label = createLabel(iconPath);
            configureLabel(actionEvent);
        }
        
        private Label createLabel(String iconPath) {
            Label lbl = new Label();
            if (iconPath != null) {
                ImageView imageView = loadIconView(iconPath);
                if (imageView != null) {
                    imageView.setFitWidth(16);
                    imageView.setFitHeight(16);
                    lbl.setGraphic(imageView);
                }
            }
            lbl.setAlignment(Pos.CENTER);
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setStyle(STYLE_TRANSPARENT);
            lbl.setCursor(Cursor.HAND);
            return lbl;
        }
        
        private void configureLabel(EventHandler<MouseEvent> actionEvent) {
            label.setOnMouseEntered(e -> {
                getTableView().getSelectionModel().select(getIndex());
                label.setStyle("-fx-background-color: #6F4CBB; -fx-padding: 10px;");
            });
            label.setOnMouseExited(e -> label.setStyle("-fx-background-color: transparent; -fx-padding: 10px;"));
            label.setOnMousePressed(actionEvent);
        }
        
        @Override
        public void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            if (empty) {
                setGraphic(null);
            } else {
                setGraphic(label);
                setAlignment(Pos.CENTER);
            }
        }
    }

    // ========== Private Constructors ==========
    
    private TableColumnUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ========== Private Helper Methods ==========
    
    private static String getAlignmentStyle(String alignment) {
        if (alignment == null) return STYLE_CENTER;
        return switch (alignment.toUpperCase()) {
            case RIGHT -> STYLE_RIGHT;
            case LEFT -> STYLE_LEFT;
            default -> STYLE_CENTER;
        };
    }
    
    private static ImageView loadIconView(String iconPath) {
        if (iconPath == null) return null;
        URL url = TableColumnUtil.class.getResource(iconPath);
        if (url == null) return null;
        
        ImageView imageView = new ImageView(new Image(url.toExternalForm()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(22);
        imageView.setFitHeight(22);
        return imageView;
    }
    
    private static <T> void setupStaticColumnProps(TableColumn<T, Void> column, String title, String iconPath, Integer width) {
        if (title != null) column.setText(title);
        if (width != null) column.setPrefWidth(width);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
    }

 
}