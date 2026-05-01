package com.swna.javafx.view_ui.pos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.swna.javafx.common.constant.IconPaths;
import com.swna.javafx.common.ui.table.TableColumnUtil;
import com.swna.javafx.domain.pos.PosItem;
import com.swna.javafx.infrastructure.scanner.BarcodeInputEngine;
import com.swna.javafx.viewmodel.pos.PosViewModel;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j // 🔥 Logger 활성화 (SLF4J)
@Component
@Scope("prototype")
@RequiredArgsConstructor
@FxmlView("/view/pos/PosView.fxml")
public class PosViewController {

    // =========================
    // ViewModel (비즈니스 상태 담당)
    // =========================
    private final PosViewModel vm;

    // =========================
    // Barcode Scanner Input Engine
    // =========================
    private final BarcodeInputEngine barcodeInputEngine = new BarcodeInputEngine();

    // =========================
    // UI Components
    // =========================
    @FXML private TableView<PosItem> table;
    
    @FXML private TableColumn<PosItem, String> colNo;
    @FXML private TableColumn<PosItem, String> colBarcode;
    @FXML private TableColumn<PosItem, String> colDesc;
    @FXML private TableColumn<PosItem, String> colComment;

    @FXML private TableColumn<PosItem, Integer> colQty;
    @FXML private TableColumn<PosItem, Integer> colStock;
    @FXML private TableColumn<PosItem, Double> colPrice;
    @FXML private TableColumn<PosItem, Double> colTotal;
    @FXML private TableColumn<PosItem, Double> colDiscount;

    @FXML private TableColumn<PosItem, Void> colDelete;
    @FXML private TableColumn<PosItem, Void> colMinus;
    @FXML private TableColumn<PosItem, Void> colPlus;
    @FXML private TableColumn<PosItem, Void> colDiscountPrice;
    @FXML private TableColumn<PosItem, Void> colChangePrice;

    @FXML private Label labelDiscount;
    @FXML private Label labelInfo;
    @FXML private Label labelTime;
    @FXML private Label labelTotalAmount;
    @FXML private Label labelTotalQty;

    private static final DateTimeFormatter FORMATTER =  DateTimeFormatter.ofPattern("MM/dd a HH:mm:ss", Locale.ENGLISH);
    // =========================
    // Initialize (View lifecycle)
    // =========================
    @FXML
    public void initialize() {

        log.info("[INIT] PosViewController initialized");

        bindTop();
        bindTable();
        setupBarcodeScanner();
        startClock();

        buttonCart2.setVisible(false);
        buttonCart2.setManaged(false);
        
        buttonCart3.setVisible(false);
        buttonCart3.setManaged(false);
    }

    // =========================
    // Barcode Scanner Setup
    // =========================
    private void setupBarcodeScanner() {

        log.info("[SCANNER] initializing BarcodeInputEngine");

        // 1. Scanner callback 등록
        barcodeInputEngine.setOnBarcode(this::handleBarcode);

        // 2. Scene attach (UI 로딩 이후)
        table.sceneProperty().addListener((obs, oldScene, scene) -> {

            if (scene != null) {

                log.info("[SCANNER] attaching to scene");

                barcodeInputEngine.attach(scene);
            }
        });
    }

    // =========================
    // 🔥 Barcode 처리 핵심
    // =========================
    private void handleBarcode(String code) {

        log.info("[SCAN] received barcode: {}", code);

        if (code == null || code.isBlank()) {
            log.warn("[SCAN] ignored empty barcode");
            return;
        }

        try {
            Platform.runLater(() -> {

                log.info("[SCAN] sending to ViewModel: {}", code);

                vm.scan(code);

                log.info("[SCAN] ViewModel scan executed: {}", code);
            });

        } catch (Exception e) {
            log.error("[SCAN] unexpected error: {}", code, e);
        }
    }

    // =========================
    // Table Binding
    // =========================
    private void bindTable() {
        table.setEditable(true);
        table.setItems(vm.getItems());

        // ViewModel의 selectedItem이 변경되면 TableView의 선택 행도 변경됨
        vm.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                table.getSelectionModel().select(newVal);
                table.scrollTo(newVal);
            } else {
                table.getSelectionModel().clearSelection();
            }
        });

        vm.selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // 선택 상태 동기화
                table.getSelectionModel().select(newVal); 
                // 화면 스크롤 동기화 🔥 이 줄이 있으면 onQuickAmount에 필요 없음
                table.scrollTo(newVal); 
            }
        });


        // 반대로 TableView에서 행을 클릭했을 때 ViewModel의 selectedItem도 업데이트
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            vm.selectedItemProperty().set(newVal);
        });
        // 1. 번호 컬럼 (기본 중앙 정렬)
        TableColumnUtil.createNumberColumn(table, colNo, 70);

        // 2. 삭제 버튼 (중앙 정렬)
        TableColumnUtil.makeButtonColumn(colDelete, null, IconPaths.DELETE, 50, this::actionEvent);

        // 3. 바코드 (일반적으로 중앙 또는 왼쪽)
        TableColumnUtil.makeStringColumn(colBarcode, PosItem::barcodeProperty, PosItem::setBarcode, false, TableColumnUtil.CENTER, null);

        // 4. 상품명/설명 (텍스트가 길 수 있으므로 왼쪽 정렬)
        TableColumnUtil.makeStringColumn(colDesc, PosItem::descriptionProperty, PosItem::setDescription, false, TableColumnUtil.LEFT, null);

        // 5. 마이너스 버튼 (중앙 정렬)
        TableColumnUtil.makeButtonColumn(colMinus, null, IconPaths.MINUS, 50, this::actionEvent);

        // 6. 수량 (숫자이므로 오른쪽 또는 중앙)
        TableColumnUtil.makeIntegerColumn(colQty, PosItem::qtyProperty, PosItem::setQty, true, TableColumnUtil.CENTER, null);

        // 7. 플러스 버튼 (중앙 정렬)
        TableColumnUtil.makeButtonColumn(colPlus, null, IconPaths.PLUS, 50, this::actionEvent);

        // 8. 판매 단가 금액 (기호 "$"를 삭제하고 메서드 정의 순서에 맞춤)
        TableColumnUtil.makeCurrencyColumn(colPrice, PosItem::sellingPriceProperty, false, TableColumnUtil.RIGHT, null);

        // 8-1. 합계 금액
        TableColumnUtil.makeCurrencyColumn(colTotal, PosItem::finalAmountProperty, false, TableColumnUtil.RIGHT, null);

        // 9. 할인액
        TableColumnUtil.makeCurrencyColumn(colDiscount, PosItem::discountTotalProperty, false, TableColumnUtil.RIGHT, null);

        // 10. 재고 (숫자이므로 오른쪽 정렬)
        TableColumnUtil.makeIntegerColumn(colStock, PosItem::stockProperty, PosItem::setStock, false, TableColumnUtil.CENTER, null);

        // 11. 할인 처리 버튼 (중앙 정렬)
        TableColumnUtil.makeButtonColumn(colDiscountPrice, null, IconPaths.DISCOUNT, 50, this::actionEvent);

        // 12. 가격 변경 버튼 (중앙 정렬)
        TableColumnUtil.makeButtonColumn(colChangePrice, null, IconPaths.PRICE_22, 50, this::actionEvent);

        // 13. 비고/코멘트 (텍스트이므로 왼쪽 정렬)
        TableColumnUtil.makeStringColumn(colComment, PosItem::commentProperty, PosItem::setComment, false, TableColumnUtil.LEFT, null);
        
        setupStockStyle();

        log.info("[TABLE] binding completed");
    }

    // =========================
    // Stock Style
    // =========================
    private void setupStockStyle() {

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

                // 1. 기본 스타일 (투명 배경 + 중앙 정렬)을 베이스로 선언
                // TableColumnUtil의 상수를 public으로 열어두셨다면 직접 접근하거나 
                // 아래처럼 동일하게 정의해서 사용하세요.
                String baseStyle = "-fx-background-color: transparent; -fx-alignment: CENTER;";

                // 2. 조건에 따라 스타일 추가
                if (value < 0) {
                    // 기존 정렬 유지 + 빨간색 + 굵게
                    setStyle(baseStyle + "-fx-text-fill: red; -fx-font-weight: bold;");
                } else {
                    // 기존 정렬 유지 + 기본 글자색
                    setStyle(baseStyle + "-fx-text-fill: black;");
                }
            }
        });
    }


    // =========================
    // Top Binding
    // =========================
    private void bindTop() {
        labelTotalAmount.textProperty().bind( vm.totalAmountProperty().asString("Total: %.2f") );
        labelTotalQty.textProperty().bind( vm.totalQtyProperty().asString("Total Qty: %d"));
        labelDiscount.textProperty().bind( vm.discountProperty().asString("Discount: %.2f"));
        labelInfo.textProperty().bind(vm.scanStatusProperty());
    }

    // =========================
    // Table Action Event
    // =========================
    private void actionEvent(MouseEvent event) {
        log.debug("[TABLE ACTION] clicked: {}", event.getSource());
    }

    // =========================
    // Add / Remove
    // =========================
    @FXML
    private void onQuickAmount(ActionEvent e) {
        try {
            Button button = (Button) e.getSource();
            String text = button.getText();
            String amountText = text.replaceAll("[^0-9.]", ""); 
            
            if (!amountText.isEmpty()) {
                double amount = Double.parseDouble(amountText);
                
                // 1. ViewModel 로직 실행
                vm.addQuickAmountItem(amount);
                
                // 2. ViewModel이 선택한 아이템을 TableView에서도 선택 상태로 만듦
                // PosItem target = vm.selectedItemProperty().get();
                // if (target != null) {
                //     table.getSelectionModel().select(target);
                    
                //     // 3. 해당 위치로 스크롤 이동
                //     table.scrollTo(target);
                // }
            }
        } catch (NumberFormatException ex) {
            log.error("[QUICK AMOUNT] Failed to parse amount", ex);
        }
    }


    @FXML
    private void onClose(MouseEvent  e) {

        log.info("[CLEAR] reset POS state");

        vm.clear();
        System.exit(0); 
    }

    // =========================
    // Payment
    // =========================
    @FXML
    private void onPayment() {

        double total = vm.totalAmountProperty().get();

        try {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setHeaderText("결제");
            alert.setContentText("총 결제 금액: " + total);
            alert.showAndWait();

        } catch (Exception e) {
            log.error("[PAYMENT] failed", e);
        }
    }
    
    @FXML
    private void onActionCart(ActionEvent event) {
        System.out.println("onActionCart");
    }

    @FXML
    private void onActionDiscountVolumn(ActionEvent event) {
        System.out.println("onActionDiscountVolumn");
    }

    @FXML
    private void onActionScanner(ActionEvent event) {
        System.out.println("onActionScanner");
    }

    @FXML
    private void onActionCancel(ActionEvent event) {
        System.out.println("onActionCancel");
    }

    @FXML
    private void onActionPrint(ActionEvent event) {
        System.out.println("onActionPrint");
    }

    @FXML
    private void onActionQty(ActionEvent event) {
        System.out.println("onActionQty");
    }

    @FXML
    private void onActionCash(ActionEvent event) {
        System.out.println("onActionCash");
    }

    @FXML
    private void onActionCredit(ActionEvent event) {
        System.out.println("onActionCredit");
    }

    @FXML
    private void onActionCashout(ActionEvent event) {
        System.out.println("onActionCashout");
    }

    @FXML
    private void onActionDrawer(ActionEvent event) {
        System.out.println("onActionDrawer");
    }

    @FXML
    private void onPos(ActionEvent event) {
        System.out.println("onPos");
    }

    @FXML
    private void onPrint(ActionEvent event) {
        System.out.println("onPrint");
    }

    @FXML private Button buttonCart1;
    @FXML private Button buttonCart2;
    @FXML private Button buttonCart3;
    @FXML private Button buttonDiscountVolumn;
    @FXML private Button buttonScanner;
    @FXML private Button buttonCancel;
    @FXML private Button buttonPrint;
    @FXML private Button buttonQty;
    @FXML private Button buttonCash;
    @FXML private Button buttonCredit;
    @FXML private Button buttonCashout;
    @FXML private Button buttonDrawer;
    @FXML private ImageView posImageView;
    @FXML private ImageView printImageView;

    private void startClock() {
        // 1초마다 실행되는 Timeline 생성
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            labelTime.setText(LocalDateTime.now().format(FORMATTER));
        }), new KeyFrame(Duration.seconds(1)));

        clock.setCycleCount(Animation.INDEFINITE); // 무한 반복
        clock.play(); // 시계 시작
    }
}