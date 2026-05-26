-- 1. 현재 컬럼 구조 확인
DESC product_stocks;

-- 2. minOrderQuantity 컬럼의 기본값 확인
SHOW COLUMNS FROM product_stocks LIKE 'min_order_quantity';

-- 3. 기본값 설정 (JPA 설정과 맞춤)
ALTER TABLE product_stocks 
MODIFY COLUMN min_order_quantity INT NOT NULL DEFAULT 12;


# 외래 키 조건 삭제 

-- 1. 외래 키 제약 조건 일시적 비활성화 (MySQL)
SET FOREIGN_KEY_CHECKS = 0;

-- 2. sales 테이블 TRUNCATE
TRUNCATE TABLE sales;

-- 3. sale_items 테이블 TRUNCATE
TRUNCATE TABLE sale_items;

-- 4. 외래 키 제약 조건 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;


# 테이블 삭제 방법 
SET FOREIGN_KEY_CHECKS = 0;

-- 2. 5개 테이블 완전히 삭제
DROP TABLE IF EXISTS `discounts`;
DROP TABLE IF EXISTS `category`;
DROP TABLE IF EXISTS `invoice`;
DROP TABLE IF EXISTS `invoice_seq`;
DROP TABLE IF EXISTS `invoicedetails`;
DROP TABLE IF EXISTS `invoicedetails_seq`;
DROP TABLE IF EXISTS `sale_discount`;
DROP TABLE IF EXISTS `sale_discount_seq`;

-- 3. 외래 키 체크 다시 활성화
SET FOREIGN_KEY_CHECKS = 1;