com.swna.javafx.pos
├── api (외부 서버와의 통신 레이어)
│   ├── ProductApiService.java
│   └── (기타 POS 관련 API 클라이언트)
│
├── domain (순수 비즈니스 모델 및 결과 객체)
│   ├── PosItem.java
│   └── PaymentResult.java
│
├── dto (계층 간 데이터 전송 객체)
│   ├── request (CardAuthRequest, SaleRequest 등)
│   └── response (SaleResponse 등)
│
├── service (비즈니스 로직 및 워크플로우 제어)
│   ├── ScanService.java (상품 스캔 및 캐싱)
│   ├── PaymentService.java (서버 결제 프로세스 관리)
│   ├── CardPaymentService.java (기존 CardClient, 결제 정책 관리)
│   └── config (상태 및 설정 관리)
│       ├── PosToggleService.java
│       └── PrintToggleService.java
│
└── infrastructure (하드웨어 의존적인 구현체)
    └── vault
        └── VaultService.java (Verifone SDK 연동 실체)