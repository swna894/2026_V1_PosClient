# 최종 데이터 흐름
PaymentProcessor.executePaymentWithRequest()
    ↓ finalSaleRequest (payments 포함)
ProcessedPayment(finalSaleRequest)
    ↓
PosViewModel.handleProcessedPayment()
    ↓ processed.saleRequest() (payments 포함)
PaymentSuccessEvent(saleRequest)
    ↓
ReceiptPrintListener.printReceipt()
    ↓ event.getSaleRequest() (payments 포함)
ReceiptPrinter.printInvoice(saleRequest)
    ↓
ReceiptFormatter.buildContent(saleRequest)
    ↓ saleRequest.payments() → null 아님! ✅


# 개선 사항

feat(print): 영수증 출력 시스템 전면 개선

## 주요 변경사항

### 1. 결제 데이터 전달 구조 개선
- PaymentProcessor.executePaymentWithRequest() 수정
  - 성공/실패 여부와 관계없이 항상 payments가 포함된 finalSaleRequest 전달
  - 기존: 실패 시 null 또는 payments 없는 SaleRequest 전달
  - 변경: 성공/실패 모두 payments 포함된 finalSaleRequest 전달
- NPE 방지: ReceiptPrintListener에서 받는 SaleRequest.payments() null 보장

### 2. PrinterService Console 디버깅 지원
- @Value 어노테이션으로 디버그 모드 설정 가능
  - print.debug.console: Console 출력 여부
  - print.debug.real: 실제 프린터 출력 여부
- ESC/POS 명령어 분석 및 Hex 덤프 기능 추가
- 인코딩 처리 강화 (cp949, UTF-8 등 지원)

### 3. ReceiptPrinter 데이터 통합 전송
- 여러 번의 printBytes 호출을 한 번의 호출로 통합
- ESC/POS 명령어(초기화, 폰트, 정렬, 바코드, 컷팅)를 하나의 바이트 배열로 구성
- 프린터 통신 최적화 및 성능 개선

### 4. ReceiptFormatter 영수증 포맷 개선
- 한글 인코딩 문제 해결 (영문 출력 기본)
- null 안전 처리 강화 (Shop, PosItem 등)
- 금액 표시에 화폐 단위($) 추가
- Notice 구분선 별표(*) 제거 (가독성 개선)
- 상품명, 수량, 단가, 금액 정렬 개선

### 5. Shop 정보 로딩 개선
- ShopViewModel에 ApiException 처리 추가
- API 실패 시 기본 Shop 정보 반환 (My Store)
- 비동기/동기 로딩 모두 지원
- CountDownLatch를 통한 동기 대기 구현

### 6. 영수증 출력 비동기 처리
- AsyncConfig를 통한 전용 스레드 풀 설정
  - corePoolSize: 2, maxPoolSize: 5, queueCapacity: 100
  - 스레드 이름: ReceiptPrint- 으로 식별 가능
- UI 스레드 블로킹 방지

### 7. ValidationResult 중복 제거
- PaymentProcessor 내부 중복 ValidationResult 클래스 삭제
- 기존 com.swna.javafx.pos.viewmodel.ValidationResult 사용 통일

## 영향
- 영수증 출력 시 NPE 발생하지 않음
- API 실패 시에도 기본 Shop으로 영수증 출력 가능
- 개발 환경에서 Console로 영수증 내용 확인 가능
- 한글 인코딩 문제 해결

## 관련 이슈
- NPE: Cannot invoke "java.util.List.iterator()" because return value is null
- IllegalStateException: Not on FX application thread
- ApiException: Internal server error (서버 장애 시 기본 Shop 사용)
- UnmappableCharacterException: 한글 인코딩 문제 해결

## 변경 파일 목록
- PaymentProcessor.java
- ReceiptPrintListener.java
- PrinterService.java
- ReceiptPrinter.java
- ReceiptFormatter.java
- ReceiptStyle.java
- ShopViewModel.java
- AsyncConfig.java
- ValidationResult.java (이동/삭제)