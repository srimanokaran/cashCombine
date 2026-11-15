# 003 — Local AI-assisted expense search

**Status:** Accepted

**Date:** 2026-11-15

**Amends:** [002 — Deterministic search](./002-deterministic-search-with-optional-query-expansion.md)

## Context

Literal search is predictable, but a question such as “Japan train expenses” can refer to bank
transactions that contain neither “Japan” nor “train”. The user wants to opt into a local model
that can inspect transaction data and suggest relevant transactions.

## Decision

- Keep regular search deterministic and the default. Selecting AI search explicitly starts or
  connects to a local Ollama server and lets the user submit a natural-language question.
- Send only the information needed for matching to the local model: transaction identifiers, dates,
  amounts, descriptions and categories. Do not send balances or account names. The AI search endpoint
  defaults to Ollama on loopback, may be pointed at another local endpoint (for example the `ollama`
  service in Docker Compose), and does not download a model automatically.
- The model selects candidate transaction identifiers. The application checks those identifiers
  against the supplied candidates and computes all totals and category splits using the same
  `SpendClassifier` as regular search. The model does not calculate or modify financial amounts.
- Label results as AI-selected and approximate. Arithmetic can be correct for the selected rows
  while the selection itself includes unrelated expenses or misses relevant ones.
- Process the candidates in bounded batches; fail visibly if processing cannot be completed instead
  of showing a partial total as though the entire ledger was searched.

## Consequences

- This amends ADR 002's rule that the model can only suggest keywords: the opt-in AI mode can see and
  select transactions. The older keyword expansion API remains a separate capability.
- AI mode may be slow, particularly when the model must load or the ledger grows. Ollama and a local
  `llama3` model are required; errors do not prevent regular search.
- Transaction descriptions are untrusted input. The model can misinterpret them or follow instructions
  embedded in them, so results need user review; selection is not a substitute for classification or
  accounting rules.
- Search does not edit the ledger. Category changes from the result table continue to use the normal
  categorisation flow and may update rules and other transactions.
