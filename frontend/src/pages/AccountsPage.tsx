import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import type { Account } from '../types'

export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [name, setName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError(null)
    try {
      setAccounts(await api.listAccounts())
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function onCreate(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      await api.createAccount(name.trim(), 'COMMBANK')
      setName('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create account')
    }
  }

  async function onDelete(account: Account) {
    if (!window.confirm(`Delete account "${account.name}" and all its transactions?`)) {
      return
    }
    setError(null)
    try {
      await api.deleteAccount(account.id)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete account')
    }
  }

  return (
    <section className="page">
      <h1>Accounts</h1>
      <p className="lede">Create an account, then import a CommBank CSV.</p>

      <form className="row-form" onSubmit={onCreate}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Account name"
          required
        />
        <select value="COMMBANK" disabled aria-label="Account type">
          <option value="COMMBANK">CommBank</option>
        </select>
        <button type="submit">Create</button>
      </form>

      {error && <p className="error">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : accounts.length === 0 ? (
        <p className="muted">No accounts yet.</p>
      ) : (
        <ul className="list">
          {accounts.map((account) => (
            <li key={account.id}>
              <div>
                <Link to={`/accounts/${account.id}`}>{account.name}</Link>
                <span className="meta">
                  {account.type}
                  {account.hasImports ? ' · has imports' : ''}
                </span>
              </div>
              <button type="button" className="danger" onClick={() => void onDelete(account)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
