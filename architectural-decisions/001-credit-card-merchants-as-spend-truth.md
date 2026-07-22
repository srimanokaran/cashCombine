# 001 — Credit card merchants as spend truth

**Status:** Accepted  
**Date:** 2026-07-22

## Context

Importing both CommBank (cash) and NAB/Qantas Money (credit card) CSVs double-counted spend if both the cash→card payment (~$6k BPAY) and the card merchants counted as expenses.

We briefly tried:

1. **Payment as truth, merchants advisory** — Expenses showed a lump “Credit cards” category; merchants only explained that payment via an auto date window.
2. That made the category mix useless (one opaque bar) and still needed payment↔merchant reconciliation when amounts/periods did not match.

## Decision

- **NAB / credit-card merchants** are the source of truth for *where* money was spent. They count toward Expenses and Dashboard totals by category (Groceries, Dining, …).
- **Cash→card payments** (e.g. CommBank “Qantas Credit Cards” BPAY) are **Funds between accounts** — excluded from spend/income totals, same as a savings transfer.
- Do **not** reconcile payment amount to the sum of merchants in a window. No auto-window breakdown under the payment.
- Timing is **accrual** (purchase date on the card), not cash (when the card was paid off).

## Consequences

- Expenses totals can look “low” vs bank cash outflow in a month where a large card payment clears older debt: the payment is excluded; only imported merchants (plus other cash spends) appear.
- Incomplete card CSV coverage understates lifestyle spend relative to what was paid.
- Interest/fees on the card still need categorisation rules if they should show as spend; the cash payment itself never should.
- The old “Credit cards” expense category is retired (migrated into Funds between accounts on startup).



## Alternatives considered


| Approach                                            | Why rejected                                                             |
| --------------------------------------------------- | ------------------------------------------------------------------------ |
| Count both payment and merchants                    | Double-counts the same lifestyle spend                                   |
| Payment as P&L, merchants advisory only             | Opaque “Credit cards” lump; weak category insight                        |
| Pro-rate payment across merchants / Unallocated gap | Extra complexity for a personal tracker; banks do not guarantee equality |


