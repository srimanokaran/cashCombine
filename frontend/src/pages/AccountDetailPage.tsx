import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import type { Account, Category, ImportResult, Transaction } from '../types'

export function AccountDetailPage() {
  const { id = '' } = useParams()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [importResult, setImportResult] = useState<ImportResult | null>(null)
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

  async function onImport(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }
    setError(null)
    setImportResult(null)
    try {
      const result = await api.importCsv(id, file)
      setImportResult(result)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed')
    } finally {
      event.target.value = ''
    }
  }

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
        <Link to="/">Back to accounts</Link>
      </section>
    )
  }

  return (
    <section className="page">
      <p className="crumb">
        <Link to="/">Accounts</Link> / {account.name}
      </p>
      <h1>{account.name}</h1>
      <p className="lede">
        {account.type}
        {account.hasImports ? ' · has imports' : ' · no imports yet'}
      </p>

      <div className="panel">
        <h2>Import CSV</h2>
        <input type="file" accept=".csv,text/csv" onChange={onImport} />
        {importResult && (
          <p className="import-result">
            Accepted {importResult.accepted} · Duplicate {importResult.duplicate} · Rejected{' '}
            {importResult.rejected}
          </p>
        )}
      </div>

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
