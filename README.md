# cashCombine

Personal finance ledger for consolidating bank CSV exports into one place: import transactions, detect duplicates, categorise with rules, and see spending and income clearly.

Built for personal use (and as a backend engineering practice project) — not a multi-tenant SaaS product. See [goal.md](./goal.md) for the longer-term vision, [domain-notes.md](./domain-notes.md) for domain decisions, and [architectural-decisions/](./architectural-decisions/) for ADRs.

## What it does today

- **Accounts** — create accounts by type (CommBank and NAB/Qantas Money credit card CSV import; other types can exist as placeholders)
- **CSV import** — upload bank exports; rows are accepted, skipped as duplicates, or rejected with a per-import summary
- **Duplicate detection** — fingerprints use date, amount, description, and balance (amounts normalised to 2 decimal places)
- **Categories & rules** — contains-match rules on description; longer/more specific patterns win over shorter seed rules
- **Manual overrides** — change a transaction’s category; that creates/updates a rule and re-applies it to matching non-manual transactions
- **Re-analyse** — re-run rules on existing imports without deleting uploads (manual overrides are kept)
- **Expenses** — spending and income breakdowns by category, with drill-down into transactions
- **Dashboard** — Recharts pie breakdown of spending and income
- **Special categories**
  - **Funds between accounts** — internal transfers (e.g. savings moves, ING→CommBank, cash→credit-card payments) excluded from spend/income totals
  - **Income** / **Uncategorised** credits count as income; credits filed under an expense category (e.g. a friend paying you back under Entertainment) net against that category’s spend
- **Credit cards** — NAB/Qantas Money merchants count toward Expenses by category; CommBank card payments are funds between accounts (not a lump “Credit cards” spend)

## Stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot, JPA/Hibernate |
| Database | SQLite (`cashcombine.db` in the project root) |
| Frontend | React 19, TypeScript, Vite, React Router, Recharts |
| Tests | JUnit 5, AssertJ; `make testAll` / Gradle |

## Prerequisites

- Java 17+
- Node.js 20+ (for the UI)
- macOS/Linux recommended for the helper scripts

## Run locally

### Backend

Quiet API log (recommended while using the UI):

```bash
./scripts/run-backend
```

Example:

```text
18:30:01 cashCombine ready → http://localhost:8080
18:30:01 Logging /api/* calls (4xx/5xx show rejection reason)
18:30:12 POST /api/accounts → 201 (18ms)
```

Full Spring logging:

```bash
./gradlew bootRun
```

- API: [http://localhost:8080](http://localhost:8080)
- SQLite file: `cashcombine.db` (gitignored, including WAL/SHM)

### Frontend

```bash
cd frontend
npm install
npm run dev
```

- UI: [http://localhost:5173](http://localhost:5173)
- Vite proxies `/api` to the backend on port 8080

## Typical workflow

1. Open **Import** and ensure a CommBank account exists (or create one).
2. Upload a CommBank CSV export.
3. Check **Expenses** / **Dashboard** for category breakdowns.
4. Expand a category, reassign transactions as needed — each change saves a rule for that description.
5. Optionally open **Categories** (and **Show classification rules** if you need manual patterns), or use **Re-analyse categories** after changing rules so existing imports catch up.

### CommBank CSV shape

Four columns per row: date, amount, description, balance (quoted fields supported). Sample fixtures live under `src/test/resources/csv/`. Real bank exports under `samples/` and root `*.csv` are gitignored.

## UI map

| Route | Purpose |
|---|---|
| `/` | Accounts + CSV import |
| `/accounts/:id` | Account transactions, imports, delete an import batch |
| `/expenses` | Spending & income by category (expand + reassign) |
| `/dashboard` | Pie charts for spend and income |
| `/categories` | Manage categories; classification rules are behind **Show classification rules** |

## Categorisation behaviour

1. On import (and re-analyse), each description is matched against rules.
2. Rules are tried **longest pattern first** (creation order breaks ties), so a full-description manual rule beats a short seed like `UBER`.
3. No match → **Uncategorised**.
4. Changing a category via the UI upserts a rule for that description and applies it to other matching transactions that are not manually overridden.

Seed categories/rules are applied on startup via `ClassificationSeedRunner` (including migrations like Streaming→Subscription and Home→Utilities).

## API overview

Base path: `/api`

| Area | Endpoints (summary) |
|---|---|
| Accounts | `GET/POST /accounts`, `GET/DELETE /accounts/{id}`, `POST /accounts/{id}/import`, imports & transactions under the account |
| Transactions | `PATCH /transactions/{id}/category` |
| Categories | `GET/POST /categories`, `DELETE /categories/{id}` |
| Rules | `GET/POST /rules`, `DELETE /rules/{id}` |
| Dashboard | `GET /dashboard/expenses`, category transaction lists, `POST /dashboard/expenses/reanalyse` |

There is **no authentication**. Treat this as a local-only app; do not expose the port on a shared network without adding auth and binding to localhost.

## Tests

```bash
make testAll          # unit + integration, live PASSED/FAILED lines
make testUnit
make testIntegration
make testReport       # summary of last full run
```

Or directly:

```bash
./gradlew test
./gradlew unitTest
./gradlew integrationTest
```

### CI

Pushes and pull requests to `main` run [`.github/workflows/ci.yml`](./.github/workflows/ci.yml):

- Backend unit tests (`./gradlew unitTest`)
- Backend integration tests (`./gradlew integrationTest`)
- Frontend typecheck & build (`npm run build`)

## Project layout

```text
src/main/java/.../cashCombine/
  api/              # HTTP controllers + response DTOs
  config/           # wiring, seed/migration runner
  infrastructure/   # JPA persistence adapters
  ledger/           # domain + application services
    accounts/
    categorisation/
    dashboard/
    imports/
    transactions/
frontend/           # React SPA
scripts/            # run-backend, test helpers
domain-notes.md     # domain decisions
architectural-decisions/  # ADRs (e.g. credit-card spend model)
goal.md             # product vision
```

## Design notes (short)

- Bank facts (date, amount, description, balance) are immutable after import; **category** is the editable interpretation.
- Account **type** selects CSV parser and fingerprint strategy.
- Import batches track what was accepted/duplicated/rejected and can be deleted (removing linked transactions).

## Related docs

- [goal.md](./goal.md) — vision, MVP scope, future ideas
- [domain-notes.md](./domain-notes.md) — Slice 1 domain Q&A and invariants
- [architectural-decisions/](./architectural-decisions/) — accepted architecture/product decisions
- [backend-review.md](./backend-review.md) — backend review findings (if present locally)
