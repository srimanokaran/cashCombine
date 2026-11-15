import { useEffect, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { api } from '../api'
import { CategoryPicker } from '../components/CategoryPicker'
import { formatDate, formatMoney } from '../format'
import type { Category, ExpenseSearchResult } from '../types'

const MIN_QUERY_LENGTH = 2
const EXAMPLE_QUERIES = ['japan', 'travel', 'woolworths']
const BAR_COLORS = ['#7a73ff', '#2dd4bf', '#f59e0b', '#38bdf8', '#f472b6', '#a78bfa', '#34d399']
type SearchMode = 'regular' | 'ai'
type AiStatus = 'idle' | 'starting' | 'ready' | 'error'

export function SearchPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const q = searchParams.get('q') ?? ''
  const [mode, setMode] = useState<SearchMode>('regular')
  const [input, setInput] = useState(q)
  const [aiInput, setAiInput] = useState('')
  // A new object even for the same query makes an explicit resubmission run again.
  const [aiQuery, setAiQuery] = useState<{ query: string } | null>(null)
  const [aiStatus, setAiStatus] = useState<AiStatus>('idle')
  const [aiStartError, setAiStartError] = useState<string | null>(null)
  const [startAttempt, setStartAttempt] = useState(0)
  const [result, setResult] = useState<{ mode: SearchMode; query: string; data: ExpenseSearchResult } | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [changingId, setChangingId] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)
  const startController = useRef<AbortController | null>(null)
  const searchController = useRef<AbortController | null>(null)
  const modeVersion = useRef(0)

  // The regular query remains in the URL, including while AI mode is selected.
  useEffect(() => {
    setInput(q)
  }, [q])

  useEffect(() => {
    if (mode !== 'regular' || input.trim() === q) return
    const timer = window.setTimeout(() => {
      const next = input.trim()
      setSearchParams(next ? { q: next } : {}, { replace: true })
    }, 250)
    return () => window.clearTimeout(timer)
  }, [mode, input, q, setSearchParams])

  useEffect(() => {
    let cancelled = false
    api.listCategories().then((rows) => {
      if (!cancelled) setCategories(rows)
    }).catch(() => {
      // Category pickers simply stay empty; search still works.
    })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    if (mode !== 'ai') return
    const controller = new AbortController()
    startController.current = controller
    setAiStatus('starting')
    setAiStartError(null)
    api.startAiSearch(controller.signal).then((response) => {
      if (controller.signal.aborted) return
      if (!response.available) throw new Error('Local AI search is unavailable.')
      setAiStatus('ready')
    }).catch((err: unknown) => {
      if (controller.signal.aborted) return
      setAiStatus('error')
      setAiStartError(err instanceof Error ? err.message : 'Could not start local AI search')
    })
    return () => { controller.abort() }
  }, [mode, startAttempt])

  const regularQuery = input.trim() === q.trim() && q.trim().length >= MIN_QUERY_LENGTH
    ? q.trim() : undefined
  const searchQuery = mode === 'regular' ? regularQuery : aiQuery?.query

  useEffect(() => {
    // Wait for the regular debounce and for explicit AI submission/readiness.
    if (!searchQuery || (mode === 'ai' && aiStatus !== 'ready')) {
      setLoading(false)
      setError(null)
      return
    }
    const query = searchQuery
    const controller = new AbortController()
    searchController.current = controller
    setResult(null)
    setError(null)
    setLoading(true)
    const request = mode === 'regular'
      ? api.searchExpenses({ q: query }, controller.signal)
      : api.searchExpensesAi(query, controller.signal)
    request.then((data) => {
      if (controller.signal.aborted) return
      setResult({ mode, query, data })
      setLoading(false)
    }).catch((err: unknown) => {
      if (controller.signal.aborted) return
      setError(err instanceof Error ? err.message : 'Search failed')
      setLoading(false)
    })
    return () => { controller.abort() }
  }, [mode, searchQuery, aiQuery, aiStatus, reloadKey])

  function switchMode(next: SearchMode) {
    if (next === mode) return
    modeVersion.current += 1
    startController.current?.abort()
    searchController.current?.abort()
    setMode(next)
    setResult(null)
    setLoading(false)
    setError(null)
    setAiQuery(null)
    setAiStatus(next === 'ai' ? 'starting' : 'idle')
    setAiStartError(null)
  }

  function changeRegularInput(value: string) {
    if (value.trim() !== input.trim()) {
      searchController.current?.abort()
      setResult(null)
      setLoading(false)
      setError(null)
    }
    setInput(value)
  }

  async function onChangeCategory(transactionId: string, categoryId: string) {
    const version = modeVersion.current
    setChangingId(transactionId)
    setError(null)
    try {
      await api.changeCategory(transactionId, categoryId)
      setReloadKey((key) => key + 1)
    } catch (err) {
      if (version === modeVersion.current) {
        setError(err instanceof Error ? err.message : 'Failed to change category')
      }
    } finally {
      setChangingId(null)
    }
  }

  const active = result?.mode === mode &&
    (mode === 'ai' ? result.query === aiQuery?.query : result.query === q.trim() && input.trim() === q.trim())
    ? result.data : null
  const isEmpty = active != null && active.expenseCount === 0 && active.excludedCount === 0
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
            Search your expenses with exact terms or ask a question using a local AI model.
            Totals use the same rules as the Expenses page: refunds reduce spend and transfers
            between accounts never count.
          </p>
        </div>
      </div>

      <div className="segmented search-mode" role="group" aria-label="Search mode">
        <button type="button" className={`segmented-item ${mode === 'regular' ? 'is-active' : ''}`}
          aria-pressed={mode === 'regular'} onClick={() => switchMode('regular')}>
          Regular search
        </button>
        <button type="button" className={`segmented-item ${mode === 'ai' ? 'is-active' : ''}`}
          aria-pressed={mode === 'ai'} onClick={() => switchMode('ai')}>
          AI search
        </button>
      </div>

      {mode === 'regular' ? (
        <>
          <div className="panel search-bar">
            <input
              type="search"
              value={input}
              placeholder="Search expenses — e.g. japan, travel, woolworths"
              aria-label="Search expenses"
              autoFocus
              onChange={(event) => changeRegularInput(event.target.value)}
            />
            {input.trim() !== q.trim() && <span className="search-pending">searching…</span>}
          </div>
          {input.trim().length < MIN_QUERY_LENGTH && (
            <div className="panel search-hints">
              <h2>Try a search</h2>
              <p className="muted">
                Every word must match a bank description or category name. Regular search is
                deterministic and does not use AI.
              </p>
              <div className="search-suggestions">
                {EXAMPLE_QUERIES.map((example) => (
                  <button key={example} type="button" className="search-chip"
                    onClick={() => changeRegularInput(example)}>{example}</button>
                ))}
              </div>
            </div>
          )}
        </>
      ) : (
        <>
          <div className="search-ai-info">
            <p>Ask in natural language. Your local Llama model receives transaction data, but not
              balances or account names; no data is sent to the cloud.</p>
            <p>AI-selected matches can be incomplete or inaccurate. The app calculates the amounts.
              Changing a category re-runs the submitted AI search.</p>
          </div>
          {aiStatus === 'starting' && <p role="status" className="muted">Starting local AI search…</p>}
          {aiStatus === 'ready' && <p role="status" className="success">Local AI search is ready.</p>}
          {aiStatus === 'error' && (
            <div className="error" role="alert">
              <p>Could not start local AI search: {aiStartError}</p>
              <button type="button" onClick={() => setStartAttempt((attempt) => attempt + 1)}>
                Retry startup
              </button>
            </div>
          )}
          <form className="panel search-bar" onSubmit={(event) => {
            event.preventDefault()
            const query = aiInput.trim()
            if (aiStatus !== 'ready' || !query) return
            searchController.current?.abort()
            setResult(null)
            setError(null)
            setLoading(true)
            setAiQuery({ query })
          }}>
            <input type="search" value={aiInput} aria-label="Ask AI about expenses"
              placeholder="e.g. What did I spend on holidays?"
              onChange={(event) => setAiInput(event.target.value)} />
            <button type="submit" disabled={aiStatus !== 'ready' || !aiInput.trim()}>
              Search with AI
            </button>
          </form>
        </>
      )}

      {error && <p className="error" role="alert">{error}</p>}
      {loading && <p className="lede" role="status">{mode === 'ai' ? 'Searching with local AI…' : 'Searching…'}</p>}
      {!loading && active && mode === 'ai' && (
        <div className="search-ai-result" role="note">
          <strong>AI-selected results for “{aiQuery?.query}”</strong>
          <p>Matches may be incomplete or inaccurate. Amounts are calculated by the app.</p>
        </div>
      )}

      {!loading && active && active.expenseCount + active.excludedCount > 0 && (
        <>
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

          {mode === 'regular' && active.suggestions.length > 0 && (
            <div className="panel">
              <h2>Matching merchants</h2>
              <div className="search-suggestions">
                {active.suggestions.map((suggestion) => (
                  <button
                    key={suggestion.label}
                    type="button"
                    className="search-chip"
                    title={`Narrow the search to ${suggestion.label}`}
                    onClick={() => changeRegularInput(suggestion.label)}
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
                      <td>{tx.description}</td>
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

      {!loading && isEmpty && (
        <div className="panel search-empty">
          <h2>{mode === 'ai' ? 'AI found no matching expenses' : `No expenses match “${q.trim()}”`}</h2>
          <p className="muted">
            {mode === 'ai'
              ? 'Try rephrasing your question, or use regular search to check exact terms.'
              : 'Search matches bank descriptions and category names. Try fewer or shorter words, or search for a merchant name.'}
          </p>
        </div>
      )}
    </section>
  )
}
