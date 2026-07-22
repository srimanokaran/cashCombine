import { useEffect, useState } from 'react'
import { api } from '../api'
import { PieChart } from '../components/PieChart'
import { formatMoney } from '../format'
import type { ExpenseDashboard } from '../types'

const SPEND_COLORS = ['#7a73ff', '#2dd4bf', '#f59e0b', '#38bdf8', '#f472b6', '#a78bfa', '#34d399']
const INCOME_COLORS = ['#34d399', '#2dd4bf', '#a3e635', '#38bdf8', '#fbbf24']

export function ChartsDashboardPage() {
  const [dashboard, setDashboard] = useState<ExpenseDashboard | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await api.getExpenseDashboard()
        if (!cancelled) {
          setDashboard(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load dashboard')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }
    void load()
    return () => {
      cancelled = true
    }
  }, [])

  const net =
    dashboard == null ? 0 : Number(dashboard.totalIncome) - Number(dashboard.totalExpenses)

  const spendSlices =
    dashboard?.categories
      .filter((row) => Number(row.amount) > 0)
      .map((row, index) => ({
        label: row.categoryName,
        value: Number(row.amount),
        color: SPEND_COLORS[index % SPEND_COLORS.length],
      })) ?? []

  const incomeSlices =
    dashboard?.incomeCategories.map((row, index) => ({
      label: row.categoryName,
      value: Number(row.amount),
      color: INCOME_COLORS[index % INCOME_COLORS.length],
    })) ?? []

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Dashboard</h1>
          <p className="lede">Pie breakdown of spending and income across accounts.</p>
        </div>
      </div>

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
              <p className="summary-label">Total income</p>
              <p className="summary-value">{formatMoney(dashboard.totalIncome)}</p>
            </div>
            <div className="panel summary-card">
              <p className="summary-label">Net</p>
              <p className={`summary-value ${net < 0 ? 'negative' : net > 0 ? 'positive' : ''}`}>
                {formatMoney(net)}
              </p>
            </div>
          </div>

          <div className="chart-grid">
            <div className="panel">
              <h2>Spending</h2>
              <PieChart
                slices={spendSlices}
                centerLabel="Spent"
                emptyMessage="No expenses yet. Import a CSV to see your spending mix."
              />
            </div>
            <div className="panel">
              <h2>Income</h2>
              <PieChart
                slices={incomeSlices}
                centerLabel="Income"
                emptyMessage="No income yet. Credits in Income or Uncategorised show here."
              />
            </div>
          </div>
        </>
      )}
    </section>
  )
}
