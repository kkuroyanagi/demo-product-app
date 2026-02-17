# CLAUDE.md - Demo Product App

## Project Overview

PostgreSQL + Spring Boot + React 19（Ant Design ProTable）によるデモ用Webアプリケーション。
商品マスタの検索・一覧表示・Excel入出力を通じて、業務系Webアプリの典型的な機能をデモする。

## Tech Stack

- **Frontend:** React 19, TypeScript, Vite 6, Ant Design 5, @ant-design/pro-components 2.x, @ant-design/v5-patch-for-react-19, Axios, React Router 7
- **Backend:** Spring Boot 3.3, Java 21, Spring Data JPA, Apache POI 5
- **Database:** PostgreSQL 16
- **Infrastructure:** Docker Compose

## Project Structure (Monorepo)

```
demo-product-app/
├── backend/          # Spring Boot application
├── frontend/         # React 19 + Vite application
├── db/init/          # DDL and seed data (mounted by Docker)
├── docs/             # Design documents
├── .github/workflows # CI/CD
└── docker-compose.yml
```

## Key Commands

### Docker

```bash
docker compose up -d          # Start all services
docker compose down            # Stop all services
docker compose logs -f backend # Backend logs
```

### Backend

```bash
cd backend
./gradlew build                # Build + test
./gradlew bootRun              # Run locally (requires DB)
./gradlew test                 # Run tests only
```

### Frontend

```bash
cd frontend
npm install                    # Install dependencies
npm run dev                    # Dev server (port 5173)
npm run build                  # Production build
npm run lint                   # ESLint
npx tsc --noEmit               # Type check
```

## Architecture Decisions

### Backend

- **Dynamic Search:** Use JPA Specification (not query methods) for building search queries dynamically
- **Package Structure:** controller → service → repository (standard layered architecture)
- **Excel:** Apache POI SXSSFWorkbook for downloads (streaming), XSSFWorkbook for uploads
- **Upload Strategy:** Upsert by product_code (existing → UPDATE, new → INSERT), partial success mode (skip error rows, commit valid rows)
- **Response Format:** ProTable-compatible JSON: `{ data, total, success, current, pageSize }`

### Frontend

- **React 19 Compatibility:** Import `@ant-design/v5-patch-for-react-19` in main.tsx before App
- **ProTable `request` function:** Convert ProTable params/sorter/filter to Spring Boot query parameters
- **Layout:** Ant Design Pro Layout with side menu + content area
- **State Management:** No global state library needed; ProTable manages its own state via `request`

### Database

- **Primary key:** BIGSERIAL (auto-increment)
- **Upsert key:** product_code (UNIQUE)
- **Indexes:** category, status, product_name

## Coding Conventions

### Java (Backend)

- Java 21 features are allowed (records, pattern matching, etc.)
- Use Lombok for Entity boilerplate (@Data, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- Naming: camelCase for Java, snake_case for DB columns (JPA naming strategy handles mapping)
- All API responses should follow the PageResponse / ImportResult DTOs defined in the design doc
- Validate inputs with Jakarta Bean Validation (@NotBlank, @Min, etc.)

### TypeScript (Frontend)

- Strict mode enabled
- Use functional components only (no class components)
- Use `interface` for props and data types, `type` for unions/intersections
- File naming: PascalCase for components (ProductTable.tsx), camelCase for utilities (productService.ts)
- Prefer named exports

### Git

- Branch naming: `{type}/{target}-{number}-{description}` (e.g., `feature/BE-001-product-entity`)
- Commit messages: `{type}: #{issue} description` (e.g., `feat: #7 商品検索APIにページネーション対応を追加`)
- Types: feat, fix, refactor, docs, test, chore, style
- Target prefixes: BE (backend), FE (frontend), DB (database), INF (infrastructure)

## Important Notes

- Design document is at `docs/design.md` — always refer to it for schema, API specs, and UI layout
- Sample data should be ~3,000 product records across 5 categories
- Excel column mapping is defined in the design doc (Section 4.2) — follow it exactly
- CORS: Allow localhost:5173 (Vite dev) → localhost:8080 (Spring Boot)
- Do NOT use React 18 APIs (ReactDOM.render, etc.) — this project uses React 19 with createRoot
