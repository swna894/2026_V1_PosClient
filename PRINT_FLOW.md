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