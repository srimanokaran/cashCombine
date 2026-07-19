import { useCallback, useEffect, useState } from 'react'
import { api } from '../api'
import { AnimatedExpand } from '../components/AnimatedExpand'
import { CategoryPicker } from '../components/CategoryPicker'
import { formatDate, formatMoney } from '../format'
import type { Category, CategorySpend, ExpenseDashboard, ExpenseTransaction } from '../types'

const BAR_COLORS = ['#7a73ff', '#2dd4bf', '#f59e0b', '#38bdf8', '#f472b6', '#a78bfa', '#34d399']
const INCOME_BAR_COLORS = ['#34d399', '#2dd4bf', '#a3e635', '#38bdf8', '#fbbf24']

type BreakdownSide = 'expense' | 'income'

export function DashboardPage() {
  const [dashboard, setDashboard] = useState<ExpenseDashboard | null>(null)
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [expandedSide, setExpandedSide] = useState<BreakdownSide | null>(null)
  const [expandedCategoryId, setExpandedCategoryId] = useState<string | null>(null)
  const [expandedTxs, setExpandedTxs] = useState<ExpenseTransaction[] | null>(null)
  const [expandedLoading, setExpandedLoading] = useState(false)
  const [expandedError, setExpandedError] = useState<string | null>(null)
  const [reanalysing, setReanalysing] = useState(false)
  const [reanalyseMessage, setReanalyseMessage] = useState<string | null>(null)
  const [changingCategoryId, setChangingCategoryId] = useState<string | null>(null)

  const loadDashboard = useCallback(async (options?: { quiet?: boolean }) => {
    const quiet = options?.quiet ?? false
    if (!quiet) {
      setLoading(true)
    }
    setError(null)
    try {
      const [dashboardData, categoryData] = await Promise.all([
        api.getExpenseDashboard(),
        api.listCategories(),
      ])
      setDashboard(dashboardData)
      setCategories(categoryData)
      return dashboardData
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load dashboard')
      return null
    } finally {
      if (!quiet) {
        setLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    void loadDashboard()
  }, [loadDashboard])

  async function loadExpandedTransactions(
    side: BreakdownSide,
    categoryId: string,
    options?: { quiet?: boolean },
  ) {
    const quiet = options?.quiet ?? false
    if (!quiet) {
      setExpandedLoading(true)
    }
    setExpandedError(null)
    try {
      const txs =
        side === 'expense'
          ? await api.listExpenseTransactions(categoryId)
          : await api.listIncomeTransactions(categoryId)
      setExpandedTxs(txs)
    } catch (err) {
      setExpandedTxs(null)
      setExpandedError(err instanceof Error ? err.message : 'Failed to load transactions')
    } finally {
      if (!quiet) {
        setExpandedLoading(false)
      }
    }
  }

  async function onToggleCategory(side: BreakdownSide, row: CategorySpend) {
    if (expandedSide === side && expandedCategoryId === row.categoryId) {
      setExpandedSide(null)
      setExpandedCategoryId(null)
      setExpandedTxs(null)
      setExpandedError(null)
      return
    }

    setExpandedSide(side)
    setExpandedCategoryId(row.categoryId)
    setExpandedTxs(null)
    await loadExpandedTransactions(side, row.categoryId)
  }

  async function onChangeCategory(transactionId: string, categoryId: string) {
    setChangingCategoryId(transactionId)
    setExpandedError(null)
    setError(null)
    try {
      await api.changeCategory(transactionId, categoryId)
      setReanalyseMessage('Category updated — a rule was saved for that description.')
      const nextDashboard = await loadDashboard({ quiet: true })
      if (!nextDashboard) {
        return
      }
      const inIncome = nextDashboard.incomeCategories.some((row) => row.categoryId === categoryId)
      const inExpense = nextDashboard.categories.some((row) => row.categoryId === categoryId)
      if (inIncome) {
        setExpandedSide('income')
        setExpandedCategoryId(categoryId)
        await loadExpandedTransactions('income', categoryId, { quiet: true })
        return
      }
      if (inExpense) {
        setExpandedSide('expense')
        setExpandedCategoryId(categoryId)
        await loadExpandedTransactions('expense', categoryId, { quiet: true })
        return
      }
      setExpandedSide(null)
      setExpandedCategoryId(null)
      setExpandedTxs(null)
    } catch (err) {
      setExpandedError(err instanceof Error ? err.message : 'Failed to change category')
    } finally {
      setChangingCategoryId(null)
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
      setExpandedSide(null)
      setExpandedCategoryId(null)
      setExpandedTxs(null)
      await loadDashboard()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to re-analyse')
    } finally {
      setReanalysing(false)
    }
  }

  const net =
    dashboard == null ? 0 : Number(dashboard.totalIncome) - Number(dashboard.totalExpenses)

  return (
    <section className="page">
      <div className="page-heading">
        <div>
          <h1>Expenses</h1>
          <p className="lede">
            Spending and income across all accounts, grouped by category. Credits in Income or
            Uncategorised show under income; put a refund or payback under an expense category
            (e.g. Entertainment) to cancel that spend. Changing a category also creates a rule.
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

          <BreakdownPanel
            title="Spending by category"
            emptyMessage="No expenses yet. Import a CSV to see your spending mix."
            side="expense"
            rows={dashboard.categories}
            barColors={BAR_COLORS}
            expandedSide={expandedSide}
            expandedCategoryId={expandedCategoryId}
            expandedTxs={expandedTxs}
            expandedLoading={expandedLoading}
            expandedError={expandedError}
            categories={categories}
            changingCategoryId={changingCategoryId}
            onToggleCategory={onToggleCategory}
            onChangeCategory={onChangeCategory}
          />

          <BreakdownPanel
            title="Income by category"
            emptyMessage="No income yet. Credits in Income or Uncategorised show here."
            side="income"
            rows={dashboard.incomeCategories}
            barColors={INCOME_BAR_COLORS}
            expandedSide={expandedSide}
            expandedCategoryId={expandedCategoryId}
            expandedTxs={expandedTxs}
            expandedLoading={expandedLoading}
            expandedError={expandedError}
            categories={categories}
            changingCategoryId={changingCategoryId}
            onToggleCategory={onToggleCategory}
            onChangeCategory={onChangeCategory}
          />
        </>
      )}
    </section>
  )
}

function BreakdownPanel({
  title,
  emptyMessage,
  side,
  rows,
  barColors,
  expandedSide,
  expandedCategoryId,
  expandedTxs,
  expandedLoading,
  expandedError,
  categories,
  changingCategoryId,
  onToggleCategory,
  onChangeCategory,
}: {
  title: string
  emptyMessage: string
  side: BreakdownSide
  rows: CategorySpend[]
  barColors: string[]
  expandedSide: BreakdownSide | null
  expandedCategoryId: string | null
  expandedTxs: ExpenseTransaction[] | null
  expandedLoading: boolean
  expandedError: string | null
  categories: Category[]
  changingCategoryId: string | null
  onToggleCategory: (side: BreakdownSide, row: CategorySpend) => void | Promise<void>
  onChangeCategory: (transactionId: string, categoryId: string) => void | Promise<void>
}) {
  return (
    <div className="panel">
      <h2>{title}</h2>
      {rows.length === 0 ? (
        <p className="muted" style={{ margin: 0 }}>
          {emptyMessage}
        </p>
      ) : (
        <ul className="spend-breakdown">
          {rows.map((row, index) => (
            <SpendCategoryItem
              key={`${side}-${row.categoryId}`}
              side={side}
              row={row}
              index={index}
              barColors={barColors}
              expanded={expandedSide === side && expandedCategoryId === row.categoryId}
              expandedTxs={expandedTxs}
              expandedLoading={expandedLoading}
              expandedError={expandedError}
              categories={categories}
              changingCategoryId={changingCategoryId}
              onToggleCategory={onToggleCategory}
              onChangeCategory={onChangeCategory}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

function SpendCategoryItem({
  side,
  row,
  index,
  barColors,
  expanded,
  expandedTxs,
  expandedLoading,
  expandedError,
  categories,
  changingCategoryId,
  onToggleCategory,
  onChangeCategory,
}: {
  side: BreakdownSide
  row: CategorySpend
  index: number
  barColors: string[]
  expanded: boolean
  expandedTxs: ExpenseTransaction[] | null
  expandedLoading: boolean
  expandedError: string | null
  categories: Category[]
  changingCategoryId: string | null
  onToggleCategory: (side: BreakdownSide, row: CategorySpend) => void | Promise<void>
  onChangeCategory: (transactionId: string, categoryId: string) => void | Promise<void>
}) {
  const [cachedTxs, setCachedTxs] = useState<ExpenseTransaction[] | null>(null)
  const [cachedLoading, setCachedLoading] = useState(false)
  const [cachedError, setCachedError] = useState<string | null>(null)

  useEffect(() => {
    if (!expanded) {
      return
    }
    setCachedTxs(expandedTxs)
    setCachedLoading(expandedLoading)
    setCachedError(expandedError)
  }, [expanded, expandedTxs, expandedLoading, expandedError])

  const detailTxs = expanded ? expandedTxs : cachedTxs
  const detailLoading = expanded ? expandedLoading : cachedLoading
  const detailError = expanded ? expandedError : cachedError

  return (
    <li className={expanded ? 'spend-item expanded' : 'spend-item'}>
      <button
        type="button"
        className="spend-toggle"
        aria-expanded={expanded}
        onClick={() => void onToggleCategory(side, row)}
      >
        <div className="spend-row-top">
          <span className="spend-name">
            <span
              className="spend-dot"
              style={{ background: barColors[index % barColors.length] }}
            />
            {row.categoryName}
          </span>
          <span className="spend-row-trailing">
            <span className={Number(row.amount) < 0 ? 'spend-amount positive' : 'spend-amount'}>
              {Number(row.amount) < 0
                ? `${formatMoney(Math.abs(Number(row.amount)))} profit`
                : formatMoney(row.amount)}
            </span>
            <span className="spend-chevron" aria-hidden>
              ▾
            </span>
          </span>
        </div>
        <div className="spend-bar-track">
          <div
            className="spend-bar-fill"
            style={{
              width: `${Math.max(
                Number(row.amount) < 0
                  ? Math.abs(Number(row.percent)) || Math.min(Math.abs(Number(row.amount)), 100)
                  : Number(row.percent),
                Number(row.amount) === 0 ? 0 : 1,
              )}%`,
              background:
                Number(row.amount) < 0 ? '#34d399' : barColors[index % barColors.length],
            }}
          />
        </div>
        <div className="spend-row-meta">
          <span>
            {Number(row.amount) < 0 ? 'surplus' : `${Number(row.percent).toFixed(1)}%`}
          </span>
          <span>
            {row.transactionCount} transaction
            {row.transactionCount === 1 ? '' : 's'}
            {expanded ? ' · hide' : ' · view'}
          </span>
        </div>
      </button>

      <AnimatedExpand open={expanded}>
        <div className="spend-detail">
          {detailLoading && (
            <div className="spend-detail-loading" aria-live="polite">
              <div className="spend-skeleton-row" />
              <div className="spend-skeleton-row" />
              <div className="spend-skeleton-row short" />
            </div>
          )}
          {detailError && <p className="error">{detailError}</p>}
          {!detailLoading && !detailError && detailTxs && detailTxs.length === 0 && (
            <p className="muted">No transactions in this category.</p>
          )}
          {!detailLoading && !detailError && detailTxs && detailTxs.length > 0 && (
            <div className="table-wrap spend-detail-table">
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
                  {detailTxs.map((tx, txIndex) => (
                    <tr
                      key={tx.id}
                      className="spend-detail-row"
                      style={{ animationDelay: `${Math.min(txIndex, 8) * 30}ms` }}
                    >
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
                          value={row.categoryId}
                          disabled={changingCategoryId === tx.id}
                          ariaLabel={`Category for ${tx.description}`}
                          onChange={(categoryId) => onChangeCategory(tx.id, categoryId)}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </AnimatedExpand>
    </li>
  )
}

