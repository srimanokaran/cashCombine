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
    accounts/       imports/   transactions/   categorisation/   dashboard/   search/
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
- `GET /api/search/expenses?q=&from=&to=&expand=&limit=` — expense search over description + category name. Totals use the shared `SpendClassifier`; `expand` is a comma-separated list of extra terms (OR) and is validated server-side.
- `GET /api/search/expenses/expand?q=` — separate optional query expansion API. Returns `available: false` when `search.expander.enabled` is off or the model is unreachable.
- `search.expander.*` config (`enabled`, `base-url`, `model`, `timeout-ms`) controls that older OpenAI-compatible keyword-expansion API; it is not the AI search toggle.
- `POST /api/search/ai/start` starts or connects to local Ollama; `POST /api/search/expenses/ai` uses it to select transactions for an explicit natural-language search. The model sees transaction data but not balances or account names; the backend validates its selections and computes totals with `SpendClassifier`. AI matching is approximate; regular search remains the default.
- `search.ai.*` config (`base-url`, `model`, `start-ollama`). Defaults to loopback with `start-ollama: true`, which lets the app launch `ollama serve` itself. Docker Compose sets `SEARCH_AI_BASE_URL=http://ollama:11434` and `SEARCH_AI_START_OLLAMA=false` because Ollama runs as its own service there.
- Docker: `docker compose up` runs `app` and `web` (nginx serving the built UI and proxying `/api`) and connects to Ollama on the host via `host.docker.internal`. The `ollama` and `ollama-pull` services are present but commented out in `docker-compose.yml`; Ollama in Docker is memory-hungry and CPU-only on macOS. `Dockerfile` has `app` and `web` targets.

## SQLite specifics

- Database file: `$XDG_DATA_HOME/cashCombine/cashcombine.db` (`~/.local/share/cashCombine/cashcombine.db` by default), set by `scripts/run-backend` via `CASHCOMBINE_DB`. One file per person, shared by every checkout/worktree — do not commit it. A bare `./gradlew bootRun` falls back to `./cashcombine.db` (gitignored, including WAL/SHM). Tests use `jdbc:sqlite::memory:` (see `src/test/resources/application.yaml`) and never touch the real file.
- Hibernate dialect: `SQLiteDialect` from `hibernate-community-dialects` (not a native dialect).
- The `JpaConfig` bean explicitly sets `hibernate.dialect` to `SQLiteDialect::class.java`.

## Design docs

- `goal.md` — product vision (gitignored)
- `domain-notes.md` — domain decisions, invariants, CommBank CSV spec
- `architectural-decisions/` — ADRs (e.g. credit-card merchants as spend truth)
- `backend-review.md` — review findings with fix statuses