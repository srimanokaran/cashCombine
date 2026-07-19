import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import type { CategorySpend, ExpenseDashboard, ExpenseTransaction } from '../types'

const BAR_COLORS = ['#7a73ff', '#2dd4bf', '#f59e0b', '#38bdf8', '#f472b6', '#a78bfa', '#34d399']

function formatMoney(value: number | string) {
  return Number(value).toLocaleString('en-AU', {
    style: 'currency',
    currency: 'AUD',
  })
}

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<ExpenseDashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [expandedCategoryId, setExpandedCategoryId] = useState<string | null>(null)
  const [expandedTxs, setExpandedTxs] = useState<ExpenseTransaction[] | null>(null)
  const [expandedLoading, setExpandedLoading] = useState(false)
  const [expandedError, setExpandedError] = useState<string | null>(null)
  const [reanalysing, setReanalysing] = useState(false)
  const [reanalyseMessage, setReanalyseMessage] = useState<string | null>(null)

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      setDashboard(await api.getExpenseDashboard())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  async function onToggleCategory(row: CategorySpend) {
    if (expandedCategoryId === row.categoryId) {
      setExpandedCategoryId(null)
      setExpandedTxs(null)
      setExpandedError(null)
      return
    }

    setExpandedCategoryId(row.categoryId)
    setExpandedTxs(null)
    setExpandedError(null)
    setExpandedLoading(true)
    try {
      setExpandedTxs(await api.listExpenseTransactions(row.categoryId))
    } catch (err) {
      setExpandedError(err instanceof Error ? err.message : 'Failed to load transactions')
    } finally {
      setExpandedLoading(false)
    }
  }

  async function onReanalyse() {
    if (
      !window.confirm(
        'Re-analyse all transactions with current category rules? Manual category overrides will be kept.',
      )
    ) {
      return
    }

    setReanalysing(true)
    setReanalyseMessage(null)
    setError(null)
    try {
      const result = await api.reanalyseExpenses()
      setReanalyseMessage(
        `Updated ${result.updated} of ${result.examined} transactions` +
          (result.skippedManual > 0 ? ` (${result.skippedManual} manual left unchanged)` : ''),
      )
      setExpandedCategoryId(null)
      setExpandedTxs(null)
      await loadDashboard()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to re-analyse')
    } finally {
      setReanalysing(false)
    }
  }

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Expenses</h1>
          <p className="lede">
            Spending across all accounts, grouped by category. Click a category to see its transactions.
          </p>
        </div>
        <button type="button" onClick={() => void onReanalyse()} disabled={reanalysing || loading}>
          {reanalysing ? 'Re-analysing…' : 'Re-analyse categories'}
        </button>
      </div>

      {error && <p className="error">{error}</p>}
      {reanalyseMessage && <p className="success">{reanalyseMessage}</p>}
      {loading && <p className="lede">Loading…</p>}

      {!loading && dashboard && (
        <>
          <div className="dashboard-summary">
            <div className="panel summary-card">
              <p className="summary-label">Total spent</p>
              <p className="summary-value">{formatMoney(dashboard.totalExpenses)}</p>
            </div>
            <div className="panel summary-card">
              <p className="summary-label">Expense transactions</p>
              <p className="summary-value">{dashboard.expenseTransactionCount}</p>
            </div>
            <div className="panel summary-card">
              <p className="summary-label">Categories</p>
              <p className="summary-value">{dashboard.categories.length}</p>
            </div>
          </div>

          <div className="panel">
            <h2>Breakdown by category</h2>
            {dashboard.categories.length === 0 ? (
              <p className="muted" style={{ margin: 0 }}>
                No expenses yet. Import a CSV to see your spending mix.
              </p>
            ) : (
              <ul className="spend-breakdown">
                {dashboard.categories.map((row, index) => {
                  const expanded = expandedCategoryId === row.categoryId
                  return (
                    <li key={row.categoryId} className={expanded ? 'spend-item expanded' : 'spend-item'}>
                      <button
                        type="button"
                        className="spend-toggle"
                        aria-expanded={expanded}
                        onClick={() => void onToggleCategory(row)}
                      >
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
                              width: `${Math.max(Number(row.percent), 1)}%`,
                              background: BAR_COLORS[index % BAR_COLORS.length],
                            }}
                          />
                        </div>
                        <div className="spend-row-meta">
                          <span>{Number(row.percent).toFixed(1)}%</span>
                          <span>
                            {row.transactionCount} transaction
                            {row.transactionCount === 1 ? '' : 's'}
                            {expanded ? ' · hide' : ' · view'}
                          </span>
                        </div>
                      </button>

                      {expanded && (
                        <div className="spend-detail">
                          {expandedLoading && <p className="muted">Loading transactions…</p>}
                          {expandedError && <p className="error">{expandedError}</p>}
                          {!expandedLoading && !expandedError && expandedTxs && expandedTxs.length === 0 && (
                            <p className="muted">No transactions in this category.</p>
                          )}
                          {!expandedLoading && !expandedError && expandedTxs && expandedTxs.length > 0 && (
                            <div className="table-wrap">
                              <table>
                                <thead>
                                  <tr>
                                    <th>Date</th>
                                    <th>Account</th>
                                    <th>Description</th>
                                    <th>Amount</th>
                                  </tr>
                                </thead>
                                <tbody>
                                  {expandedTxs.map((tx) => (
                                    <tr key={tx.id}>
                                      <td>{tx.date}</td>
                                      <td>{tx.accountName}</td>
                                      <td>{tx.description}</td>
                                      <td className="negative">{formatMoney(tx.amount)}</td>
                                    </tr>
                                  ))}
                                </tbody>
                              </table>
                            </div>
                          )}
                        </div>
                      )}
                    </li>
                  )
                })}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  )
}
