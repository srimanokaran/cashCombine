# 002 — Deterministic search, optional local model for query expansion

**Status:** Accepted  
**Date:** 2026-11-14

## Context

We want to ask questions like “how much have I spent on my Japan trip so far?” and get a total plus the
matching expenses. Bank descriptions are literal (`JAPAN RAIL PASS`), and a local llama model is
available on the same machine.

Two shapes were considered:

1. **Model answers the question** — pass transactions to the model and let it pick rows and sum them.
2. **Deterministic search, model only widens the query** — Kotlin matches rows and computes totals; the
   model may suggest related search terms when nothing matched.

Option 1 makes the total unreproducible (no test can assert it), replaces the documented spend rule
with token prediction, and puts model latency in the request path. It cannot be the answer to a money
question.

## Decision

- Search matches a query term against **bank description or category name**. Every typed term must
  match (AND); terms suggested by the model are alternatives (OR) and are clearly marked per row.
- **Totals are always computed in Kotlin** through the shared `SpendClassifier` — the same rule the
  dashboard uses (refunds net, transfers excluded). A model never produces or edits an amount.
- The local model is an **optional fallback**, only consulted when a search returns nothing and the
  query is at least 3 characters. It returns a JSON list of terms only, which `SearchTokens`
  validates. Any failure (disabled, offline, timeout, unusable output) silently keeps the
  deterministic result.
- Disabled by default (`search.expander.enabled`), so the app behaves identically with no model
  running and integration tests never depend on one.

## Consequences

- Searching “japan” does not find a Shibuya hotel unless the merchant, category name or an expanded
  term says so. Categories and rules remain the durable way to track a trip; search is read-only and
  does not write categorisation.
- Because category names are part of the match, recategorising a transaction can change whether a
  search finds it. That is intended: searching a category name is how “travel” works.
- The model can only broaden, never narrow or reinterpret. Its worst case is a few irrelevant rows,
  never a wrong total.
- Search scans transactions in memory like the dashboard does. The seam is `ExpenseSearchService`;
  moving to a SQL `LIKE` query later is a single-file change.
