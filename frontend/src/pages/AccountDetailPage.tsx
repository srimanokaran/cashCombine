import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import type { Account, Category, Transaction } from '../types'

export function AccountDetailPage() {
  const { id = '' } = useParams()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [accountData, txData, categoryData] = await Promise.all([
        api.getAccount(id),
        api.listTransactions(id),
        api.listCategories(),
      ])
      setAccount(accountData)
      setTransactions(txData)
      setCategories(categoryData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load account')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [id])

  async function onChangeCategory(transactionId: string, categoryId: string) {
    setError(null)
    try {
      const updated = await api.changeCategory(transactionId, categoryId)
      setTransactions((current) =>
        current.map((tx) => (tx.id === updated.id ? updated : tx)),
      )
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to change category')
    }
  }

  function categoryName(categoryId: string) {
    return categories.find((c) => c.id === categoryId)?.name ?? categoryId
  }

  if (loading) {
    return (
      <section className="page">
        <p>Loading…</p>
      </section>
    )
  }

  if (!account) {
    return (
      <section className="page">
        <p className="error">{error ?? 'Account not found'}</p>
        <Link to="/">Back to import</Link>
      </section>
    )
  }

  return (
    <section className="page">
      <p className="crumb">
        <Link to="/">Import</Link> / {account.name}
      </p>
      <h1>{account.name}</h1>
      <p className="lede">
        {account.hasImports ? 'Has imports' : 'No imports yet'} — upload CSVs from the{' '}
        <Link to="/">Import</Link> page.
      </p>

      {error && <p className="error">{error}</p>}

      <h2>Transactions</h2>
      {transactions.length === 0 ? (
        <p className="muted">No transactions yet.</p>
      ) : (
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Amount</th>
                <th>Description</th>
                <th>Category</th>
              </tr>
            </thead>
            <tbody>
              {transactions.map((tx) => (
                <tr key={tx.id}>
                  <td>{tx.date}</td>
                  <td className={Number(tx.amount) < 0 ? 'negative' : 'positive'}>
                    {Number(tx.amount).toFixed(2)}
                  </td>
                  <td>{tx.description}</td>
                  <td>
                    <select
                      value={tx.categoryId}
                      onChange={(e) => void onChangeCategory(tx.id, e.target.value)}
                      aria-label={`Category for ${tx.description}`}
                    >
                      {categories.map((category) => (
                        <option key={category.id} value={category.id}>
                          {category.name}
                        </option>
                      ))}
                    </select>
                    <span className="meta">
                      {tx.categoryAssignmentSource === 'MANUAL' ? 'manual' : 'rule'}
                      {` · ${categoryName(tx.categoryId)}`}
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  )
}
