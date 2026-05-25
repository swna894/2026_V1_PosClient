# 서버 구조
    barcode-server
    └── src/main/java
        └── com.example.server
            ├── controller
            │    └── ProductController.java
            │
            ├── dto
            │    └── ProductLabelDto.java
            │
            └── ServerApplication.java

# JavaFX MVVM 구조
    barcode-client
    └── src/main/java
        └── com.example.client

            ├── application
            │    └── LabelApplication.java
            │
            ├── config
            │    ├── WebClientConfig.java
            │    └── AppConfig.java
            │
            ├── dto
            │    └── ProductLabelDto.java
            │
            ├── infrastructure
            │    ├── api
            │    │    └── ProductApiClient.java
            │    │
            │    ├── barcode
            │    │    └── BarcodeGenerator.java
            │    │
            │    └── pdf
            │         └── PdfLabelGenerator.java
            │
            ├── domain
            │    └── model
            │         └── ProductLabel.java
            │
            ├── applicationservice
            │    └── LabelPrintService.java
            │
            ├── presentation
            │    ├── view
            │    │    ├── LabelView.fxml
            │    │    └── component
            │    │
            │    ├── viewmodel
            │    │    └── LabelViewModel.java
            │    │
            │    └── controller
            │         └── LabelController.java
            │
            └── util
                    └── PdfOpenUtil.java

# 처리 흐름
    LabelView
        ↓
    LabelController
        ↓
    LabelViewModel
        ↓
    LabelPrintService
        ↓
    ProductApiClient
        ↓
    BarcodeGenerator
        ↓
    PdfLabelGenerator

# pom.xml

    <!-- ZXing -->
    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>core</artifactId>
        <version>3.5.3</version>
    </dependency>

    <dependency>
        <groupId>com.google.zxing</groupId>
        <artifactId>javase</artifactId>
        <version>3.5.3</version>
    </dependency>

    <!-- iText -->
    <dependency>
        <groupId>com.itextpdf</groupId>
        <artifactId>itext7-core</artifactId>
        <version>8.0.5</version>
        <type>pom</type>
    </dependency>