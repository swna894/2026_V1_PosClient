# vault 개발 환경 만들기
   -.  2026_Posclient/lib 에 4개의 jar 파일 복사
   -. .m2 repository에 상기 4개 jar 파일 구성을 우해 하기 실행
     
./mvnw --% install:install-file -Dfile=D:\2025_SERVER\2026_PosClient\lib\Verifone.Vault.JavaAPI.jar -DgroupId=com.verifone -DartifactId=vault-api -Dversion=1.0 -Dpackaging=jar

   -. pom.xml에 vault 관련 하기를 import
         		<!-- Verifone Vault -->
		<dependency>
			<groupId>com.verifone</groupId>
			<artifactId>vault-api</artifactId>
			<version>1.0</version>
		</dependency>

		<!-- JNA -->
		<dependency>
			<groupId>net.java.dev.jna</groupId>
			<artifactId>jna</artifactId>
			<version>5.13.0</version>
		</dependency>

		<!-- JNA Platform -->
		<dependency>
			<groupId>net.java.dev.jna</groupId>
			<artifactId>jna-platform</artifactId>
			<version>5.13.0</version>
		</dependency>

		<!-- Joda-Time -->
		<dependency>
			<groupId>joda-time</groupId>
			<artifactId>joda-time</artifactId>
			<version>2.2</version>
		</dependency>

   -. 실행파일 만들기 : ./build.ps1 실행


[CashoutDialogController]
    ↓ cashOutAmount, totalAmount
[PosViewModel.processCardPaymentWithCashOut()]
    ↓
[PaymentProcessor.processCardPaymentWithCashOut()]
    ↓ cardAmount = totalAmount + cashOutAmount
[PaymentService.executePayment()]
    ↓
[CardPaymentService.payWithCashOut()]
    ↓ CardAuthRequest.cashOut(txId, amount, cashOutAmount)
[CardAuthorizationService.authorize()]
    ↓
[VaultService.processTransaction()]
    ↓
[CardAuthResult] 반환
    ↓
[PaymentService.mapToPaymentResult()] → PaymentResult 변환
    ↓
[PaymentProcessor] → 결과 처리
    ↓
[PosViewModel] → UI 업데이트 및 이벤트 발행

# 클래스 구조 다이어그램

┌─────────────────────────────────────────────────────────────────┐
│                        card.dto 패키지                           │
├─────────────────────────────┬───────────────────────────────────┤
│   CardAuthRequest (Record)  │     CardAuthResult (Class)        │
├─────────────────────────────┼───────────────────────────────────┤
│ - amount (필드)             │ - success                         │
│ - currency                  │ - authCode                        │
│ - reference                 │ - responseCode                    │
│ - cashOutAmount             │ - message                         │
│ - originalReference         │ - transactionId                   │
├─────────────────────────────┤ - cardType                        │
│ + forSale()                 │ - maskedCardNumber                │
│ + forCashOut()              │ - approvedAmount                  │
│ + forRefund()               │ - cashOutAmount                   │
│ + hasCashOut()              │ - approvedAt                      │
│ + isRefund()                ├───────────────────────────────────┤
└─────────────────────────────┤ + success()                       │
                              │ + successWithCashOut()            │
                              │ + failure()                       │
                              │ + fromVaultResponse()             │
                              │ + hasCashOut()                    │
                              │ + hasAuthCode()                   │
                              │ + withMessage()                   │
                              └───────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                  CardAuthorizationService                       │
│                                                                 │
│  + authorize(CardAuthRequest): CardAuthResult                   │
│                                                                 │
│  내부 메서드:                                                    │
│    - processNormalSale(CardAuthRequest)                         │
│    - processCashOutSale(CardAuthRequest)                        │
│    - processRefund(CardAuthRequest)                             │
└─────────────────────────────────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                      VaultService                               │
│                   (카드 단말기 연동)                              │
└─────────────────────────────────────────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                      PaymentDialogManager                       │
│  - showCashDialog()                                             │
┌─────────────────────────────────────────────────────────────────┐
│                      PaymentDialogManager                       │
│  - showCashDialog()                                             │
│  - showCreditDialog()  → CardAuthorizationService 주입          │
│  - showCashoutDialog() → CardAuthorizationService 주입          │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│CashDialog     │    │CreditDialog   │    │CashoutDialog  │
│Controller     │    │Controller     │    │Controller     │
│               │    │               │    │               │
│- 현금만 결제    │    │- 현금+카드     │    │- 카드+CashOut  │
│               │    │- CardAuthReq  │    │- CardAuthReq  │
└───────────────┘    └───────────────┘    └───────────────┘
                              │                     │
                              └──────────┬──────────┘
                                         ▼
                          ┌─────────────────────────┐
                          │ CardAuthorizationService │
                          │  + authorize()           │
                          └─────────────────────────┘
                                         │
                                         ▼
                          ┌─────────────────────────┐
                          │      VaultService        │
                          │  + purchase()            │
                          │  + refund()              │
                          └─────────────────────────┘        │
└─────────────────────────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        ▼                     ▼                     ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│CashDialog     │    │CreditDialog   │    │CashoutDialog  │
│Controller     │    │Controller     │    │Controller     │
│               │    │               │    │               │
│- 현금만 결제    │    │- 현금+카드     │    │- 카드+CashOut  │
│               │    │- CardAuthReq  │    │- CardAuthReq  │
└───────────────┘    └───────────────┘    └───────────────┘
                              │                     │
                              └──────────┬──────────┘
                                         ▼
                          ┌─────────────────────────┐
                          │ CardAuthorizationService │
                          │  + authorize()           │
                          └─────────────────────────┘
                                         │
                                         ▼
                          ┌─────────────────────────┐
                          │      VaultService        │
                          │  + purchase()            │
                          │  + refund()              │
                          └─────────────────────────┘

                          ┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CashoutDialogController                                  │
│  cashOutAmount 입력 → handleConfirm() → processPayment()                            │
│                              ↓                                                        │
│              totalCardAmount = totalAfterDiscount + cashOutAmount                    │
│                              ↓                                                        │
│              cardClient.purchaseWithCashOut(totalCardAmount, cashOutAmount)          │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                    CardClient                                         │
│  purchaseWithCashOut(totalCardAmount, cashOutAmount)                                 │
│                              ↓                                                        │
│  if (!posToggleService.isPosEnabled()) return virtualSuccess()  // 테스트 모드       │
│                              ↓                                                        │
│  CardAuthRequest.cashOut(transactionId, amount, cashOutAmount)                       │
│                              ↓                                                        │
│  vaultService.processTransaction(request)                                            │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   VaultService                                        │
│  processTransaction(request)                                                         │
│                              ↓                                                        │
│  doPurchase(request)  // request.hasCashOut() = true                                 │
│                              ↓                                                        │
│  session = getOrCreateSession()                                                      │
│  purchaseTx = new PurchaseTransaction(receiptNo, request.amount())                   │
│  purchaseTx.setCashOutAmount(request.cashOutAmount())  // 🔑 현금인출 설정            │
│                              ↓                                                        │
│  result = session.executeTransaction(purchaseTx)  // 카드 단말 호출                   │
│                              ↓                                                        │
│  return switch(result) {                                                             │
│      Success → CardAuthResult.successWithCashOut(...)                                │
│      Cancelled → CardAuthResult.cancelled()                                          │
│      TimedOut → CardAuthResult.timeout()                                             │
│      default → CardAuthResult.failure(...)                                           │
│  }                                                                                   │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CashoutDialogController (계속)                          │
│  CardAuthResult 반환                                                                 │
│                              ↓                                                        │
│  if (!result.isSuccess()) → handlePaymentFailure() → showError()                    │
│                              ↓                                                        │
│  cardNumber = result.getCardNumber()                                                 │
│                              ↓                                                        │
│  callback.accept(cashOutAmount, totalAfterDiscount, cardNumber)  // TriConsumer     │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   PosViewModel                                        │
│  processCashoutPayment(cashOutAmount, totalCardAmount, cardNumber, onComplete)       │
│                              ↓                                                        │
│  paymentProcessor.processCashoutPayment(...)                                         │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                  PaymentProcessor                                     │
│  processCashoutPayment(cashoutAmount, totalCardAmount, cardNumber, ...)              │
│                              ↓                                                        │
│  validateCashoutPayment(cashoutAmount)  // null 체크, 음수 체크                       │
│                              ↓                                                        │
│  finalCardAmount = resolveCardAmount(totalCardAmount)  // null이면 totalAfterDiscount│
│  payment = PaymentRequestBuilder.cashout(finalCardAmount, cashoutAmount, refNo, cardNumber)│
│                              ↓                                                        │
│  executePayment([payment], "Cashout", successMessage, onComplete, resultHandler)     │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                  PaymentService                                       │
│  executePayment(saleRequest, "Cashout")                                              │
│                              ↓                                                        │
│  metadata = apiEndpointMapper.getMetadata("sale_create")                             │
│                              ↓                                                        │
│  commonApiClient.postForData(metadata, request, Map.of(), Map.of())                  │
│                              ↓                                                        │
│  .map(response → PaymentResult.success(response, BigDecimal.ZERO))                   │
│  .onErrorResume(e → PaymentResult.fail(e.getMessage(), request))                     │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              CashoutDialogController (최종)                          │
│  PaymentResult 반환 (PaymentService → PaymentProcessor → PosViewModel)               │
│                              ↓                                                        │
│  closeDialog()                                                                       │
└─────────────────────────────────────────────────────────────────────────────────────┘
                                              ↓
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                                   PosViewModel                                        │
│  handleProcessedPayment(processed, onComplete)                                       │
│                              ↓                                                        │
│  if (processed.success()) {                                                          │
│      eventPublisher.publishEvent(PaymentSuccessEvent)  // 영수증 출력 트리거          │
│      updateUIBeforeComplete() → clear cart, status update                           │
│      onComplete.accept(true)                                                         │
│  } else {                                                                            │
│      onComplete.accept(false)                                                        │
│  }                                                                                   │
└─────────────────────────────────────────────────────────────────────────────────────┘

CashoutDialogController
    ↓ handleConfirm()
    ↓ processPayment()
    ↓ cardClient.purchaseWithCashOut(totalCardAmount, cashoutAmount)

CardClient
    ↓ purchaseWithCashOut()
    ↓ vaultService.processTransaction(request)

VaultService
    ↓ doPurchase()
    ↓ purchaseTx.setCashOutAmount()
    ↓ session.executeTransaction(purchaseTx)
    ↓ CardAuthResult.successWithCashOut()

CashoutDialogController
    ↓ callback.accept(cashoutAmount, totalAmount, cardNumber)

PosViewModel
    ↓ paymentProcessor.processCashoutPayment()

PaymentProcessor
    ↓ validateCashoutPayment()
    ↓ executePayment([payment], "Cashout")

PaymentService
    ↓ commonApiClient.postForData()
    ↓ PaymentResult.success/fail

PosViewModel
    ↓ handleProcessedPayment()
    ↓ PaymentSuccessEvent 발행
    ↓ clear cart
    ↓ closeDialog()