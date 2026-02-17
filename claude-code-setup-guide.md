# Claude Code によるプロジェクトセットアップガイド

## 1. Claude Code でできること

Claude Code はターミナル上で動作するエージェント型コーディングツールで、以下の操作をすべて自然言語の指示で実行できる。

### プロジェクト初期構築で特に有効な操作

| 操作 | Claude Code での実行例 |
|---|---|
| ディレクトリ構成の作成 | 「設計書に基づいてモノレポのディレクトリ構造を作って」 |
| Spring Boot プロジェクト初期化 | 「Java 21 + Gradle で Spring Boot 3.3 プロジェクトを作って」 |
| React プロジェクト初期化 | 「Vite + React 19 + TypeScript でフロントエンドを初期化して」 |
| 依存パッケージの追加 | 「antd と @ant-design/pro-components を追加して」 |
| Docker Compose 作成 | 「PostgreSQL 16 + バックエンド + フロントエンドの docker-compose.yml を作って」 |
| DDL / 初期データ作成 | 「設計書の products テーブルの DDL と 3000件のサンプルデータを生成して」 |
| 設定ファイル作成 | 「application.yml に PostgreSQL 接続設定と CORS 設定を書いて」 |
| .gitignore 作成 | 「Java + Node.js + Docker のモノレポ用 .gitignore を作って」 |
| Git 操作 | 「feature/DB-001-initial-schema ブランチを作ってコミットして」 |
| テスト実行 | 「テストを実行して、失敗したら修正して」 |
| コードレビュー | PR上で `@claude` メンションするとレビューしてくれる |

### Git ワークフローの自動化

Claude Code は Git 操作にも対応しており、以下のような流れを対話的に進められる。

```
あなた: 「Phase 1 の DB 初期化作業をやろう。ブランチを切って」
Claude: feature/DB-001-initial-schema ブランチを作成しました。

あなた: 「設計書の DDL を db/init/01_ddl.sql に作って」
Claude: （ファイル生成）

あなた: 「コミットして」
Claude: feat: #2 テーブル定義（DDL）作成 でコミットしました。

あなた: 「プッシュして」
Claude: origin に push しました。
```

---

## 2. セットアップ手順

### 2.1 Claude Code のインストール

```bash
# Mac / Linux
curl -fsSL https://code.claude.com/install | sh

# 確認
claude --version
```

### 2.2 プロジェクトディレクトリで起動

```bash
cd demo-product-app
claude
```

初回起動時にプロジェクト構造を自動的に読み取る。`CLAUDE.md` があればそれも参照される。

---

## 3. CLAUDE.md を用意する（重要）

`CLAUDE.md` はプロジェクトルートに置く設定ファイルで、Claude Code にプロジェクトのコンテキストを伝える最も重要な仕組み。これがあると Claude Code の出力品質が大幅に向上する。

**次のセクションの内容を `CLAUDE.md` としてリポジトリルートにコミットすること。**

---

## 4. 推奨する作業の進め方

### Phase 1: 基盤構築（Claude Code に依頼する順序）

```
1. 「CLAUDE.md を読んで、プロジェクトの全体像を把握して」

2. 「モノレポのディレクトリ構造を作って。
    backend/, frontend/, db/init/, docs/, .github/workflows/ を用意して」

3. 「.gitignore と .editorconfig を作って」

4. 「docker-compose.yml を作って。
    PostgreSQL 16 をポート5432で起動、
    初期化SQLは db/init/ から読み込む構成で」

5. 「db/init/01_ddl.sql を設計書に基づいて作って」

6. 「db/init/02_master_data.sql にカテゴリマスタ5件を作って」

7. 「db/init/03_sample_data.sql に商品データ3000件を生成して」

8. 「docker compose up -d でDBを起動して、テーブルが作られたか確認して」
```

### Phase 2: バックエンド（例）

```
1. 「backend/ に Spring Boot 3.3 + Java 21 + Gradle プロジェクトを作って。
    依存: Spring Web, Spring Data JPA, PostgreSQL Driver, Apache POI」

2. 「設計書に基づいて Entity, Repository, DTO を作って」

3. 「ProductSpecification で動的検索を実装して」

4. 「ProductController と ProductService を実装して」

5. 「ビルドして、テストを実行して」

6. 「docker compose up で起動して、curl で /api/products を叩いて確認して」
```

### Phase 3: フロントエンド（例）

```
1. 「frontend/ に Vite + React 19 + TypeScript プロジェクトを作って。
    antd, @ant-design/pro-components, @ant-design/v5-patch-for-react-19,
    axios, react-router-dom を追加して」

2. 「BasicLayout を作って。左にサイドメニュー、右にコンテンツ領域」

3. 「ProTable で商品一覧ページを作って。検索・ソート・フィルター・ページネーション対応で」

4. 「npm run dev で起動して、画面を確認して」
```

---

## 5. Tips

### 設計書を参照させる

Claude Code はプロジェクト内のファイルを読めるので、`docs/design.md` を置いておけば「設計書を参照して」と言うだけで内容を反映してくれる。

### 段階的に進める

一度に「全部作って」ではなく、Phase ごと・機能ごとに指示した方が品質が高い。特にバックエンドとフロントエンドは別々に作って結合する流れが良い。

### テスト実行を組み込む

「実装したらテストを実行して、失敗したら修正して」と伝えると自律的にデバッグしてくれる。

### GitHub Actions との連携

`/install-github-app` コマンドで GitHub App を設定すると、PR 上で `@claude` メンションすることでコードレビューや修正を依頼できる。
