# GitHub プロジェクト構成ガイド

## 1. リポジトリ戦略: モノレポ（推奨）

### なぜモノレポか

| 観点 | モノレポ（単一リポジトリ） | マルチレポ（分割） |
|---|---|---|
| 規模感 | ✅ デモ〜中規模に最適 | 大規模マイクロサービス向き |
| フロント⇔バック連携 | ✅ API変更時に一度にコミットできる | 別々にPR管理が必要 |
| Docker Compose | ✅ 1リポジトリで完結 | compose.ymlの置き場所に困る |
| CI/CD | ✅ 一つのパイプラインで管理 | 各リポジトリに個別設定 |
| オンボーディング | ✅ clone 1回で開発開始 | 複数リポジトリをセットアップ |
| コードレビュー | ✅ API変更を一つのPRで確認可能 | 変更の全体像が見えにくい |

デモ用途で開発者も少人数であれば、モノレポが圧倒的にシンプル。

---

## 2. ディレクトリ構成

```
demo-product-app/
│
├── .github/
│   ├── workflows/
│   │   ├── ci.yml                    # CI: テスト・ビルド
│   │   └── cd.yml                    # CD: デプロイ（必要時）
│   ├── ISSUE_TEMPLATE/
│   │   ├── bug_report.md
│   │   └── feature_request.md
│   └── pull_request_template.md
│
├── backend/
│   ├── build.gradle                  # Gradle設定（またはpom.xml）
│   ├── settings.gradle
│   ├── Dockerfile
│   ├── .env.example                  # 環境変数テンプレート
│   └── src/
│       ├── main/
│       │   ├── java/com/example/demo/
│       │   │   ├── DemoApplication.java
│       │   │   ├── config/
│       │   │   ├── controller/
│       │   │   ├── service/
│       │   │   ├── repository/
│       │   │   ├── entity/
│       │   │   ├── dto/
│       │   │   └── exception/
│       │   └── resources/
│       │       ├── application.yml
│       │       └── application-local.yml
│       └── test/
│           └── java/com/example/demo/
│               ├── controller/
│               ├── service/
│               └── repository/
│
├── frontend/
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   ├── Dockerfile
│   ├── .env.example
│   └── src/
│       ├── main.tsx
│       ├── App.tsx
│       ├── layouts/
│       ├── pages/
│       ├── services/
│       ├── types/
│       └── utils/
│
├── db/
│   └── init/
│       ├── 01_ddl.sql                # テーブル定義
│       ├── 02_master_data.sql        # マスタデータ
│       └── 03_sample_data.sql        # デモ用サンプルデータ
│
├── docs/
│   ├── design.md                     # 設計書（前回作成済み）
│   ├── api-spec.md                   # API仕様詳細
│   └── setup-guide.md               # 環境構築手順
│
├── docker-compose.yml                # ローカル開発用
├── docker-compose.prod.yml           # 本番用（必要時）
├── .gitignore
├── .editorconfig
├── README.md
└── LICENSE
```

---

## 3. `.gitignore` 設定

```gitignore
# === Java / Spring Boot ===
backend/build/
backend/.gradle/
backend/bin/
backend/out/
*.class
*.jar
*.war

# === Node.js / React ===
frontend/node_modules/
frontend/dist/
frontend/.vite/

# === IDE ===
.idea/
.vscode/
*.iml
*.swp
.DS_Store

# === Environment ===
.env
*.env.local

# === Docker ===
postgres_data/

# === OS ===
Thumbs.db
```

---

## 4. ブランチ戦略

デモ用の少人数開発には GitHub Flow をベースとしたシンプルな運用を推奨。

```
main（本番相当・常にデプロイ可能）
 │
 ├── feature/BE-001-product-entity      ← バックエンド機能
 ├── feature/FE-001-product-table       ← フロントエンド機能
 ├── feature/DB-001-initial-schema      ← DB変更
 ├── fix/BE-010-search-null-handling    ← バグ修正
 └── docs/update-api-spec              ← ドキュメント
```

### ブランチ命名規則

```
{種別}/{対象}-{連番}-{概要（kebab-case）}
```

| 種別 | 用途 | 例 |
|---|---|---|
| `feature/` | 新機能 | `feature/BE-001-product-entity` |
| `fix/` | バグ修正 | `fix/FE-005-pagination-reset` |
| `refactor/` | リファクタリング | `refactor/BE-003-service-layer` |
| `docs/` | ドキュメント | `docs/api-spec-update` |
| `chore/` | 設定・依存更新 | `chore/upgrade-spring-boot` |

### 対象プレフィックス

| プレフィックス | 対象 |
|---|---|
| `BE` | バックエンド |
| `FE` | フロントエンド |
| `DB` | データベース |
| `INF` | インフラ・Docker |
| なし | 横断的な変更 |

---

## 5. GitHub Issues によるタスク管理

### 5.1 ラベル設計

| ラベル | 色 | 用途 |
|---|---|---|
| `backend` | 🔵 Blue | バックエンドタスク |
| `frontend` | 🟢 Green | フロントエンドタスク |
| `database` | 🟠 Orange | DB関連 |
| `infra` | 🟣 Purple | Docker・CI/CD |
| `docs` | ⚪ Gray | ドキュメント |
| `bug` | 🔴 Red | バグ |
| `enhancement` | 🟡 Yellow | 機能追加 |
| `good first issue` | 🩵 Teal | 着手しやすいタスク |

### 5.2 マイルストーン案

開発をフェーズ分けして段階的に進める。

| マイルストーン | 内容 | 主なIssue |
|---|---|---|
| **Phase 1: 基盤構築** | プロジェクト初期化・DB・Docker | #1〜#5 |
| **Phase 2: バックエンド** | API実装（CRUD・検索・ページネーション） | #6〜#12 |
| **Phase 3: フロントエンド** | ProTable一覧・検索・ソート・フィルター | #13〜#18 |
| **Phase 4: Excel機能** | ダウンロード・アップロード・一括更新 | #19〜#22 |
| **Phase 5: 仕上げ** | テスト・ドキュメント・デモデータ調整 | #23〜#25 |

### 5.3 Issue 一覧（初期起票案）

#### Phase 1: 基盤構築

```
#1  [INF] Docker Compose 環境構築（PostgreSQL + Spring Boot + React）
#2  [DB]  テーブル定義（DDL）作成
#3  [DB]  初期データ・サンプルデータ投入スクリプト作成
#4  [BE]  Spring Boot プロジェクト初期化（依存定義・設定ファイル）
#5  [FE]  React 19 + Vite プロジェクト初期化（Ant Design + ProTable導入）
```

#### Phase 2: バックエンド

```
#6  [BE]  Entity / Repository 定義（Product, Category）
#7  [BE]  商品検索API（GET /api/products）- ページネーション・ソート・動的フィルター
#8  [BE]  商品CRUD API（POST / PUT / DELETE）
#9  [BE]  カテゴリ一覧API（GET /api/categories）
#10 [BE]  CORS設定・グローバル例外ハンドラー
#11 [BE]  Excelダウンロード API（GET /api/products/export）
#12 [BE]  Excelアップロード API（POST /api/products/import）
```

#### Phase 3: フロントエンド

```
#13 [FE]  BasicLayout（サイドメニュー + コンテンツ領域）
#14 [FE]  ProTable 商品一覧表示（カラム定義・request連携）
#15 [FE]  検索フォーム（キーワード・カテゴリ・ステータス・単価範囲）
#16 [FE]  ページネーション・ソート・カラムフィルター
#17 [FE]  Excelダウンロードボタン実装
#18 [FE]  Excelアップロードモーダル実装（結果表示含む）
```

#### Phase 4: Excel機能結合

```
#19 [BE+FE] Excelダウンロード結合テスト・動作確認
#20 [BE+FE] Excelアップロード結合テスト・エラーハンドリング確認
#21 [BE]    アップロード バリデーション強化
#22 [FE]    アップロード結果のエラー詳細表示改善
```

#### Phase 5: 仕上げ

```
#23 [BE]   単体テスト作成（Service / Controller）
#24 [DOCS] README・セットアップガイド整備
#25 [INF]  デモ用サンプルデータ最終調整（3,000件）
```

---

## 6. GitHub Actions CI 設定例

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  # --- バックエンド ---
  backend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: backend

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: demo_test
          POSTGRES_USER: demo
          POSTGRES_PASSWORD: demo
        ports:
          - 5432:5432
        options: >-
          --health-cmd "pg_isready -U demo"
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: gradle-${{ hashFiles('backend/**/*.gradle*') }}

      - name: Build & Test
        run: ./gradlew build
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/demo_test
          SPRING_DATASOURCE_USERNAME: demo
          SPRING_DATASOURCE_PASSWORD: demo

  # --- フロントエンド ---
  frontend:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: frontend

    steps:
      - uses: actions/checkout@v4

      - name: Set up Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install dependencies
        run: npm ci

      - name: Type check
        run: npx tsc --noEmit

      - name: Lint
        run: npm run lint

      - name: Build
        run: npm run build
```

---

## 7. README.md テンプレート

```markdown
# Demo Product App

PostgreSQL + Spring Boot + React 19 (Ant Design ProTable) によるデモ用Webアプリケーション。

## 技術スタック

- **Frontend:** React 19, TypeScript, Ant Design 5, ProTable
- **Backend:** Spring Boot 3.3, Java 21, Spring Data JPA
- **Database:** PostgreSQL 16
- **Infrastructure:** Docker Compose

## クイックスタート

### 前提条件

- Docker / Docker Compose
- Java 21（バックエンド単体開発時）
- Node.js 22（フロントエンド単体開発時）

### 全体起動（Docker Compose）

git clone https://github.com/{user}/demo-product-app.git
cd demo-product-app
docker compose up -d

- フロントエンド: http://localhost:3000
- バックエンドAPI: http://localhost:8080
- PostgreSQL: localhost:5432

### バックエンド単体起動

cd backend
./gradlew bootRun

### フロントエンド単体起動

cd frontend
npm install
npm run dev

## 主な機能

- 商品マスタの検索・一覧表示（ページネーション付き）
- カラムソート・フィルター
- Excelダウンロード（検索条件反映）
- Excelアップロードによる一括更新

## ドキュメント

- [設計書](docs/design.md)
- [API仕様](docs/api-spec.md)
- [環境構築ガイド](docs/setup-guide.md)
```

---

## 8. 推奨する開発の進め方

```
Step 1: リポジトリ作成 + 初期コミット
         ├── ディレクトリ構成
         ├── .gitignore / .editorconfig
         ├── README.md
         └── docker-compose.yml（PostgreSQLのみ起動可能な状態）

Step 2: Issue 一括作成 + マイルストーン設定

Step 3: Phase 1 から順にブランチを切って開発
         feature/DB-001-initial-schema
           → PR → main にマージ
         feature/BE-001-project-init
           → PR → main にマージ
         ...（以降繰り返し）

Step 4: 各Phaseの完了時に動作確認
         Phase 2 完了 → Postman / curl でAPI確認
         Phase 3 完了 → 画面で検索・一覧表示確認
         Phase 4 完了 → Excel ダウンロード/アップロード確認
```

### コミットメッセージ規約

```
{種別}: #{Issue番号} 概要

例:
feat: #7 商品検索APIにページネーション・ソート対応を追加
fix: #16 ページ切替時に検索条件がリセットされる問題を修正
docs: #24 README にクイックスタート手順を追加
chore: #5 React 19 + Vite プロジェクト初期化
```

| 種別 | 用途 |
|---|---|
| `feat` | 新機能 |
| `fix` | バグ修正 |
| `refactor` | リファクタリング |
| `docs` | ドキュメント |
| `test` | テスト追加・修正 |
| `chore` | 設定・依存更新 |
| `style` | コードスタイル（動作に影響なし） |
