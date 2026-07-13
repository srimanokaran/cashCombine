import { useEffect, useState } from 'react'
import { api } from '../api'
import type { ExpenseDashboard } from '../types'

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

  useEffect(() => {
    void (async () => {
      setLoading(true)
      setError(null)
      try {
        setDashboard(await api.getExpenseDashboard())
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load dashboard')
      } finally {
        setLoading(false)
      }
    })()
  }, [])

  return (
    <section className="page">
      <h1>Expenses</h1>
      <p className="lede">Spending across all accounts, grouped by category.</p>

      {error && <p className="error">{error}</p>}
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
                {dashboard.categories.map((row, index) => (
                  <li key={row.categoryId}>
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
                      </span>
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </div>
        </>
      )}
    </section>
  )
}
