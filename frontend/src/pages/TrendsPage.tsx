import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api'
import { formatMoney, formatMonth } from '../format'
import type { MonthlyCashflow } from '../types'

export function TrendsPage() {
  const navigate = useNavigate()
  const [months, setMonths] = useState<MonthlyCashflow[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await api.getMonthlyCashflow()
        if (!cancelled) {
          setMonths(data)
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load trends')
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

  const chartData = months.map((row) => ({
    month: row.month,
    label: formatMonth(row.month),
    spend: Number(row.totalExpenses),
    income: Number(row.totalIncome),
    net: Number(row.net),
  }))

  function openMonth(month: string) {
    void navigate(`/expenses?month=${month}`)
  }

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Trends</h1>
          <p className="lede">
            Month-by-month spend, income, and net across accounts. Click a month to open its category
            breakdown on Expenses.
          </p>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p className="lede">Loading…</p>}

      {!loading && months.length === 0 && (
        <div className="panel">
          <p className="muted" style={{ margin: 0 }}>
            No transactions yet. Import a CSV spanning one or more months to see trends over time.
          </p>
        </div>
      )}

      {!loading && months.length > 0 && (
        <>
          <div className="panel trends-chart-panel">
            <h2>Spend vs income</h2>
            <div className="trends-chart">
              <ResponsiveContainer width="100%" height={320}>
                <BarChart
                  data={chartData}
                  margin={{ top: 8, right: 8, left: 8, bottom: 8 }}
                  onClick={(state) => {
                    const payload = state as {
                      activePayload?: Array<{ payload?: { month?: string } }>
                    }
                    const month = payload.activePayload?.[0]?.payload?.month
                    if (month) {
                      openMonth(month)
                    }
                  }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                  <XAxis dataKey="label" tick={{ fill: 'var(--navy-muted)', fontSize: 12 }} />
                  <YAxis
                    tick={{ fill: 'var(--navy-muted)', fontSize: 12 }}
                    tickFormatter={(value: number) =>
                      Number(value).toLocaleString('en-AU', {
                        style: 'currency',
                        currency: 'AUD',
                        maximumFractionDigits: 0,
                      })
                    }
                  />
                  <Tooltip
                    formatter={(value) => formatMoney(Number(value ?? 0))}
                    labelFormatter={(label) => String(label)}
                    contentStyle={{
                      background: '#1a2234',
                      border: '1px solid rgba(255,255,255,0.1)',
                      borderRadius: 8,
                    }}
                  />
                  <Legend />
                  <Bar dataKey="spend" name="Spend" fill="#f472b6" radius={[4, 4, 0, 0]} cursor="pointer" />
                  <Bar dataKey="income" name="Income" fill="#34d399" radius={[4, 4, 0, 0]} cursor="pointer" />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="panel">
            <h2>By month</h2>
            <div className="table-wrap">
              <table className="trends-table">
                <thead>
                  <tr>
                    <th>Month</th>
                    <th>Spend</th>
                    <th>Income</th>
                    <th>Net</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {[...months].reverse().map((row) => {
                    const net = Number(row.net)
                    return (
                      <tr key={row.month} className="trends-row" onClick={() => openMonth(row.month)}>
                        <td>{formatMonth(row.month)}</td>
                        <td>{formatMoney(row.totalExpenses)}</td>
                        <td>{formatMoney(row.totalIncome)}</td>
                        <td className={net < 0 ? 'negative' : net > 0 ? 'positive' : ''}>
                          {formatMoney(row.net)}
                        </td>
                        <td>
                          <Link
                            to={`/expenses?month=${row.month}`}
                            onClick={(e) => e.stopPropagation()}
                          >
                            View →
                          </Link>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </>
      )}
    </section>
  )
}
