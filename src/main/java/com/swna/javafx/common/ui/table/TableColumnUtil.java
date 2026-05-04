package com.swna.javafx.common.ui.table;

import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
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
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableColumnUtil {

    // 정렬 상수를 정의하여 외부에서 가독성 있게 호출하도록 합니다.
    public static final String CENTER = "CENTER";
    public static final String RIGHT = "RIGHT";
    public static final String LEFT = "LEFT";

    // CSS 스타일링 상수
    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT;";
    private static final String STYLE_TRANSPARENT = "-fx-background-color: transparent;";

    // 🔥 인스턴스 생성 방지
    private TableColumnUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * 정렬 값에 따른 CSS 스타일을 반환하는 헬퍼 메서드
     */
    private static String getAlignmentStyle(String alignment) {
        if (alignment == null) return STYLE_CENTER;
        return switch (alignment.toUpperCase()) {
            case RIGHT -> STYLE_RIGHT;
            case LEFT -> STYLE_LEFT;
            default -> STYLE_CENTER;
        };
    }

    // ========================
    // Number (Row Index)
    // ========================
    public static <T> TableColumn<T, String> createNumberColumn(TableView<T> tableView, TableColumn<T, String> column, int width) {
        if (width != 0) column.setPrefWidth(width);
        column.setText("NO");
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
        column.setCellValueFactory(p -> new ReadOnlyObjectWrapper<>(" " + (tableView.getItems().indexOf(p.getValue()) + 1) + " "));
        return column;
    }

    // ========================
    // String
    // ========================
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
                String newValue = event.getNewValue();
                setter.accept(row, newValue);
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    /**
     * 통화(Currency) 컬럼 생성
     * Locale에 맞는 기호, 소수점 2자리 표시 및 편집 기능 포함
     */
    public static <T> void makeCurrencyColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            boolean editable,
            String alignment,
            DirtyConsumer<T> dirtyConsumer
    ) {
        // 1. 값 바인딩 (DoubleProperty -> Object)
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        
        // 2. 스타일 및 정렬 적용
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        // 3. 통화 포맷터 설정 (소수점 2자리 고정)
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault());
        currencyFormat.setMinimumFractionDigits(2);
        currencyFormat.setMaximumFractionDigits(2);

        // 4. 셀 팩토리 설정 (표시 형식 지정)
        column.setCellFactory(tc -> new TableCell<T, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    // 화면 표시: $ 1,234.56 또는 ₩ 1,234.56
                    setText(currencyFormat.format(price));
                }
            }
        });

        // 5. 편집 모드 설정
        if (editable) {
            // 편집 시에는 기호 없이 숫자만 입력받기 위해 기본 DoubleStringConverter 사용
            column.setCellFactory(TextFieldTableCell.forTableColumn(new DoubleStringConverter()));

            // 편집 완료 시 로직
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                Double newValue = event.getNewValue();
                
                // 데이터 모델 업데이트 (Property의 set 메서드 호출)
                DoubleProperty prop = propertyGetter.apply(row);
                prop.set(newValue);

                // Dirty 상태 등록
                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });
        } else {
            column.setEditable(false);
        }
    }


    // ========================
    // Integer
    // ========================
    public static <T> void makeIntegerColumn(
            TableColumn<T, Integer> column,
            Function<T, IntegerProperty> propertyGetter,
            ObjIntConsumer<T> setter,
            boolean editable,
            String alignment,           // 정렬 추가
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        if (editable) {
           column.setCellFactory(col -> new TextFieldTableCell<T, Integer>(new IntegerStringConverter()) {
                {
                    // 기본 스타일 유지 (핵심)
                    setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
                }

                private final UnaryOperator<TextFormatter.Change> filter = change -> {
                    String text = change.getControlNewText();

                    if (text.isEmpty()) return change;

                    if (text.matches("-?\\d*")) {
                        return change;
                    }

                    return null;
                };

                @Override
                public void startEdit() {
                    super.startEdit();

                    TextField textField = (TextField) getGraphic();
                    if (textField != null) {
                        textField.setTextFormatter(
                                new TextFormatter<>(new IntegerStringConverter(), null, filter)
                        );

                        // 🔥 중요: edit mode에서도 스타일 유지
                        textField.setStyle(getAlignmentStyle(alignment));
                    }

                    // 🔥 cell 자체도 유지
                    this.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
                }

                @Override
                public void updateItem(Integer item, boolean empty) {
                    super.updateItem(item, empty);

                    // 🔥 렌더링 시마다 다시 적용 (중요)
                    setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
                }
            });
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        } else {
            column.setEditable(false);
        }
    }

    public static <T> void makeReadOnlyLongColumn(TableColumn<T, Long> column, Function<T, LongProperty> propertyGetter, String alignment) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject());
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setEditable(false);
    }

    // ========================
    // Double
    // ========================
    public static <T> void makeDoubleColumn(
            TableColumn<T, Double> column,
            Function<T, DoubleProperty> propertyGetter,
            ObjDoubleConsumer<T> setter,
            boolean editable,
            String alignment,           // 정렬 추가
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

    // ========================
    // LocalDateTime
    // ========================
    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            String alignment,           // 정렬 추가
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> new SimpleObjectProperty<>(getter.apply(cell.getValue())));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString()); 
                }
            }
        });

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());
                if (dirtyConsumer != null) dirtyConsumer.accept(row);
            });
        }
    }

    public static <T> void makeDatePickerColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<T, LocalDateTime> setter,
            String alignment,           // 정렬 추가
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cell -> getter.apply(cell.getValue()));
        column.setStyle(STYLE_TRANSPARENT + getAlignmentStyle(alignment));
        column.setCellFactory(col -> new DatePickerTableCell<T>(setter, dirtyConsumer));
        column.setEditable(true);
    }

    // ========================
    // Boolean
    // ========================
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
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER); // 불리언은 보통 중앙 정렬 고정
        column.setEditable(editable);
    }

    // ========================
    // Button & Label Column
    // ========================
    public static <T> void makeButtonColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            Consumer<T> action
    ) {

        setupStaticColumnProps(column, title, iconPath, width);

        column.setCellFactory(param -> new TableCell<>() {

            private final Button button = new Button();

            {
                if (iconPath != null) {
                    button.setGraphic(loadIconView(iconPath));
                }

                button.setAlignment(Pos.CENTER);

                button.setMinWidth(45);
                button.setPrefWidth(45);

                button.setMinHeight(34);
                button.setPrefHeight(34);

                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

                button.setPadding(Insets.EMPTY);

                button.setStyle("-fx-background-color:transparent; -fx-alignment: center;");

                button.setOnMouseEntered(e -> {
                    getTableView().getSelectionModel().select(getIndex());
                    button.setStyle("-fx-background-color:#6F4CBB;");
                });

                button.setOnMouseExited(e ->
                        button.setStyle(STYLE_TRANSPARENT));

                // 핵심 수정 부분
                button.setOnAction(e -> {

                    T item = getTableRow().getItem();

                    if (item != null) {
                        action.accept(item);
                    }
                });
            }

            @Override
            public void updateItem(Void item, boolean empty) {

                super.updateItem(item, empty);

                setGraphic(empty ? null : button);

                setAlignment(Pos.CENTER);
            }
        });
    }

    public static <T> void makeLableColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {
        setupStaticColumnProps(column, title, iconPath, width);

        column.setCellFactory(param -> new TableCell<>() {
            private final Label iconLabel = new Label();
            {
                if (iconPath != null) {
                    ImageView imageView = loadIconView(iconPath);
                    if (imageView != null) {
                        imageView.setFitWidth(16);
                        imageView.setFitHeight(16);
                        iconLabel.setGraphic(imageView);
                    }
                }
                iconLabel.setAlignment(Pos.CENTER);
                iconLabel.setMaxWidth(Double.MAX_VALUE);
                iconLabel.setStyle(STYLE_TRANSPARENT);
                iconLabel.setCursor(Cursor.HAND);
                iconLabel.setOnMouseEntered(e -> {
                    getTableView().getSelectionModel().select(getIndex());
                    iconLabel.setStyle("-fx-background-color: #6F4CBB; -fx-padding: 10x;");
                });
                iconLabel.setOnMouseExited(e -> iconLabel.setStyle("-fx-background-color: transparent; -fx-padding: 10px;"));
                iconLabel.setOnMousePressed(actionEvent);
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(iconLabel);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private static <T> void setupStaticColumnProps(TableColumn<T, Void> column, String title, String iconPath, Integer width) {
        if (title != null) column.setText(title);
        if (width != null) column.setPrefWidth(width);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(false);
        column.setStyle(STYLE_TRANSPARENT + STYLE_CENTER);
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
}