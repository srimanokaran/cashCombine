import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { CategoryPicker } from '../components/CategoryPicker'
import { formatDate, formatMoney } from '../format'
import type { Category, ExpenseSearchResult } from '../types'

const MIN_QUERY_LENGTH = 2
const MIN_EXPAND_LENGTH = 3
const EXAMPLE_QUERIES = ['japan', 'travel', 'woolworths']
const BAR_COLORS = ['#7a73ff', '#2dd4bf', '#f59e0b', '#38bdf8', '#f472b6', '#a78bfa', '#34d399']

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''

  const [input, setInput] = useState(q)
  const [direct, setDirect] = useState<ExpenseSearchResult | null>(null)
  const [expanded, setExpanded] = useState<ExpenseSearchResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [changingId, setChangingId] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  // Keep the box in step with the URL (back/forward, or a suggestion click elsewhere).
  useEffect(() => {
    setInput(q)
  }, [q])

  // Debounced search-as-you-type, with the query in the URL so a search is shareable.
  useEffect(() => {
    if (input.trim() === q) {
      return
    }
    const timer = window.setTimeout(() => {
      const next = input.trim()
      setSearchParams(next ? { q: next } : {}, { replace: true })
    }, 250)
    return () => window.clearTimeout(timer)
  }, [input, q, setSearchParams])

  useEffect(() => {
    let cancelled = false
    api
      .listCategories()
      .then((rows) => {
        if (!cancelled) {
          setCategories(rows)
        }
      })
      .catch(() => {
        // Category pickers simply stay empty; search still works.
      })
    return () => {
      cancelled = true
    }
  }, [])

  useEffect(() => {
    const trimmed = q.trim()
    let cancelled = false
    setExpanded(null)
    setError(null)

    if (trimmed.length < MIN_QUERY_LENGTH) {
      setDirect(null)
      setLoading(false)
      return
    }

    setLoading(true)
    const run = async () => {
      try {
        const directResult = await api.searchExpenses({ q: trimmed })
        if (cancelled) {
          return
        }
        setDirect(directResult)
        setLoading(false)

        const nothingFound =
          directResult.expenseCount === 0 && directResult.excludedCount === 0
        if (!nothingFound || trimmed.length < MIN_EXPAND_LENGTH) {
          return
        }

        // Optional local-model fallback: only asked when the deterministic search found nothing.
        // Best-effort — a failure here must never surface as a search error.
        try {
          const expansion = await api.expandSearch(trimmed)
          if (cancelled || !expansion.available || expansion.terms.length === 0) {
            return
          }
          const relatedResult = await api.searchExpenses({ q: trimmed, expand: expansion.terms })
          if (cancelled) {
            return
          }
          setExpanded(relatedResult)
        } catch {
          // Related terms are optional; the direct result stands.
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Search failed')
          setLoading(false)
        }
      }
    }
    void run()
    return () => {
      cancelled = true
    }
  }, [q, reloadKey])

  async function onChangeCategory(transactionId: string, categoryId: string) {
    setChangingId(transactionId)
    setError(null)
    try {
      await api.changeCategory(transactionId, categoryId)
      setReloadKey((key) => key + 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to change category')
    } finally {
      setChangingId(null)
    }
  }

  const active = expanded ?? direct
  const directIsEmpty = direct != null && direct.expenseCount === 0 && direct.excludedCount === 0
  const maxCategoryAmount = Math.max(
    0,
    ...(active?.categories.map((row) => Math.abs(Number(row.amount))) ?? []),
  )

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Search</h1>
          <p className="lede">
            Find expenses by bank description or category name. Type a few words — “japan”, “travel”,
            “woolworths” — and every word must match. Totals use the same rules as the Expenses page,
            so refunds reduce spend and transfers between accounts never count.
          </p>
        </div>
      </div>

      <div className="panel search-bar">
        <input
          type="search"
          value={input}
          placeholder="Search expenses — e.g. japan, travel, woolworths"
          aria-label="Search expenses"
          autoFocus
          onChange={(event) => setInput(event.target.value)}
        />
        {input !== q && <span className="search-pending">searching…</span>}
      </div>

      {q.trim().length < MIN_QUERY_LENGTH && (
        <div className="panel search-hints">
          <h2>Try a search</h2>
          <p className="muted">
            Every word you type has to match, in either the bank description or the category name.
          </p>
          <div className="search-suggestions">
            {EXAMPLE_QUERIES.map((example) => (
              <button
                key={example}
                type="button"
                className="search-chip"
                onClick={() => setInput(example)}
              >
                {example}
              </button>
            ))}
          </div>
        </div>
      )}

      {error && <p className="error">{error}</p>}
      {loading && <p className="lede">Searching…</p>}

      {!loading && active && active.expenseCount + active.excludedCount > 0 && (
        <>
          {expanded && (
            <div className="search-expanded">
              <div>
                <strong>Nothing matched “{active.query}” directly.</strong> These are related
                matches found by your local model, searched as:{' '}
                {expanded.expandedTerms.map((term) => (
                  <span key={term} className="search-related-term">
                    {term}
                  </span>
                ))}
              </div>
              <button type="button" className="search-link" onClick={() => setExpanded(null)}>
                Show direct results
              </button>
            </div>
          )}

          <div className="panel search-summary">
            <div>
              <p className="summary-label">
                {Number(active.totalSpend) < 0 ? 'Net refund' : 'Total spent'}
              </p>
              <p className={`summary-value ${Number(active.totalSpend) < 0 ? 'positive' : ''}`}>
                {formatMoney(Math.abs(Number(active.totalSpend)))}
              </p>
              <p className="search-summary-meta">
                across {active.expenseCount} expense{active.expenseCount === 1 ? '' : 's'}
                {active.firstDate && active.lastDate && (
                  <>
                    {' · '}
                    {formatDate(active.firstDate)} – {formatDate(active.lastDate)}
                  </>
                )}
                {active.excludedCount > 0 && (
                  <>
                    {' · '}
                    {active.excludedCount} income/transfer match
                    {active.excludedCount === 1 ? '' : 'es'} not counted
                  </>
                )}
              </p>
            </div>
            {active.truncated && (
              <p className="muted search-summary-note">
                Showing the {active.transactions.length} most recent transactions; the total covers
                all {active.expenseCount}.
              </p>
            )}
          </div>

          {active.categories.length > 0 && (
            <div className="panel">
              <h2>By category</h2>
              <ul className="spend-breakdown">
                {active.categories.map((row, index) => (
                  <li key={row.categoryId} className="spend-item">
                    <div className="spend-toggle search-category-row">
                      <div className="spend-row-top">
                        <span className="spend-name">
                          <span
                            className="spend-dot"
                            style={{ background: BAR_COLORS[index % BAR_COLORS.length] }}
                          />
                          {row.categoryName}
                        </span>
                        <span className="spend-amount">{formatMoney(row.amount)}</span>
                      </div>
                      <div className="spend-bar-track">
                        <div
                          className="spend-bar-fill"
                          style={{
                            width: `${Math.max(
                              (Math.abs(Number(row.amount)) / maxCategoryAmount) * 100,
                              1,
                            )}%`,
                            background:
                              Number(row.amount) < 0
                                ? '#34d399'
                                : BAR_COLORS[index % BAR_COLORS.length],
                          }}
                        />
                      </div>
                      <div className="spend-row-meta">
                        {row.transactionCount} transaction{row.transactionCount === 1 ? '' : 's'}
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {active.suggestions.length > 0 && (
            <div className="panel">
              <h2>Matching merchants</h2>
              <div className="search-suggestions">
                {active.suggestions.map((suggestion) => (
                  <button
                    key={suggestion.label}
                    type="button"
                    className="search-chip"
                    title={`Narrow the search to ${suggestion.label}`}
                    onClick={() => setInput(suggestion.label)}
                  >
                    <span className="search-chip-label">{suggestion.label}</span>
                    <span className="search-chip-meta">
                      {suggestion.matchCount} × {formatMoney(suggestion.total)}
                    </span>
                  </button>
                ))}
              </div>
            </div>
          )}

          <div className="panel">
            <h2>Matched expenses</h2>
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Account</th>
                    <th>Description</th>
                    <th>Amount</th>
                    <th>Category</th>
                  </tr>
                </thead>
                <tbody>
                  {active.transactions.map((tx) => (
                    <tr key={tx.id} className="spend-detail-row">
                      <td>{formatDate(tx.date)}</td>
                      <td>{tx.accountName}</td>
                      <td>
                        {tx.description}
                        {tx.matchedBy === 'EXPANDED' && (
                          <span className="search-related-tag">related</span>
                        )}
                      </td>
                      <td
                        className={
                          Number(tx.amount) < 0
                            ? 'negative'
                            : Number(tx.amount) > 0
                              ? 'positive'
                              : undefined
                        }
                      >
                        {formatMoney(tx.amount)}
                      </td>
                      <td>
                        <CategoryPicker
                          categories={categories}
                          value={tx.categoryId}
                          disabled={changingId === tx.id}
                          ariaLabel={`Category for ${tx.description}`}
                          onChange={(categoryId) => onChangeCategory(tx.id, categoryId)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}

      {!loading && q.trim().length >= MIN_QUERY_LENGTH && directIsEmpty && !expanded && (
        <div className="panel search-empty">
          <h2>No expenses match “{q.trim()}”</h2>
          <p className="muted">
            Search matches bank descriptions and category names. Try fewer or shorter words, or
            search for a merchant name.
          </p>
        </div>
      )}
    </section>
  )
}
