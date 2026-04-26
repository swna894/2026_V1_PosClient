
# 전체 구조
com.newvista.client.pos
│
├── view                ← FXML + Controller (View 역할만)
│   ├── PosMainView.fxml
│   └── PosMainController.java
│
├── viewmodel          ← 핵심 (상태 + UI 로직)
│   └── PosViewModel.java
│
├── model              ← 순수 데이터 + 상태 관리
│   ├── dto
│   │   └── InvoiceItem.java
│   ├── state
│   │   ├── PosState.java
│   │   └── CartHolder.java
│   └── repository
│       └── ScannedItemRepository.java
│
├── service            ← 비즈니스 로직
│   ├── BarcodeService.java
│   ├── PaymentService.java
│   ├── InvoiceService.java
│   └── ProductService.java
│
├── command            ← 기존 Command 유지 (좋은 구조임)
│   └── CartToggleCommand.java
│
├── factory
│   ├── QuickButtonFactory.java
│   └── RenderTableFactory.java
│
├── handler
│   ├── BarcodeScanHandler.java
│   ├── ShortcutKeyHandler.java
│   └── AlertLabelHandler.java
│
└── dialog
    └── (기존 Dialog 그대로 유지)