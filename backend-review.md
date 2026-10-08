# Backend review

Review of the Java/Spring backend as of Jul 2026. Prioritizes high-impact bugs, performance, security, and design issues over style preferences. Full test suite was green at review time (58 tests).

## Highest-impact findings

### 1. Manual rules can lose to broader seed rules — High — Fixed

**Status:** Fixed (Jul 2026). `TransactionClassifier` now prefers longer patterns before shorter ones (stable sort keeps `createdOrder` for ties). Seed rules return `Collections.unmodifiableMap` so LinkedHashMap encounter order is preserved. Covered by `prefersLongerMoreSpecificPatternOverBroaderSeedRule`.

**Original issue:** `TransactionClassifier` returned the first matching rule in creation order. A manually created full-description rule was appended after seed rules, so e.g. reclassifying `UBER *ONE MEMBERSHIP` lost to the earlier `UBER` rule on future imports.

### 2. API is unauthenticated and network-accessible — High if not strictly local

`application.yaml` does not configure `server.address`, and there is no authentication. The API exposes financial data and destructive endpoints (account/import/category deletion).

For a personal local-only application, bind it explicitly:

```yaml
server:
  address: 127.0.0.1
```

If it will ever be accessed remotely, add Spring Security authentication and HTTPS rather than relying on network location.

### 3. Duplicate detection is not reliably idempotent — High — Fixed

**Status:** Fixed (Jul 2026). `TransactionFingerprint` (and stored amounts) canonicalize money to scale 2, so `-45.0` and `-45.00` match. Import no longer wraps the whole CSV in one transaction, and `DataIntegrityViolationException` on save is counted as a duplicate (unique constraint as final authority). Covered by `TransactionFingerprintTest` and `treatsDifferentAmountScalesAsDuplicates`.

**Original issue:** `TransactionFingerprint` used `BigDecimal` record equality, which is scale-sensitive. Thus `-45.0` and `-45.00` were different in `acceptedThisImport`, despite representing the same amount. The database unique constraint could then reject the second row at flush/commit, rolling back the entire upload instead of counting it as a duplicate. Concurrent imports also had a check-then-insert race.

### 4. Classification causes excessive database work — Medium-high

`TransactionClassifier.classify()` loads every rule from the database for every transaction.

Consequences:

- Import: each CSV row loads all rules and performs a duplicate query.
- Re-analysis: `N` transactions cause `N` rule queries.
- Category changes scan and save against all transactions.

**Improve by** loading rules once per operation:

```java
var rules = ruleRepository.findAll();

for (Transaction transaction : transactions) {
    CategoryId result = classifier.classify(transaction.description(), rules);
}
```

For imports, also consider fetching existing fingerprints once or processing in batches. For category drill-down, use the existing `findByCategoryId()` instead of loading every transaction in `DashboardService.listTransactions()`.

### 5. Unbounded transaction queries will eventually affect responsiveness — Medium

Several endpoints and services load the entire transaction table:

- `DashboardService` (breakdown and drill-down)
- `TransactionService.applyRuleToMatchingTransactions`
- `CategoryReanalysisService.reanalyse`
- Account transaction listing

The request logger then buffers the complete response body as well.

**Improvements:**

- Add pagination to account transaction endpoints.
- Query category drill-down directly by category.
- Aggregate dashboard totals with SQL `GROUP BY`.
- Process re-analysis in pages/batches.

This is not urgent for hundreds of rows, but becomes noticeable at thousands or tens of thousands.

### 6. Schema changes are spread across Hibernate and startup runners — Medium

The project uses:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

alongside custom startup migrations such as:

- `SqliteAccountTypeSchemaFixer`
- `ClassificationSeedRunner`
- `ImportBatchBackfillRunner`

This makes schema history difficult to reproduce and test. `ddl-auto=update` is particularly risky for persisted financial data because schema evolution is implicit.

**Use versioned migrations such as Flyway:**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

Move table rebuilds, indexes, backfills, and category migrations into numbered migration scripts. Keep seed data separate from schema migration.

### 7. Important system categories can be deleted — Medium

`CategoryService.deleteCategory()` protects only `Uncategorised`. It allows deletion of:

- `Income`
- `Funds between accounts`

Deleting either reassigns transactions and deletes rules, changing dashboard accounting semantics. The seed runner recreates the category later, but cannot restore those assignments.

**Introduce:**

```java
public boolean isSystemCategory() {
    return isUncategorised()
        || isIncome()
        || isExcludedFromExpenses();
}
```

Reject deletion of all system categories and test each case.

### 8. Deleting a category can leave transactions permanently “manual” — Medium

`CategoryService` uses `reassignCategory()`, which changes only the category. If the deleted category was manually assigned, the transaction becomes Uncategorised but remains `MANUAL`, so reanalysis will always skip it.

**Improve by** using a dedicated operation such as `resetToUncategorised()` that also changes the assignment source to `RULE` or a new `SYSTEM` value.

### 9. Malformed CSV can expose financial data in logs/responses — Medium

Parse errors in `CommBankCsvParser` include the complete CSV row. For a malformed first row, that message reaches the API response and request logs via `ApiExceptionHandler` / `ApiRequestLoggingFilter`, potentially exposing transaction descriptions, amounts, and balances.

**Improve by** reporting a row number and field-level reason (e.g. `Invalid amount on row 7`), while logging detailed data only through explicitly redacted diagnostics.

### 10. Response logging buffers every API response — Medium

`ApiRequestLoggingFilter` wraps every API response in `ContentCachingResponseWrapper`, even though the body is only inspected for errors.

Large transaction responses are fully held in memory before being sent, adding latency and memory usage.

Prefer logging only method, path, status, and duration. Error messages can be logged directly by `ApiExceptionHandler`, avoiding response-body interception entirely.

### 11. ING and NAB accounts are created but cannot import — Medium

`ensureFixedAccounts()` creates all three account types, but parser and fingerprint maps only support `COMMBANK`. Uploading to generated ING or NAB accounts returns “No CSV parser registered.”

**Improve by** implementing both strategies before exposing those account types, or only creating/accepting types present in both registries.

## Other worthwhile improvements

- Add a database uniqueness constraint for case-normalized rule patterns; `createRule()` currently permits duplicate and conflicting rules.
- Replace `max(created_order) + 1` with explicit priority or database-managed ordering; concurrent rule creation can assign the same order.
- Configure explicit multipart limits and validate empty uploads.
- Consider a real CSV library if additional bank formats are added — the current parser does not correctly preserve escaped quotes.
- Rule matching uses `toLowerCase()` without `Locale.ROOT`; prefer `Locale.ROOT` (and preferably store a normalized pattern once).
- Keep package names lowercase (`com.example.cashcombine`) when a package migration is practical; conventional but not urgent.

## Important test gaps

- Duplicate amounts with differing `BigDecimal` scales and simultaneous imports.
- Real JPA import rollback/constraint behavior (most import tests use in-memory repositories).
- Unsupported ING/NAB import behavior.
- Large-import query count and dashboard scalability.
- Malformed quoting, escaped quotes, embedded newlines, BOM, and oversized CSV lines.
- Log-redaction assertions for malformed financial rows.
- Deleting a category containing manually categorized transactions.
- Manual rules overriding broad seed rules.
- Authorization/network exposure tests if the API is intended beyond localhost.

## Strengths

- Clear domain/application/persistence separation with repository ports.
- Good use of domain-specific ID types and `BigDecimal` for money.
- Transactional boundaries cover multi-step destructive operations.
- Database-level transaction fingerprint uniqueness provides useful defense in depth.
- Tests cover primary import, categorization, deletion, reanalysis, dashboard, and API flows.
