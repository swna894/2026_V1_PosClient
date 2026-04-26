#🔥 한 줄 정의
   application 👉 “비즈니스 흐름을 조립하는 레이어”
   infrastructure 👉 “외부 세계와 연결하는 레이어”

# 🧠 전체 흐름에서 위치
   UI (JavaFX)
      ↓
   Application (UseCase / Service)
      ↓
   Domain (핵심 로직)
      ↓
   Infrastructure (DB / API / Scanner)

# 전체 구조 (MVVM + POS + Admin Menu) 
   pos-system/
   ├─ ui/
   │   ├─ pos/                ← POS 운영 화면
   │   ├─ admin/              ← 관리 메뉴 (상품/매장/설정)
   │   ├─ common/
   │   └─ component/
   │
   ├─ viewmodel/              ← MVVM 핵심
   │   ├─ pos/
   │   ├─ payment/
   │   └─ admin/
   │
   ├─ application/            ← Service
   ├─ domain/                 ← 비즈니스 모델
   ├─ infrastructure/         ← 외부 연동

# 🧠 MVVM 적용 핵심
   View (FXML)
      ↓ bind
   ViewModel (State + Logic)
      ↓ call
   Service
      ↓
   Domain / API

# 🔥 실제 흐름 연결
   [ScannerEngine]
      ↓
   [UI Controller]
      ↓
   [ProductService]
      ↓
   [ProductApiClient]
      ↓
   [Server]

# 💡 Payment Dialog 연결 흐름
   PosMainController
      ↓
   PaymentDialog open
      ↓
   Payment 생성 (MixedPayment)
      ↓
   PaymentService.process()
      ↓
   PaymentApiClient

# ✔ 이벤트 기반 확장
   application/event/
      ├─ BarcodeScannedEvent
      └─ PaymentCompletedEvent


# 🔥 폴더 구조 (최종 형태)
   src/main/java/com/example/pos/

   ├─ PosApplication.java
   ├─ PosFxApplication.java

   ├─ config/
   │   ├─ FxConfig.java
   │   └─ WebClientConfig.java

   ├─ ui/
   │   ├─ pos/
   │   │   ├─ PosMainView.fxml
   │   │   ├─ PosMainController.java
   │   │   └─ PosMenuBar.fxml   ← POS 메뉴 (결제/취소/설정)
   │   │
   │   ├─ payment/
   │   │   ├─ PaymentDialog.fxml
   │   │   └─ PaymentController.java
   │   │
   │   ├─ admin/               🔥 (관리자 메뉴)
   │   │   ├─ AdminMainView.fxml
   │   │   ├─ AdminMainController.java
   │   │   │
   │   │   ├─ product/
   │   │   │   ├─ ProductManageView.fxml
   │   │   │   └─ ProductManageController.java
   │   │   │
   │   │   ├─ shop/
   │   │   │   ├─ ShopConfigView.fxml
   │   │   │   └─ ShopConfigController.java
   │   │   │
   │   │   └─ user/
   │   │       ├─ UserManageView.fxml
   │   │       └─ UserManageController.java
   │   │
   │   ├─ component/
   │   │   ├─ ProductTableView.java
   │   │   ├─ OrderSummaryPane.java
   │   │   └─ ScannerInputView.java
   │   │
   │   └─ common/
   │       └─ BaseController.java

   ├─ viewmodel/              🔥 MVVM 핵심
   │   ├─ pos/
   │   │   └─ PosViewModel.java
   │   │
   │   ├─ payment/
   │   │   └─ PaymentViewModel.java
   │   │
   │   ├─ admin/
   │   │   ├─ AdminViewModel.java
   │   │   ├─ ProductViewModel.java
   │   │   ├─ ShopViewModel.java
   │   │   └─ UserViewModel.java
   │
   ├─ application/
   │   ├─ order/OrderService.java
   │   ├─ payment/PaymentService.java
   │   └─ product/ProductService.java

   ├─ domain/
   │   ├─ order/
   │   ├─ payment/
   │   └─ product/

   ├─ infrastructure/
   │   ├─ api/
   │   ├─ cache/
   │   └─ scanner/

   ├─ dto/
   └─ util/

# 추천 아키텍처: MVVM (또는 MVC + Service 분리)
   View (FXML)
      ↓ bind
   ViewModel (State + Logic)
      ↓ call
   Service
      ↓
   Domain / API

# 전체 구조 (MVVM + POS + Admin Menu)

   pos-system/
   ├─ ui/
   │   ├─ pos/                ← POS 운영 화면
   │   ├─ admin/              ← 관리 메뉴 (상품/매장/설정)
   │   ├─ common/
   │   └─ component/
   │
   ├─ viewmodel/              ← MVVM 핵심
   │   ├─ pos/
   │   ├─ payment/
   │   └─ admin/
   │
   ├─ application/            ← Service
   ├─ domain/                 ← 비즈니스 모델
   ├─ infrastructure/         ← 외부 연동

# 전체 아키텍처 (JavaFX POS 표준 구조)

   [UI Layer - JavaFX]
      ├─ PosMainView (메인화면)
      ├─ PaymentDialog (결제창)
      └─ Components (TableView, ScannerInput)

   [Application Layer]
      ├─ OrderService
      ├─ PaymentService
      └─ ProductService

   [Domain Layer]
      ├─ Order
      ├─ OrderItem
      ├─ Payment (추상)
      │    ├─ CashPayment
      │    ├─ CardPayment
      │    └─ MixedPayment
      └─ Product

   [Infrastructure]
      ├─ WebClient (서버 API)
      ├─ LocalCache
      └─ ScannerEngine

# 확장 구조 (POS에서 많이 쓰는 방식)
   Scanner Input (TextField)
         ↓
   Validation Layer (상품코드 체크)
         ↓
   Service Layer (WebClient / REST)
         ↓
   Server (Spring Boot)
         ↓
   DB 조회 (Product / Inventory)
         ↓
   UI 업데이트 (TableView / Popup)