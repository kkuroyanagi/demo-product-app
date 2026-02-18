-- products テーブル（商品マスタ）
CREATE TABLE products (
    id            BIGSERIAL PRIMARY KEY,
    product_code  VARCHAR(20)  NOT NULL UNIQUE,
    product_name  VARCHAR(200) NOT NULL,
    category      VARCHAR(50)  NOT NULL,
    price         NUMERIC(12,2) NOT NULL DEFAULT 0,
    stock_quantity INTEGER      NOT NULL DEFAULT 0,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    description   TEXT,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- インデックス
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_status   ON products(status);
CREATE INDEX idx_products_name     ON products(product_name);

-- ステータス制約
ALTER TABLE products
    ADD CONSTRAINT chk_status
    CHECK (status IN ('ACTIVE', 'INACTIVE', 'DISCONTINUED'));

COMMENT ON TABLE  products IS '商品マスタ';
COMMENT ON COLUMN products.product_code   IS '商品コード';
COMMENT ON COLUMN products.product_name   IS '商品名';
COMMENT ON COLUMN products.category       IS 'カテゴリ';
COMMENT ON COLUMN products.price          IS '単価';
COMMENT ON COLUMN products.stock_quantity IS '在庫数量';
COMMENT ON COLUMN products.status         IS 'ステータス (ACTIVE/INACTIVE/DISCONTINUED)';

-- categories テーブル（カテゴリマスタ）
CREATE TABLE categories (
    id            SERIAL PRIMARY KEY,
    category_code VARCHAR(20)  NOT NULL UNIQUE,
    category_name VARCHAR(100) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0
);

COMMENT ON TABLE  categories IS 'カテゴリマスタ';
COMMENT ON COLUMN categories.category_code IS 'カテゴリコード';
COMMENT ON COLUMN categories.category_name IS 'カテゴリ名';
COMMENT ON COLUMN categories.sort_order    IS '表示順';
