import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { CategoryPicker } from '../components/CategoryPicker'
import { formatDate } from '../format'
import type { Account, Category, ImportBatch, Transaction } from '../types'

function formatImportedAt(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return date.toLocaleString()
}

export function AccountDetailPage() {
  const { id = '' } = useParams()
  const [account, setAccount] = useState<Account | null>(null)
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [imports, setImports] = useState<ImportBatch[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const [accountData, txData, categoryData, importData] = await Promise.all([
        api.getAccount(id),
        api.listTransactions(id),
        api.listCategories(),
        api.listImports(id),
      ])
      setAccount(accountData)
      setTransactions(txData)
      setCategories(categoryData)
      setImports(importData)
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
      await api.changeCategory(transactionId, categoryId)
      // Reload so sibling transactions updated by the new rule appear correctly.
      const txData = await api.listTransactions(id)
      setTransactions(txData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to change category')
    }
  }

  async function onDeleteImport(batch: ImportBatch) {
    const label = batch.filename ?? 'this import'
    if (
      !window.confirm(
        `Delete ${label}? This removes ${batch.accepted} transaction${batch.accepted === 1 ? '' : 's'} from this upload.`,
      )
    ) {
      return
    }
    setError(null)
    try {
      await api.deleteImport(id, batch.id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete import')
    }
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
        <Link to="/">Import</Link> page. Changing a category also creates a rule for that description.
      </p>

      {error && <p className="error">{error}</p>}

      <h2>Imports</h2>
      {imports.length === 0 ? (
        <p className="muted">
          {account.hasImports
            ? 'No tracked uploads yet. New CSV imports will appear here and can be deleted.'
            : 'No imports yet.'}
        </p>
      ) : (
        <ul className="list compact">
          {imports.map((batch) => (
            <li key={batch.id}>
              <div>
                <strong>{batch.filename ?? 'CSV upload'}</strong>
                <span className="meta">
                  {formatImportedAt(batch.importedAt)} · accepted {batch.accepted} · duplicate{' '}
                  {batch.duplicate} · rejected {batch.rejected}
                </span>
              </div>
              <button type="button" className="danger" onClick={() => void onDeleteImport(batch)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}

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
                  <td>{formatDate(tx.date)}</td>
                  <td className={Number(tx.amount) < 0 ? 'negative' : 'positive'}>
                    {Number(tx.amount).toFixed(2)}
                  </td>
                  <td>{tx.description}</td>
                  <td>
                    <CategoryPicker
                      categories={categories}
                      value={tx.categoryId}
                      ariaLabel={`Category for ${tx.description}`}
                      onChange={(categoryId) => onChangeCategory(tx.id, categoryId)}
                    />
                    <span className="meta">
                      {tx.categoryAssignmentSource === 'MANUAL' ? 'manual' : 'rule'}
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
