-- 1. 현재 컬럼 구조 확인
DESC product_stocks;

-- 2. minOrderQuantity 컬럼의 기본값 확인
SHOW COLUMNS FROM product_stocks LIKE 'min_order_quantity';

-- 3. 기본값 설정 (JPA 설정과 맞춤)
ALTER TABLE product_stocks 
MODIFY COLUMN min_order_quantity INT NOT NULL DEFAULT 12;