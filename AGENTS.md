# AGENTS.md

## Stack

- **Backend**: Kotlin 2.1, JDK 17, Spring Boot 4.1, JPA/Hibernate, SQLite
- **Frontend**: React 19, TypeScript 6, Vite 8, React Router 7, Recharts
- **Linter**: oxlint (not ESLint)

## Commands

```bash
# Run backend (quiet API log with SPRING_PROFILES_ACTIVE=quiet)
./scripts/run-backend             # http://localhost:8080

# Run frontend
cd frontend && npm run dev        # http://localhost:5173 (proxies /api → :8080)

# Backend tests
make testAll                      # unit + integration
make testUnit
make testIntegration
make testReport                   # terminal summary from XML results

# Frontend
npm run lint                      # oxlint
npm run build                     # tsc -b && vite build
```

## Test conventions

- Integration tests must be named `*IntegrationTest` (not `*IT` or `*Tests`). Gradle tasks `unitTest` and `integrationTest` filter on that naming.
- Tests use JUnit 5 + AssertJ. Test results are logged live in the terminal (all Test tasks have `testLogging` configured in build.gradle.kts).
- Kotlin test methods use backtick names: `fun \`descriptive name here\`()`.
- XML test results live under `build/test-results/{taskName}/`.
- CI (`.github/workflows/ci.yml`) runs `./gradlew unitTest`, `./gradlew integrationTest`, and `npm run build` for the frontend.

## Architecture

```
src/main/java/.../cashCombine/   # (path unchanged; all .kt files now)
  api/              # REST controllers, DTOs, exception handler, request logging filter
  config/           # Wiring: JPA dialect (SQLite), seed runner, backfill runner, CORS
  infrastructure/   # JPA persistence adapters (repositories)
  ledger/           # Domain + application services per subpackage:
    accounts/       imports/   transactions/   categorisation/   dashboard/
```

- The app bootstraps with `ClassificationSeedRunner` (ApplicationRunner, Order(1)) — idempotently seeds categories/rules and runs schema migrations (Streaming→Subscription, Home→Utilities, Credit cards→Funds between accounts).
- `ImportBatchBackfillRunner` (Order(2)) backfills import batches for pre-existing transactions.

## Kotlin specifics

- Gradle plugins: `kotlin("jvm")`, `kotlin("plugin.spring")` (auto-`open` for Spring CGLIB proxies), `kotlin("plugin.jpa")` (synthetic no-arg constructors for JPA entities).
- **JPA entities use regular `class`**, not `data class` — Hibernate proxies conflict with data class `equals`/`hashCode`.
- Domain records (non-entity) use `data class`.
- `Optional<X>` → `X?` (nullable types). No `Optional` in the codebase.
- Domain property access: some use `@get:JvmName` (access via `.property`), others use explicit `fun` methods (access via `.method()`). Check the class definition to see which pattern.
- `@Transactional` classes/methods need `open` modifier for CGLIB bytecode proxies.
- Build scripts: `build.gradle.kts`, `settings.gradle.kts` (Kotlin DSL).

## Domain invariants (do not break)

1. Bank facts (date, amount, description, balance) are **immutable** after import. Only **category** may change.
2. Account **type** is locked once the account has imported transactions.
3. Transactions have exactly **one** category at any time. "Uncategorised" is a real category, not null.
4. Duplicate fingerprint per account type (CommBank: date + amount + description + balance, amounts normalised to scale 2).
5. `Categories/Rules` handles import + classification boundary — rules are tried **longest pattern first**; creation order breaks ties.
6. Deleting an account removes **all** its transactions.

## CSV import quirks

- CommBank exports have **no header row** — columns: date (DD/MM/YYYY), amount, description, balance.
- NAB/Qantas Money credit card has headers and its own parser.
- Importable types: `COMMBANK`, `NAB_CREDIT_CARD`. ING exists as a placeholder.
- Negative amount = money out; positive = money in.
- `goal.md` and root `*.csv`/`samples/` are gitignored.

## API notes

- Base path: `/api`. No authentication — local-only app.
- `POST /api/accounts/ensure-fixed` — idempotently creates default accounts. Frontend calls this on first load.
- `POST /api/dashboard/expenses/reanalyse` — re-runs classification rules on existing transactions (manual overrides preserved).
- Changing a transaction's category via `PATCH /transactions/{id}/category` upserts a rule for that description and re-applies to non-manual matches.

## SQLite specifics

- Database file: `cashcombine.db` in project root (gitignored, including WAL/SHM).
- Hibernate dialect: `SQLiteDialect` from `hibernate-community-dialects` (not a native dialect).
- The `JpaConfig` bean explicitly sets `hibernate.dialect` to `SQLiteDialect::class.java`.

## Design docs

- `goal.md` — product vision (gitignored)
- `domain-notes.md` — domain decisions, invariants, CommBank CSV spec
- `architectural-decisions/` — ADRs (e.g. credit-card merchants as spend truth)
- `backend-review.md` — review findings with fix statuses