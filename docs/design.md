# デモ用Webアプリケーション 設計書

## 1. システム概要

### 1.1 目的

データベース（PostgreSQL）に格納された数千件規模のデータを、Web画面上で検索・一覧表示・並び替え・フィルター・ページネーション・Excelダウンロード/アップロードによる一括更新ができるデモ用Webアプリケーション。

### 1.2 技術スタック

| レイヤー | 技術 | バージョン目安 |
|---|---|---|
| フロントエンド | React 19 + TypeScript | 19.x |
| UIフレームワーク | Ant Design 5 + @ant-design/pro-components（ProTable） | 5.x / 2.x |
| React 19互換パッチ | @ant-design/v5-patch-for-react-19 | 1.x |
| ルーティング | React Router | 7.x |
| HTTPクライアント | Axios | 1.x |
| ビルドツール | Vite | 6.x |
| バックエンド | Spring Boot (Java 21) | 3.3.x |
| ORM | Spring Data JPA (Hibernate) | — |
| Excel処理 | Apache POI | 5.x |
| データベース | PostgreSQL | 16.x |
| コンテナ | Docker / Docker Compose | — |

> **React 19 互換性について**
>
> Ant Design v5 は React 16〜18 をデフォルトでサポートしており、React 19 では一部の静的メソッド（`Modal.confirm`、`message.success` 等）が動作しない問題がある。公式互換パッケージ `@ant-design/v5-patch-for-react-19` を導入することで解決する。Pro Components（ProTable等）は antd v5 ベースの 2.x 系を使用する。

### 1.3 全体アーキテクチャ

```
┌─────────────────────────────────────────────────────┐
│                    Browser                          │
│  ┌───────────────────────────────────────────────┐  │
│  │  React + Ant Design Pro (ProTable)            │  │
│  │  ┌──────────┐  ┌───────────────────────────┐  │  │
│  │  │ SideMenu │  │ SearchForm + ResultTable  │  │  │
│  │  │          │  │                           │  │  │
│  │  └──────────┘  └───────────────────────────┘  │  │
│  └───────────────────────────────────────────────┘  │
└───────────────────┬─────────────────────────────────┘
                    │ REST API (JSON)
                    │ multipart/form-data (Upload)
                    ▼
┌─────────────────────────────────────────────────────┐
│              Spring Boot Application                │
│  ┌────────────┐  ┌────────────┐  ┌──────────────┐  │
│  │ Controller │→ │  Service   │→ │ Repository   │  │
│  │  (REST)    │  │  (業務)    │  │  (JPA)       │  │
│  └────────────┘  └────────────┘  └──────┬───────┘  │
│  ┌────────────────────────────┐         │          │
│  │  Excel Service (POI)      │         │          │
│  └────────────────────────────┘         │          │
└─────────────────────────────────────────┼──────────┘
                                          │ JDBC
                                          ▼
                                ┌──────────────────┐
                                │   PostgreSQL     │
                                │   (products等)   │
                                └──────────────────┘
```

---

## 2. データベース設計

### 2.1 サンプルテーブル: `products`（商品マスタ）

デモ用として商品マスタを題材とする。

```sql
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
```

### 2.2 カテゴリマスタ: `categories`

```sql
CREATE TABLE categories (
    id            SERIAL PRIMARY KEY,
    category_code VARCHAR(20)  NOT NULL UNIQUE,
    category_name VARCHAR(100) NOT NULL,
    sort_order    INTEGER      NOT NULL DEFAULT 0
);
```

### 2.3 初期データ

デモ用に約3,000件の商品データを `data.sql` で投入する。カテゴリは「電子機器」「食品」「衣類」「書籍」「日用品」の5種を用意。

---

## 3. API設計

### 3.1 エンドポイント一覧

| メソッド | パス | 説明 |
|---|---|---|
| GET | `/api/products` | 商品一覧検索（ページネーション・ソート・フィルター対応） |
| GET | `/api/products/{id}` | 商品詳細取得 |
| POST | `/api/products` | 商品新規登録 |
| PUT | `/api/products/{id}` | 商品更新 |
| DELETE | `/api/products/{id}` | 商品削除 |
| GET | `/api/products/export` | Excel一括ダウンロード（検索条件付き） |
| POST | `/api/products/import` | Excelアップロード一括更新 |
| GET | `/api/categories` | カテゴリ一覧取得 |

### 3.2 検索API詳細: `GET /api/products`

#### リクエストパラメータ

| パラメータ | 型 | 必須 | 説明 |
|---|---|---|---|
| `keyword` | string | — | 商品名・商品コードの部分一致検索 |
| `category` | string | — | カテゴリ完全一致 |
| `status` | string | — | ステータス完全一致 |
| `priceMin` | number | — | 単価下限 |
| `priceMax` | number | — | 単価上限 |
| `current` | number | — | 現在ページ（デフォルト: 1） |
| `pageSize` | number | — | 1ページ件数（デフォルト: 20, 最大: 100） |
| `sorter` | string | — | ソートカラムと方向（例: `price,asc`） |

#### レスポンス（ProTable互換形式）

```json
{
  "data": [
    {
      "id": 1,
      "productCode": "PRD-0001",
      "productName": "ワイヤレスマウス",
      "category": "電子機器",
      "price": 3980.00,
      "stockQuantity": 150,
      "status": "ACTIVE",
      "description": "...",
      "createdAt": "2025-01-15T10:30:00",
      "updatedAt": "2025-01-15T10:30:00"
    }
  ],
  "total": 3000,
  "success": true,
  "current": 1,
  "pageSize": 20
}
```

### 3.3 Excelダウンロード: `GET /api/products/export`

検索条件パラメータは検索APIと同一（`current`/`pageSize`除く）。条件に合致する全件をExcelとしてダウンロードする。

レスポンス: `Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`

### 3.4 Excelアップロード: `POST /api/products/import`

リクエスト: `Content-Type: multipart/form-data`

| パラメータ | 型 | 説明 |
|---|---|---|
| `file` | file | Excelファイル (.xlsx) |

#### レスポンス

```json
{
  "success": true,
  "totalRows": 150,
  "insertedCount": 10,
  "updatedCount": 130,
  "errorCount": 10,
  "errors": [
    {
      "row": 25,
      "field": "price",
      "message": "単価は0以上の数値を指定してください"
    }
  ]
}
```

#### アップロード処理ルール

- `product_code` をキーとして既存レコードの存否を判定する
- 既存 → UPDATE、新規 → INSERT
- バリデーションエラーがある行はスキップし、正常行のみ処理する
- 処理結果（成功件数・エラー詳細）をレスポンスで返却する
- トランザクション: 正常行は一括コミット、エラー行はスキップ（部分成功方式）

---

## 4. バックエンド設計

### 4.1 パッケージ構成

```
src/main/java/com/example/demo/
├── DemoApplication.java
├── config/
│   ├── WebConfig.java              # CORS設定
│   └── JacksonConfig.java          # 日付フォーマット等
├── controller/
│   ├── ProductController.java
│   └── CategoryController.java
├── service/
│   ├── ProductService.java
│   ├── ProductServiceImpl.java
│   ├── ExcelService.java
│   └── ExcelServiceImpl.java
├── repository/
│   ├── ProductRepository.java      # Spring Data JPA
│   └── ProductSpecification.java   # 動的検索条件
├── entity/
│   ├── Product.java
│   └── Category.java
├── dto/
│   ├── ProductSearchRequest.java   # 検索リクエスト
│   ├── ProductResponse.java        # レスポンス
│   ├── PageResponse.java           # ページネーション汎用
│   └── ImportResult.java           # アップロード結果
└── exception/
    ├── GlobalExceptionHandler.java
    └── BusinessException.java
```

### 4.2 主要クラス設計

#### Product Entity

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    @Column(nullable = false)
    private String status;

    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

#### ProductSpecification（動的検索）

JPA Specification を用いて検索条件を動的に組み立てる。

```java
public class ProductSpecification {
    public static Specification<Product> search(ProductSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.getKeyword())) {
                String kw = "%" + req.getKeyword() + "%";
                predicates.add(cb.or(
                    cb.like(root.get("productName"), kw),
                    cb.like(root.get("productCode"), kw)
                ));
            }
            if (StringUtils.hasText(req.getCategory())) {
                predicates.add(cb.equal(root.get("category"), req.getCategory()));
            }
            if (StringUtils.hasText(req.getStatus())) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getPriceMin() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), req.getPriceMin()));
            }
            if (req.getPriceMax() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), req.getPriceMax()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
```

#### ExcelService（ダウンロード・アップロード）

**ダウンロード処理フロー:**

1. 検索条件で全件取得（ページネーション無し）
2. Apache POI で `SXSSFWorkbook`（ストリーミング書き込み）を使用
3. ヘッダー行 → データ行を順次書き込み
4. レスポンスの OutputStream に直接書き出し

**Excelカラムマッピング:**

| Excel列 | DBカラム | 必須 | 備考 |
|---|---|---|---|
| A: 商品コード | product_code | ○ | キー項目（更新/新規判定） |
| B: 商品名 | product_name | ○ | |
| C: カテゴリ | category | ○ | |
| D: 単価 | price | ○ | 数値 |
| E: 在庫数量 | stock_quantity | ○ | 整数 |
| F: ステータス | status | ○ | ACTIVE/INACTIVE/DISCONTINUED |
| G: 説明 | description | — | |

**アップロード処理フロー:**

1. マルチパートでExcelファイルを受信
2. Apache POI でワークブックを読み込み
3. 1行目をヘッダーとしてスキップ
4. 2行目以降を1行ずつ読み取り
5. 行ごとにバリデーション（必須チェック、型チェック、値域チェック）
6. `product_code` で既存レコードを検索 → 存在すれば UPDATE、なければ INSERT
7. エラー行はスキップしてエラーリストに蓄積
8. 正常行を一括保存（`saveAll`）
9. 処理結果を返却

---

## 5. フロントエンド設計

### 5.1 ディレクトリ構成

```
src/
├── main.tsx                        # エントリーポイント（React 19パッチ適用）
├── App.tsx                         # ルーティング定義
├── layouts/
│   └── BasicLayout.tsx             # サイドメニュー + コンテンツ領域
├── pages/
│   ├── products/
│   │   ├── index.tsx               # 商品一覧ページ（メイン）
│   │   └── components/
│   │       ├── SearchForm.tsx      # 検索条件フォーム
│   │       ├── ProductTable.tsx    # ProTable 一覧表
│   │       └── ImportModal.tsx     # Excelアップロードモーダル
│   └── dashboard/
│       └── index.tsx               # ダッシュボード（プレースホルダー）
├── services/
│   └── productService.ts           # API呼び出し関数
├── types/
│   └── product.ts                  # 型定義
└── utils/
    └── request.ts                  # Axiosインスタンス設定
```

### 5.2 React 19 + Ant Design 初期化

React 19 では `ReactDOM.render` が廃止され `createRoot` が標準となっている。Ant Design v5 の静的メソッド（`Modal.confirm` 等）を動作させるため、エントリーポイントでパッチを適用する。

```typescript
// main.tsx
import React from 'react';
import { createRoot } from 'react-dom/client';
import '@ant-design/v5-patch-for-react-19';
import App from './App';

const root = createRoot(document.getElementById('root')!);
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
```

**主な依存パッケージ（package.json）:**

```json
{
  "dependencies": {
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "react-router-dom": "^7.0.0",
    "antd": "^5.23.0",
    "@ant-design/pro-components": "^2.8.0",
    "@ant-design/v5-patch-for-react-19": "^1.0.0",
    "@ant-design/icons": "^5.6.0",
    "axios": "^1.7.0",
    "dayjs": "^1.11.0"
  },
  "devDependencies": {
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "typescript": "^5.7.0",
    "vite": "^6.0.0",
    "@vitejs/plugin-react": "^4.0.0"
  }
}
```

### 5.3 画面レイアウト

```
┌──────────────────────────────────────────────────────────────┐
│  ヘッダー（アプリ名: Demo App）                                │
├──────────┬───────────────────────────────────────────────────┤
│          │  パンくずリスト: ホーム > 商品管理 > 商品一覧        │
│  サイド   │ ┌─────────────────────────────────────────────┐   │
│  メニュー │ │ 検索条件エリア（折りたたみ可能）                │   │
│          │ │ ┌────────┐ ┌────────┐ ┌─────────────────┐  │   │
│ ・ダッシュ │ │ │キーワード│ │カテゴリ │ │ステータス        │  │   │
│   ボード  │ │ └────────┘ └────────┘ └─────────────────┘  │   │
│ ▼商品管理 │ │ ┌────────┐ ┌────────┐                      │   │
│  ・商品   │ │ │単価(Min)│ │単価(Max)│  [検索] [リセット]  │   │
│   一覧   │ │ └────────┘ └────────┘                      │   │
│          │ └─────────────────────────────────────────────┘   │
│          │                                                   │
│          │ ツールバー:  [Excelダウンロード] [Excelアップロード] │
│          │                                                   │
│          │ ┌─────────────────────────────────────────────┐   │
│          │ │ ProTable 一覧表                              │   │
│          │ │ ┌──┬──────┬──────┬────┬────┬───┬──────┐    │   │
│          │ │ │No│商品CD│商品名│分類│単価│在庫│ｽﾃｰﾀｽ│    │   │
│          │ │ │──┼──────┼──────┼────┼────┼───┼──────│    │   │
│          │ │ │ 1│PRD.. │ﾜｲﾔﾚ..│電子│3980│150│有効  │    │   │
│          │ │ │ 2│PRD.. │...   │... │... │...│...   │    │   │
│          │ │ └──┴──────┴──────┴────┴────┴───┴──────┘    │   │
│          │ │                                              │   │
│          │ │ ◀ 1 2 3 ... 150 ▶  表示件数: [20▼]         │   │
│          │ └─────────────────────────────────────────────┘   │
└──────────┴───────────────────────────────────────────────────┘
```

### 5.4 ProTable カラム定義

```typescript
const columns: ProColumns<Product>[] = [
  {
    title: 'No',
    dataIndex: 'index',
    valueType: 'indexBorder',
    width: 60,
  },
  {
    title: '商品コード',
    dataIndex: 'productCode',
    sorter: true,
    width: 140,
    copyable: true,
  },
  {
    title: '商品名',
    dataIndex: 'productName',
    sorter: true,
    ellipsis: true,
    width: 250,
  },
  {
    title: 'カテゴリ',
    dataIndex: 'category',
    filters: true,
    valueEnum: categoryEnum,  // APIから取得したカテゴリマスタ
    width: 120,
  },
  {
    title: '単価',
    dataIndex: 'price',
    sorter: true,
    valueType: 'money',
    width: 120,
    align: 'right',
  },
  {
    title: '在庫数量',
    dataIndex: 'stockQuantity',
    sorter: true,
    width: 100,
    align: 'right',
  },
  {
    title: 'ステータス',
    dataIndex: 'status',
    filters: true,
    valueEnum: {
      ACTIVE:       { text: '有効',   status: 'Success'  },
      INACTIVE:     { text: '無効',   status: 'Default'  },
      DISCONTINUED: { text: '廃止',   status: 'Error'    },
    },
    width: 100,
  },
  {
    title: '更新日時',
    dataIndex: 'updatedAt',
    sorter: true,
    valueType: 'dateTime',
    width: 180,
  },
];
```

### 5.5 ProTable リクエスト関数

ProTableの `request` プロパティに渡す関数。ProTableのパラメータをSpring Boot APIのフォーマットに変換する。React 19 の `use` hook は ProTable 内部では使用せず、従来の `async/await` パターンで統一する（ProTable が内部で状態管理を行うため）。

```typescript
const fetchProducts = async (
  params: ParamsType & { current?: number; pageSize?: number },
  sorter: Record<string, SortOrder>,
  filter: Record<string, React.ReactText[] | null>,
) => {
  // ソートパラメータ変換
  const sorterKey = Object.keys(sorter)[0];
  const sorterParam = sorterKey
    ? `${sorterKey},${sorter[sorterKey] === 'ascend' ? 'asc' : 'desc'}`
    : undefined;

  // フィルターをパラメータに統合
  const response = await productService.search({
    keyword: params.keyword,
    category: filter.category?.[0] as string,
    status: filter.status?.[0] as string,
    priceMin: params.priceMin,
    priceMax: params.priceMax,
    current: params.current,
    pageSize: params.pageSize,
    sorter: sorterParam,
  });

  return {
    data: response.data,
    total: response.total,
    success: response.success,
  };
};
```

### 5.6 Excelダウンロード処理

```typescript
const handleExport = async () => {
  const response = await productService.exportExcel(currentSearchParams);
  const blob = new Blob([response.data], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `products_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`;
  a.click();
  window.URL.revokeObjectURL(url);
};
```

### 5.7 Excelアップロード処理

ImportModal コンポーネントで `Dragger`（ドラッグ&ドロップ）対応のアップロードUIを提供する。

```
┌─────────────────────────────────────────┐
│         Excelアップロード               │
│ ┌─────────────────────────────────────┐ │
│ │                                     │ │
│ │   📄 ここにExcelファイルをドロップ    │ │
│ │   またはクリックして選択             │ │
│ │                                     │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ 対応フォーマット: .xlsx                  │
│ テンプレート: [ダウンロード]             │
│                                         │
│ ─── アップロード結果 ───                │
│ ✅ 成功: 140件  ❌ エラー: 10件         │
│                                         │
│ エラー詳細:                              │
│ ┌──────┬────────┬──────────────────┐   │
│ │ 行番号│ 項目    │ エラー内容       │   │
│ │ 25   │ 単価    │ 数値を指定して…  │   │
│ │ 48   │ ｽﾃｰﾀｽ  │ 不正な値です     │   │
│ └──────┴────────┴──────────────────┘   │
│                          [閉じる]       │
└─────────────────────────────────────────┘
```

---

## 6. サイドメニュー構成

```typescript
const menuItems = [
  {
    path: '/dashboard',
    name: 'ダッシュボード',
    icon: <DashboardOutlined />,
  },
  {
    path: '/products',
    name: '商品管理',
    icon: <ShoppingOutlined />,
    children: [
      {
        path: '/products/list',
        name: '商品一覧',
        icon: <UnorderedListOutlined />,
      },
    ],
  },
];
```

拡張性を考慮し、メニューは配列で定義。将来的にカテゴリ管理、ユーザー管理等を追加可能。

---

## 7. バリデーション仕様

### 7.1 検索条件バリデーション

| 項目 | ルール |
|---|---|
| キーワード | 最大100文字 |
| 単価 (Min/Max) | 0以上の数値、Min ≤ Max |
| pageSize | 1〜100 |

### 7.2 Excelアップロード バリデーション

| 項目 | ルール |
|---|---|
| 商品コード | 必須、最大20文字、半角英数+ハイフン |
| 商品名 | 必須、最大200文字 |
| カテゴリ | 必須、カテゴリマスタに存在すること |
| 単価 | 必須、0以上の数値 |
| 在庫数量 | 必須、0以上の整数 |
| ステータス | 必須、ACTIVE/INACTIVE/DISCONTINUEDのいずれか |
| ファイル形式 | .xlsx のみ受付 |
| ファイルサイズ | 最大10MB |

---

## 8. 非機能要件

### 8.1 パフォーマンス

- データ量: 数千件規模（最大10,000件を想定）
- 検索応答: 500ms以内（インデックス活用）
- Excelダウンロード: SXSSFWorkbook によるストリーミング書き込み
- Excelアップロード: 一括 `saveAll` でDB書き込みを効率化

### 8.2 エラーハンドリング

- バックエンド: `@ControllerAdvice` による統一的な例外ハンドリング
- フロントエンド: Axios インターセプターでエラー通知（Ant Design `message` コンポーネント）
- アップロード: 行単位エラー収集、部分成功方式

### 8.3 CORS設定

開発時はフロントエンド（Vite: `localhost:5173`）→ バックエンド（`localhost:8080`）間のCORSを許可。

---

## 9. 開発環境構成 (Docker Compose)

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_DB: demo
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./db/init:/docker-entrypoint-initdb.d   # DDL + 初期データ

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/demo
    depends_on:
      - db

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"
    depends_on:
      - backend

volumes:
  postgres_data:
```

---

## 10. ディレクトリ全体構成

```
project-root/
├── docker-compose.yml
├── db/
│   └── init/
│       ├── 01_ddl.sql              # テーブル作成
│       └── 02_data.sql             # 初期データ投入
├── backend/
│   ├── Dockerfile
│   ├── build.gradle                # or pom.xml
│   └── src/
│       └── main/
│           ├── java/com/example/demo/
│           │   └── (4.1 パッケージ構成参照)
│           └── resources/
│               └── application.yml
└── frontend/
    ├── Dockerfile
    ├── package.json
    ├── vite.config.ts
    ├── tsconfig.json
    └── src/
        └── (5.1 ディレクトリ構成参照)
```

---

## 11. 処理フロー

### 11.1 検索 → 一覧表示フロー

```
[ユーザー] 検索条件入力 → [検索]ボタンクリック
    ↓
[ProTable] request関数呼び出し → params, sorter, filter をAPI形式に変換
    ↓
[Axios] GET /api/products?keyword=...&current=1&pageSize=20
    ↓
[Controller] @GetMapping → ProductSearchRequest にバインド
    ↓
[Service] Specification 生成 → Repository.findAll(spec, pageable)
    ↓
[Repository] JPA → SQL実行 → Page<Product> 返却
    ↓
[Controller] PageResponse に変換 → JSON返却
    ↓
[ProTable] data, total を受け取って表を再描画
```

### 11.2 Excelアップロード → DB更新フロー

```
[ユーザー] アップロードモーダル → ファイルドロップ
    ↓
[Axios] POST /api/products/import (multipart/form-data)
    ↓
[Controller] @PostMapping → MultipartFile 受信
    ↓
[ExcelService] POIで読み込み → 行ごとバリデーション
    ↓  正常行リスト / エラー行リスト
[ProductService] product_code で既存検索
    ↓  既存あり → UPDATE / なし → INSERT
[Repository] saveAll(正常行リスト) → 一括コミット
    ↓
[Controller] ImportResult(成功数, エラー数, エラー詳細) → JSON返却
    ↓
[ImportModal] 結果を画面に表示
    ↓
[ProTable] actionRef.current.reload() → 最新データで再表示
```

---

## 12. 今後の拡張案

- 認証・認可の追加（Spring Security + JWT）
- 行内編集（ProTable の `editable` 機能）
- 商品詳細画面（ドロワー or 別ページ）
- 削除機能（論理削除）
- 操作ログ・監査ログ
- E2Eテスト（Playwright）
