# SaleRequest 저장 흐름 개요

## 파일명: `sale-storage-flow.md`

---

## 1. 개요

본 문서는 POS 시스템에서 판매(Sale) 요청이 들어왔을 때, `SaleRequest` DTO가 컨트롤러로부터 유스케이스를 거쳐 데이터베이스에 저장되기까지의 전체 흐름을 설명합니다.

### 주요 구성 요소

| 계층               | 구성 요소                           | 책임                                        |
| ------------------ | ----------------------------------- | ------------------------------------------- |
| **Presentation**   | `SaleController`                    | HTTP 요청 수신, 유효성 검증, 응답 반환      |
| **Application**    | `ProcessSaleUseCase`                | 비즈니스 로직 오케스트레이션, 트랜잭션 관리 |
| **Domain**         | `Sale`, `SaleItem`, `PaymentEntity` | 핵심 비즈니스 규칙 및 상태 관리             |
| **Infrastructure** | `SaleRepository`, Mapper, Factory   | 데이터 영속성, 객체 변환, 생성 로직         |

### 기술 스택
   - Spring Boot + Spring Data JPA
   - Jakarta Validation (`@Valid`)
   - Lombok (Record, Builder 패턴)
   - Single Table Inheritance (결제 엔티티)

---

## 2. 전체 저장 흐름

   1. Client → POST /api/v1/sales (JSON 요청)
                     ↓
   2. SaleController.createSale()
      - @Valid로 기본 검증
      - request.validate()로 비즈니스 검증
                     ↓
   3. ProcessSaleUseCase.execute()
      a. validateRequest() - 추가 검증
      b. createSale()
         - createSaleItems() (Product 조회, SaleItem 생성)
         - Sale.create(items)
         - applyDiscountsIfPresent()
         - generateReceiptNumber()
      c. processPayments()
         - PaymentFactory로 PaymentEntity 생성
         - sale.addPayments()
      d. completeSale()
         - sale.validatePayments()
         - sale.complete()
      e. saveAndPublishEvent()
         - saleRepository.save()
         - eventPublisher.publishEvent()
                     ↓
   4. SaleRepository.save()
      - JPA가 INSERT 실행
      - CascadeType.ALL로 인해 SaleItem, PaymentEntity도 함께 저장
                     ↓
   5. Client ← 201 Created (SaleResponse)


# ERD 개요
   ┌─────────────┐     ┌─────────────┐     ┌─────────────────┐
   │   sales     │     │ sale_items  │     │    payments     │
   ├─────────────┤     ├─────────────┤     ├─────────────────┤
   │ id (PK)     │◄────│ sale_id (FK)│     │ id (PK)         │
   │ receipt_no  │     │ product_id  │     │ sale_id (FK)    │
   │ status      │     │ barcode     │     │ amount          │
   │ total_amount│     │ quantity    │     │ payment_type(DT)│
   │ discount_amt│     │ price_at_sale│    │ approval_no     │
   │ final_amount│     │ discount_val │    │ received_amount │
   │ sale_date   │     │ discount_type│    │ change_amount   │
   └─────────────┘     └─────────────┘     └─────────────────┘


# 저장 시점 쿼리 순서
INSERT INTO sales (Sale 기본 정보)

INSERT INTO sale_items (각 아이템)

INSERT INTO payments (결제 정보, payment_type에 따라 필요한 컬럼만 저장)

# 예외 처리
상황	               예외 타입	                  HTTP 응답
Product 없음	      IllegalArgumentException	 400 Bad Request
결제 금액 불일치	     IllegalStateException	      400 Bad Request
할인율 > 100%	       IllegalArgumentException	  400 Bad Request
승인번호 누락(CARD)	  IllegalArgumentException	   400 Bad Request
이미 완료된 주문 취소	IllegalStateException	    409 Conflict
동일 영수증 번호	     DataIntegrityViolation	   500 Internal Error


## 1. 전체 폴더 구조



# 프로젝트 폴더 구조

src/main/java/com/swna/server/
│
├── sale/
│ ├── controller/
│ │ └── SaleController.java
│ │
│ ├── dto/
│ │ ├── request/
│ │ │ ├── SaleRequest.java
│ │ │ ├── SaleItemRequest.java
│ │ │ ├── PaymentRequest.java
│ │ │ └── DiscountRequest.java
│ │ │
│ │ └── response/
│ │ ├── SaleResponse.java
│ │ ├── SaleItemResponse.java
│ │ └── PaymentResponse.java
│ │
│ ├── entity/
│ │ ├── Sale.java
│ │ ├── SaleItem.java
│ │ ├── SaleStatus.java
│ │ ├── PaymentEntity.java
│ │ ├── CashPaymentEntity.java
│ │ ├── CardPaymentEntity.java
│ │ ├── PaymentType.java
│ │ ├── Discount.java
│ │ └── DiscountType.java
│ │
│ ├── repository/
│ │ └── SaleRepository.java
│ │
│ ├── mapper/
│ │ ├── SaleMapper.java
│ │ ├── SaleItemMapper.java
│ │ └── PaymentMapper.java
│ │
│ ├── factory/
│ │ └── PaymentFactory.java
│ │
│ ├── usecase/
│ │ └── ProcessSaleUseCase.java
│ │
│ └── event/
│ └── SaleCompletedEvent.java
│
├── product/
│ ├── entity/
│ │ └── Product.java
│ └── repository/
│ └── ProductRepository.java
│
└── common/
├── entity/
│ └── BaseEntity.java
└── config/
└── (기타 공통 설정)

