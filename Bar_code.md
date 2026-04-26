# 🖥 사용 방법 (JavaFX Controller)

BarcodeInputEngine engine = new BarcodeInputEngine();

engine.setOnBarcode(code -> {
    System.out.println("✔ 바코드 인식: " + code);

    // 서버 호출 or 상품 조회
    searchProduct(code);
});

engine.attach(scene);

# 서버 연결까지 포함하면

engine.setOnBarcode(code -> {

    webClient.get()
            .uri("/api/product?barcode=" + code)
            .retrieve()
            .bodyToMono(Product.class)
            .subscribe(product -> {

                Platform.runLater(() -> {
                    addToTable(product);
                });

            });
});

# 
public class PosController {

    private Order currentOrder = new Order();

    @FXML private TableView<OrderItem> table;

    public void onBarcodeScanned(String barcode) {
        Product product = productService.findByBarcode(barcode);

        currentOrder.addItem(product);

        refreshTable();
    }

    public void onClickPayment() {
        PaymentDialog.show(currentOrder, this::handlePaymentResult);
    }

    private void handlePaymentResult(Payment payment) {
        paymentService.process(payment);
        currentOrder = new Order(); // 초기화
    }
}