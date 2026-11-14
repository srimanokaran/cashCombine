import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { api } from '../api'
import { SPEND_COLORS } from '../colors'
import { formatMoney, formatMonth } from '../format'
import type { CategoryTrendsResponse } from '../types'

type RangePreset = '6m' | '12m' | '24m' | 'all'
type ChartMode = 'stacked' | 'line'

const RANGE_PRESETS: Array<{ value: RangePreset; label: string }> = [
  { value: '6m', label: '6 months' },
  { value: '12m', label: '12 months' },
  { value: '24m', label: '24 months' },
  { value: 'all', label: 'All time' },
]

const TOOLTIP_STYLE = {
  background: '#1a2234',
  border: '1px solid rgba(255,255,255,0.1)',
  borderRadius: 8,
}

function isoDate(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function rangeFor(preset: RangePreset): { from?: string; to?: string } {
  if (preset === 'all') {
    return {}
  }
  const months = preset === '6m' ? 6 : preset === '12m' ? 12 : 24
  const now = new Date()
  const from = new Date(now.getFullYear(), now.getMonth() - (months - 1), 1)
  const to = new Date(now.getFullYear(), now.getMonth() + 1, 0)
  return { from: isoDate(from), to: isoDate(to) }
}

function currencyTick(value: number) {
  return Number(value).toLocaleString('en-AU', {
    style: 'currency',
    currency: 'AUD',
    maximumFractionDigits: 0,
  })
}

function currentMonth() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

export function CategoryTrendsPage() {
  const navigate = useNavigate()
  const [preset, setPreset] = useState<RangePreset>('12m')
  const [mode, setMode] = useState<ChartMode>('stacked')
  const [report, setReport] = useState<CategoryTrendsResponse | null>(null)
  const [hidden, setHidden] = useState<Set<string>>(new Set())
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const data = await api.getCategoryTrends(rangeFor(preset))
        if (!cancelled) {
          setReport(data)
          setHidden(new Set())
        }
      } catch (err) {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Failed to load category trends')
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
  }, [preset])

  const categories = report?.categories ?? []
  const months = report?.months ?? []

  const rows = months.map((month) => {
    const row: Record<string, string | number> = {
      month: month.month,
      totalSpend: Number(month.totalSpend),
    }
    for (const category of categories) {
      row[category.categoryId] = 0
    }
    for (const slice of month.slices) {
      row[slice.categoryId] = Number(slice.amount)
    }
    return row
  })

  const colorById: Record<string, string> = {}
  categories.forEach((category, index) => {
    colorById[category.categoryId] = SPEND_COLORS[index % SPEND_COLORS.length]
  })

  const visibleCategories = categories.filter((category) => !hidden.has(category.categoryId))
  const totalSpend = months.reduce((sum, month) => sum + Number(month.totalSpend), 0)
  const averageSpend = months.length > 0 ? totalSpend / months.length : 0
  const topCategory = categories[0]
  const includesCurrentMonth = months.some((month) => month.month === currentMonth())
  const hasNegativeCategory = categories.some((category) => Number(category.total) < 0)

  function toggleCategory(categoryId: string) {
    setHidden((previous) => {
      const next = new Set(previous)
      if (next.has(categoryId)) {
        next.delete(categoryId)
      } else {
        next.add(categoryId)
      }
      return next
    })
  }

  function openMonth(state: unknown) {
    const payload = state as { activePayload?: Array<{ payload?: { month?: string } }> }
    const month = payload.activePayload?.[0]?.payload?.month
    if (month) {
      void navigate(`/expenses?month=${month}`)
    }
  }

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Category trends</h1>
          <p className="lede">
            Monthly spend by category. Click a month to open its breakdown on Expenses.
          </p>
        </div>
      </div>

      <div className="trend-controls">
        <div className="segmented" role="group" aria-label="Time range">
          {RANGE_PRESETS.map((option) => (
            <button
              key={option.value}
              type="button"
              className={`segmented-item${preset === option.value ? ' is-active' : ''}`}
              aria-pressed={preset === option.value}
              onClick={() => setPreset(option.value)}
            >
              {option.label}
            </button>
          ))}
        </div>
        <div className="segmented" role="group" aria-label="Chart type">
          <button
            type="button"
            className={`segmented-item${mode === 'stacked' ? ' is-active' : ''}`}
            aria-pressed={mode === 'stacked'}
            onClick={() => setMode('stacked')}
          >
            Stacked
          </button>
          <button
            type="button"
            className={`segmented-item${mode === 'line' ? ' is-active' : ''}`}
            aria-pressed={mode === 'line'}
            onClick={() => setMode('line')}
          >
            Lines
          </button>
        </div>
      </div>

      {error && <p className="error">{error}</p>}
      {loading && <p className="lede">Loading…</p>}

      {!loading && report && months.length === 0 && (
        <div className="panel">
          <p className="muted" style={{ margin: 0 }}>
            No spending in this range. Import a CSV or widen the range to see category trends.
          </p>
        </div>
      )}

      {!loading && months.length > 0 && (
        <>
          <div className="dashboard-summary">
            <div className="panel summary-card">
              <p className="summary-label">Total spend</p>
              <p className="summary-value">{formatMoney(totalSpend)}</p>
            </div>
            <div className="panel summary-card">
              <p className="summary-label">Monthly average</p>
              <p className="summary-value">{formatMoney(averageSpend)}</p>
            </div>
            <div className="panel summary-card">
              <p className="summary-label">Top category</p>
              <p className="summary-value summary-category">{topCategory?.categoryName ?? '—'}</p>
              {topCategory && <p className="meta">{formatMoney(topCategory.total)}</p>}
            </div>
          </div>

          <div className="panel">
            <h2>{mode === 'stacked' ? 'Spend by category' : 'Category comparison'}</h2>
            <div className="trend-chart">
              <ResponsiveContainer width="100%" height={360}>
                {mode === 'stacked' ? (
                  <BarChart
                    data={rows}
                    margin={{ top: 8, right: 8, left: 8, bottom: 8 }}
                    onClick={openMonth}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                    <XAxis
                      dataKey="month"
                      tickFormatter={(value) => formatMonth(String(value))}
                      tick={{ fill: 'var(--navy-muted)', fontSize: 12 }}
                    />
                    <YAxis
                      width={72}
                      tickFormatter={currencyTick}
                      tick={{ fill: 'var(--navy-muted)', fontSize: 12 }}
                    />
                    <Tooltip
                      formatter={(value) => formatMoney(Number(value ?? 0))}
                      labelFormatter={(label) => formatMonth(String(label))}
                      contentStyle={TOOLTIP_STYLE}
                      cursor={{ fill: 'rgba(255,255,255,0.04)' }}
                    />
                    {visibleCategories.map((category) => (
                      <Bar
                        key={category.categoryId}
                        dataKey={category.categoryId}
                        name={category.categoryName}
                        stackId="spend"
                        fill={colorById[category.categoryId]}
                        cursor="pointer"
                      />
                    ))}
                  </BarChart>
                ) : (
                  <LineChart
                    data={rows}
                    margin={{ top: 8, right: 8, left: 8, bottom: 8 }}
                    onClick={openMonth}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.08)" />
                    <XAxis
                      dataKey="month"
                      tickFormatter={(value) => formatMonth(String(value))}
                      tick={{ fill: 'var(--navy-muted)', fontSize: 12 }}
                    />
                    <YAxis
                      width={72}
                      tickFormatter={currencyTick}
                      tick={{ fill: 'var(--navy-muted)', fontSize: 12 }}
                    />
                    <Tooltip
                      formatter={(value) => formatMoney(Number(value ?? 0))}
                      labelFormatter={(label) => formatMonth(String(label))}
                      contentStyle={TOOLTIP_STYLE}
                    />
                    {visibleCategories.map((category) => (
                      <Line
                        key={category.categoryId}
                        type="monotone"
                        dataKey={category.categoryId}
                        name={category.categoryName}
                        stroke={colorById[category.categoryId]}
                        strokeWidth={2}
                        dot={false}
                        activeDot={{ r: 4 }}
                      />
                    ))}
                  </LineChart>
                )}
              </ResponsiveContainer>
            </div>

            <div className="trend-legend">
              {categories.map((category) => {
                const isHidden = hidden.has(category.categoryId)
                return (
                  <button
                    key={category.categoryId}
                    type="button"
                    className={`trend-legend-item${isHidden ? ' is-hidden' : ''}`}
                    aria-pressed={!isHidden}
                    onClick={() => toggleCategory(category.categoryId)}
                  >
                    <span
                      className="trend-swatch"
                      style={{ background: colorById[category.categoryId] }}
                    />
                    {category.categoryName}
                  </button>
                )
              })}
            </div>

            {includesCurrentMonth && (
              <p className="trend-note">
                The current month is still in progress, so its {mode === 'stacked' ? 'bar' : 'point'} is
                partial.
              </p>
            )}
            {hasNegativeCategory && (
              <p className="trend-note">
                Categories with more refunds than spend appear below zero. Totals are net of refunds.
              </p>
            )}
          </div>
        </>
      )}
    </section>
  )
}