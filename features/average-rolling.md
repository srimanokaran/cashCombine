# Rolling Average Alerts — Feature Plan

## Concept

For each category the user configures, compare the **current (ongoing) month's spend** against the **rolling average** of the previous N completed months. Alert when the current month exceeds the average by more than X%.

## What Already Exists

- `MonthlyCashflow` — `DashboardService.monthlyCashflow()` already buckets transactions by `YearMonth` with `totalExpenses`, `totalIncome`, `net` per month.
- `DashboardService` — loads all transactions via `findAll()` and processes in-memory. New alert computation will follow the same pattern.

## Data Model

New JPA entity: `AlertConfig`

| Field | Type | Description |
|---|---|---|
| `id` | UUID | Primary key |
| `categoryId` | UUID | Which category to track |
| `windowMonths` | Int | Rolling window size (default: 3) |
| `thresholdPercent` | Int | Alert when current > average × (1 + threshold/100) (default: 40) |

One config per category. No config = no alert. Default: 3-month window, 40% threshold.

## Computation

New service `AlertService`, wired in `LedgerConfig`:

```
alerts():
  for each AlertConfig:
    bucket all transactions for category by YearMonth
    currentMonth = ongoing (partial) month
    previousNMonths = N completed months before current
    if previousNMonths.size < windowMonths: skip
    rollingAvg = sum(previousNMonths) / N
    currentSpend = sum for current month
    pctOver = (currentSpend - rollingAvg) / rollingAvg × 100
    if pctOver >= thresholdPercent: include in alert results
  sort by pctOver descending
```

### Design decisions

- **Current month is partial**: a partial month naturally undercounts, so an alert mid-month is significant.
- **No persistence of alert state**: computed fresh each request. No scheduled jobs/caching.
- **Expense categories only** — income alerts are a separate future feature.
- **No alerts for "Funds between accounts" or "Uncategorised"**.

## API

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/api/dashboard/alerts` | List active alerts (category, current spend, avg, pct over) |
| `GET` | `/api/dashboard/alerts/config` | List all alert configs |
| `POST` | `/api/dashboard/alerts/config` | Create or update config for a category |
| `DELETE` | `/api/dashboard/alerts/config/{configId}` | Remove alert config |

Alert response shape:

```json
{
  "categoryId": "uuid",
  "categoryName": "Dining",
  "currentMonthSpend": 380.00,
  "rollingAverage": 210.00,
  "percentOver": 81,
  "windowMonths": 3,
  "thresholdPercent": 40
}
```

## Frontend

- New **"Alerts" tab/pill** on the `/trends` page — compact alert cards showing category name, current spend, rolling average, and percentage over
- A small **configuration modal** to add/edit/delete alerts per category: pick category, set window (3/6/12 months), set threshold (20-100%)
- Color coding: green (< threshold), yellow (near threshold), red (>> threshold)

## Test Plan

1. **`AlertServiceTest`** (unit, in-memory repos):
   - No configs → empty alerts
   - Not enough history → no alert
   - Spend below threshold → no alert
   - Spend above threshold → alert fires with correct pct
   - Multiple categories → sorted by severity
   - Current partial month counting correctly

2. **`LedgerApiIntegrationTest`** (additions):
   - `alertsEndpointReturnsEmptyWhenNoConfigs()`
   - Full flow: create config, import transactions, assert alert response

## Files

| Action | File |
|---|---|
| Create | `.../ledger/dashboard/AlertConfig.kt` (JPA entity) |
| Create | `.../ledger/dashboard/AlertConfigRepository.kt` |
| Create | `.../ledger/dashboard/AlertsResult.kt` (data class) |
| Create | `.../ledger/dashboard/AlertService.kt` |
| Create | `.../infrastructure/persistence/AlertConfigJpaEntity.kt` |
| Create | `.../infrastructure/persistence/AlertConfigJpaRepository.kt` |
| Create | `.../infrastructure/persistence/JpaAlertConfigRepository.kt` |
| Modify | `.../config/LedgerConfig.kt` (wire AlertService) |
| Create | `.../api/dashboard/AlertConfigController.kt` (or add to DashboardController) |
| Create | `.../api/dashboard/AlertResponse.kt` |
| Create | `src/test/.../ledger/dashboard/AlertServiceTest.kt` |
| Modify | `src/test/.../api/LedgerApiIntegrationTest.kt` |
| Modify | `frontend/src/api.ts` |
| Modify | `frontend/src/types.ts` |
| Create | `frontend/src/components/AlertsPanel.tsx` |
| Modify | `frontend/src/pages/TrendsPage.tsx` |