package com.swna.javafx.common.ui.table;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DoubleStringConverter;
import javafx.util.converter.IntegerStringConverter;

public class TableColumnUtil {

        // 정렬을 위한 상수를 정의하여 가독성을 높입니다.
    public static final String CENTER = "CENTER";
    public static final String RIGHT = "RIGHT";
    public static final String LEFT = "LEFT";

    // CSS 스타일링 상수를 정의하여 중복을 피하고 유지보수를 용이하게 합니다.
    private static final String STYLE_CENTER = "-fx-alignment: CENTER;";
    private static final String STYLE_RIGHT = "-fx-alignment: CENTER-RIGHT;";
    private static final String STYLE_LEFT = "-fx-alignment: CENTER-LEFT;";

        // 🔥 인스턴스 생성 방지
    private TableColumnUtil() {
        throw new UnsupportedOperationException("Utility class");
    }
    // ========================
    // String
    // ========================
    public static <T> void makeStringColumn(
            TableColumn<T, String> column,
            Function<T, StringProperty> propertyGetter,
            BiConsumer<T, String> setter,
            boolean editable,
            DirtyConsumer<T> dirtyConsumer   // ⭐ 추가
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue())
        );

        if (editable) {

            column.setCellFactory(TextFieldTableCell.forTableColumn());

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();
                String newValue = event.getNewValue();

                setter.accept(row, newValue);

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);   // ⭐ Dirty 등록
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
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue()).asObject()
        );

        if (editable) {

            StringConverter<Integer> converter = new IntegerStringConverter();

            column.setCellFactory(TextFieldTableCell.forTableColumn(converter));

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();

                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });

        } else {
            column.setEditable(false);
        }
    }

    public static <T> void makeReadOnlyIntegerColumn( TableColumn<T, Long> column, Function<T, LongProperty> propertyGetter) {
        column.setCellValueFactory(cellData -> propertyGetter.apply(cellData.getValue()).asObject() );
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
            DirtyConsumer<T> dirtyConsumer
    ) {
        column.setCellValueFactory(cellData ->
                propertyGetter.apply(cellData.getValue()).asObject()
        );

        if (editable) {

            StringConverter<Double> converter = new DoubleStringConverter();

            column.setCellFactory(TextFieldTableCell.forTableColumn(converter));

            column.setOnEditCommit(event -> {

                T row = event.getRowValue();

                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });

        } else {
            column.setEditable(false);
        }
    }

    public static <T> void makeDateTimeColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, LocalDateTime> getter,
            BiConsumer<T, LocalDateTime> setter,
            boolean editable,
            Consumer<T> dirtyConsumer
    ) {

        column.setCellValueFactory(cell ->
                new SimpleObjectProperty<>(getter.apply(cell.getValue()))
        );

        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.toString()); // 필요하면 formatter 적용
                }
            }
        });

        if (editable) {
            column.setOnEditCommit(event -> {
                T row = event.getRowValue();
                setter.accept(row, event.getNewValue());

                if (dirtyConsumer != null) {
                    dirtyConsumer.accept(row);
                }
            });
        }
    }

    // ========================
    // LocalDateTime
    // ========================
    public static <T> void makeDatePickerColumn(
            TableColumn<T, LocalDateTime> column,
            Function<T, ObjectProperty<LocalDateTime>> getter,
            BiConsumer<T, LocalDateTime> setter,
            DirtyConsumer<T> dirtyConsumer
    ) {

        column.setCellValueFactory(cell ->
                getter.apply(cell.getValue())
        );

        column.setCellFactory(col ->
                new DatePickerTableCell<T>(setter, dirtyConsumer) // ✅ 핵심
        );

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

                    if (dirtyConsumer != null) {
                        dirtyConsumer.accept(cellData.getValue());
                    }
                });
            }

            return prop;
        });

        column.setEditable(editable);
    }

    // ========================
    // Button Column
    // ========================

    public static <T> void makeButtonColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {

        if (title != null) { column.setText(title); }
        column.setStyle(STYLE_CENTER);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(false);
        column.setStyle("-fx-background-color: transparent; -fx-padding: 0px 0px 0px 6px;");

        if (width != null) { column.setPrefWidth(width); }

        Callback<TableColumn<T, Void>, TableCell<T, Void>> cellFactory = param -> new TableCell<>() {
            private final Button button = new Button();
            {
                if (iconPath != null) {
                    button.setGraphic(loadIconView(iconPath));
                }

                button.setAlignment(Pos.CENTER);
                setAlignment(Pos.CENTER);
                button.setMaxWidth(Double.MAX_VALUE);
                button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                button.setPadding(Insets.EMPTY);
                button.setStyle("-fx-background-color:transparent; -fx-alignment: center;");

                button.setOnMouseEntered(event -> {
                    getTableView().getSelectionModel().select(getIndex());
                    button.setStyle("-fx-background-color:#6F4CBB");
                });
                button.setOnMouseExited(event -> button.setStyle("-fx-background-color:transparent"));
                button.setOnMousePressed(actionEvent);
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
                setAlignment(Pos.CENTER);
            }
        };

        column.setCellFactory(cellFactory);
    }

    public static <T> void makeLableColumn(
            TableColumn<T, Void> column,
            String title,
            String iconPath,
            Integer width,
            EventHandler<MouseEvent> actionEvent
    ) {
        if (title != null) { column.setText(title); }
        column.setStyle(STYLE_RIGHT);
        column.setGraphic(loadIconView(iconPath));
        column.setSortable(false);
        column.setStyle("-fx-background-color: transparent; -fx-padding: 0px 0px 0px 5px;");
                
        if (width != null) { column.setPrefWidth(width); }

        Callback<TableColumn<T, Void>, TableCell<T, Void>> cellFactory = param -> new TableCell<>() {
            private final Label iconLabel = new Label();

            {
                if (iconPath != null) {
                    ImageView imageView = loadIconView(iconPath);
                    if (imageView != null) {
                        imageView.setPreserveRatio(true);
                        imageView.setFitWidth(16);  // 필요시 이미지 크기 조정
                        imageView.setFitHeight(16);
                        iconLabel.setGraphic(imageView);
                    }
                }

                // Label 중앙 정렬 설정
                iconLabel.setAlignment(Pos.CENTER);
                iconLabel.setMaxWidth(Double.MAX_VALUE);
                iconLabel.setStyle("-fx-background-color: transparent;");
                
                // 마우스 이벤트 처리 (Button 대신 Label에 직접 적용)
                iconLabel.setOnMouseEntered(event -> {
                    getTableView().getSelectionModel().select(getIndex());
                    iconLabel.setStyle("-fx-background-color: #6F4CBB; -fx-padding: 5px;");
                });
                
                iconLabel.setOnMouseExited(event -> 
                    iconLabel.setStyle("-fx-background-color: transparent; -fx-padding: 5px;")
                );
                
                iconLabel.setOnMousePressed(actionEvent);
                
                // 클릭 시 마우스 커서 변경 (선택사항)
                iconLabel.setCursor(Cursor.HAND);
            }

            @Override
            public void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    setGraphic(iconLabel);
                    setText(null);  // 텍스트는 사용하지 않음
                    // 셀 자체도 중앙 정렬
                    setAlignment(Pos.CENTER);
                }
            }
        };

        column.setCellFactory(cellFactory);
    }

    private static ImageView loadIconView(String iconPath) {
        if (iconPath == null) return null;

        URL url = TableColumnUtil.class.getResource(iconPath);
        if (url == null) return null;

        ImageView imageView = new ImageView(new Image(url.toExternalForm()));
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(24);  // 기본 이미지 크기 설정
        imageView.setFitHeight(24);
        
        return imageView;
    }
}