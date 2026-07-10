# Domain Notes — Slice 1 (CSV Import)

Living document capturing domain decisions for the personal finance tracker.  
Refined through deliberate Q&A — not CRUD-first design.

See also: [goal.md](./goal.md) for project vision.

---

## User questions this slice answers

| Question | When |
|---|---|
| Did this CSV import work? | Immediately after upload |
| What was new vs already seen vs unusable? | Import summary (`accepted` / `duplicate` / `rejected`) |
| What category is this transaction? | During import (rules) and after (manual override) |

Reporting questions (“how much on Groceries?”) are **out of slice 1** — they read data later.

---

## Facts vs interpretations

### Facts (from bank CSV — immutable after import)

- Date
- Amount
- Description (raw text)
- Balance (when provided by bank, e.g. CommBank)
- Optional: reference ID (when bank provides one)

### Interpretations (app assigns — user may change some)

- Which **account** (user picks at import time — import context, not a CSV column)
- **Category** (rules, default, or manual override)
- Duplicate decision (derived, not user-edited)

### Invariant

> Imported bank fields (date, amount, description) cannot be changed after import.  
> Only **category** may be updated by the user.

---

## Accounts

### Creation

- User provides **name** + **type**
- **Type** is chosen from a **fixed list** defined by the app (e.g. CommBank, Amex)
- Account must exist before import

### What account type controls

Each type defines:

- Expected CSV **headers** (format validation)
- **Parser** (how to read rows)
- **Duplicate fingerprint** (which fields identify “same transaction”)

### Changing account type

| Situation | Rule |
|---|---|
| No imports yet | Type **may** be changed |
| Has imports | Type is **locked** |

**Recovery from wrong type:** delete the whole account → recreate with correct type → re-import.

### Deleting an account

- Deletes the account **and all its transactions**
- No orphan transactions

---

## Import

### Command (conceptual)

```text
ImportCsv(accountId, file) → ImportResult
```

User picks account first, then uploads CSV.

### Outcomes

#### File-level failure

| Condition | Result |
|---|---|
| Headers don’t match account type’s expected format | **Whole import fails** (`400`). Nothing stored. |

#### Row-level processing (headers OK)

| Condition | Bucket | Behaviour |
|---|---|---|
| Row parses and is new | **accepted** | Store transaction + assign category |
| Row matches existing transaction (per duplicate rules) | **duplicate** | Skip — no insert, no update |
| Row has bad data (unparseable date, empty amount, etc.) | **rejected** | Skip row, continue with rest |

### Idempotency

> Re-uploading the same transactions does not change the ledger.

- Same row seen again → **duplicate**, not a second insert
- File with 50 existing rows + 1 new row → `1 accepted, 50 duplicate`

### Success response shape

```text
ImportResult {
  accepted:  N,
  duplicate: N,
  rejected:  N
}
```

---

## Duplicate detection

### Scope

- Duplicates are evaluated **per account** (imports are always tied to one account)
- Fingerprint fields are **per account type** — different banks export different columns

### Examples

| Account type | Fingerprint (candidate) |
|---|---|
| CommBank | date + amount + description + **balance** |
| Amex (no balance) | date + amount + description (+ reference ID if available) |

> **Note:** Balance helps distinguish two real purchases with the same date, amount, and merchant on CommBank exports.

### Within a single upload

Rows are checked against **already-stored** transactions and **rows accepted earlier in the same import** (first wins, second is duplicate).

---

## Categorisation

### Rules (slice 1)

- **Contains** match on description: e.g. `"WOOLWORTHS"` → Groceries
- Rules are configurable (exact UI/API TBD at implementation time)

### Default

- No rule matches → assign category **"Uncategorised"**
- "Uncategorised" is a real category in the system, not `null`

### Manual override (slice 1)

- User may change category on any transaction after import
- Bank facts remain locked

### Re-import behaviour (recommended — confirm when implementing)

> Manual category assignment **should not** be overwritten when the same row is seen again as a duplicate (duplicate = skip, no update).  
> If rules are re-run on existing data in a future feature, manual assignments should win over rules.

### Not in slice 1

- ML / AI categorisation
- Merchant normalization
- LLM / RAG insights

---

## Invariants (summary)

1. Every import targets exactly **one** user-selected account.
2. Wrong CSV headers for account type → **fail entire import**.
3. A row already in the ledger (per duplicate fingerprint) → **duplicate**, never inserted twice.
4. A transaction has **exactly one** category at any time.
5. Imported bank data (date, amount, description) is **immutable** after import.
6. Account type is **immutable** once the account has imported transactions.
7. Deleting an account removes **all** its transactions.
8. Every successful import returns **accepted / duplicate / rejected** counts.

---

## What changes together

### One operation: `ImportCsv`

```text
1. Validate headers against account type
2. For each row:
     parse → duplicate check → accept | duplicate | reject
3. For each accepted row:
     store facts → apply categorisation rules → assign category
4. Return ImportResult summary
```

The user sees the summary only when this whole flow completes (for successful parses).

### Separate operations

| Operation | Notes |
|---|---|
| `CreateAccount(name, type)` | |
| `DeleteAccount(id)` | Wipes all transactions |
| `ChangeCategory(transactionId, categoryId)` | Interpretation only |
| `CreateRule(pattern, categoryId)` | Rules must exist before categorisation is useful |
| Spending queries / dashboard | Read side — later slice |

---

## Commands (slice 1)

| Command | Purpose |
|---|---|
| `CreateAccount(name, type)` | Register account with bank/format type |
| `DeleteAccount(id)` | Remove account and all transactions |
| `ImportCsv(accountId, file)` | Core vertical slice |
| `ChangeCategory(transactionId, categoryId)` | Manual override |
| `CreateRule(pattern, categoryId)` | Define contains-match rules |

---

## Structural sketch (not package layout)

For slice 1, one cohesive area is enough — call it **Ledger**:

```text
Ledger
  ├── Account          (name, type, hasImports?)
  ├── AccountType      (headers, parser, duplicate fingerprint) — app-defined config
  ├── Transaction      (immutable facts + category)
  ├── Import / ImportResult
  ├── Category         (including "Uncategorised")
  └── ClassificationRule (contains pattern → category)
```

**Aggregate guidance**

- **Import** (one upload) is the main consistency boundary during ingestion.
- **Transaction** is its own thing after import — do not model all transactions as children of Account.
- **Account** is thin metadata + type reference, not a growing aggregate of every transaction.

Bounded contexts (Budgeting, Insights, etc.) stay out of slice 1 per [goal.md](./goal.md).

---

## Build order (vertical slices)

```text
1. Account types (hardcoded CommBank) + CreateAccount / DeleteAccount
2. ImportCsv → accepted / duplicate / rejected (no categorisation yet)
3. Categories + rules + "Uncategorised"
4. ChangeCategory (manual override)
5. Second account type (e.g. Amex) — proves per-type headers and duplicate rules
```

Test invariants at each step before adding the next.

---

## CommBank CSV format (from real export)

**Important:** CommBank exports have **no header row**. The file is data only.

| Column index | Field | Example format |
|---|---|---|
| 0 | **Date** | `DD/MM/YYYY` (e.g. `10/07/2026`) |
| 1 | **Amount** | Quoted, signed (e.g. `"-15.26"`, `"+2000.00"`) |
| 2 | **Description** | Free text (merchant, transfer narrative, etc.) |
| 3 | **Balance** | Quoted, signed running balance after the row |

### Domain mapping

| Domain field | Source |
|---|---|
| `date` | Column 0 |
| `amount` | Column 1 (parse signed decimal, strip quotes) |
| `description` | Column 2 |
| `balance` | Column 3 (parse signed decimal, strip quotes) |

### Duplicate fingerprint (CommBank)

```text
accountId + date + amount + description + balance
```

### Format validation (replaces “header check” for CommBank)

Because there are no column names, **wrong format** means:

- Row does not have exactly **4** columns, or
- Date / amount / balance cannot be parsed

First row that fails structure → treat as format mismatch → **fail whole import** (per domain rules).  
Alternatively: if row 1 looks like valid data, accept the file and only **reject** bad rows — decide at implementation; lean toward failing fast if column count is wrong on row 1.

### Test fixtures

- **Real exports:** keep in `samples/` (gitignored) — never commit
- **Tests:** add a **sanitized** copy under `src/test/resources/csv/commbank-sample.csv` with fake descriptions/amounts

### Sign convention

- **Negative** amount → money out (spend, transfer out)
- **Positive** amount → money in (salary, credits, transfers in)

---

## Open decisions (resolve at implementation)

- [ ] Confirm: manual category never overwritten on duplicate re-import (recommended: yes)
- [ ] How users create categorisation rules in slice 1 (UI vs seeded config)
- [x] CommBank CSV column mapping (see above — no header row)
- [ ] Amex / second type fingerprint fields
- [ ] ImportResult: counts only, or include rejected-row reasons?
- [ ] CommBank format validation: fail on first bad column count vs per-row reject only

---

## Design framework (how we got here)

When adding a feature, answer in order:

1. **What is the user trying to decide or understand?**
2. **What are facts vs interpretations?**
3. **What must always be true?** (invariants)
4. **What changes together under one operation?**

Only then: entities, aggregates, tables, code.
