import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api'
import { IMPORTABLE_ACCOUNT_TYPES, type Account, type ImportResult } from '../types'

export function AccountsPage() {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [accountId, setAccountId] = useState('')
  const [file, setFile] = useState<File | null>(null)
  const [importResult, setImportResult] = useState<ImportResult | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)

  const selected = accounts.find((account) => account.id === accountId)
  const importSupported =
    selected != null && IMPORTABLE_ACCOUNT_TYPES.includes(selected.type)
  const canUpload = accountId !== '' && file != null && importSupported && !uploading

  async function load() {
    setLoading(true)
    setError(null)
    try {
      const fixed = await api.ensureFixedAccounts()
      setAccounts(fixed)
      if (accountId && !fixed.some((account) => account.id === accountId)) {
        setAccountId('')
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function onUpload(event: React.FormEvent) {
    event.preventDefault()
    if (!canUpload || !file) {
      return
    }
    setError(null)
    setImportResult(null)
    setUploading(true)
    try {
      const result = await api.importCsv(accountId, file)
      setImportResult(result)
      setFile(null)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Import failed')
    } finally {
      setUploading(false)
    }
  }

  return (
    <section className="page">
      <h1>Import</h1>
      <p className="lede">Pick an account, choose a CSV, then upload.</p>

      {error && <p className="error">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : (
        <form className="import-form" onSubmit={onUpload}>
          <div className="panel">
            <h2>Account</h2>
            <label className="field">
              <span>Bank / card</span>
              <select
                value={accountId}
                onChange={(e) => {
                  setAccountId(e.target.value)
                  setImportResult(null)
                }}
                required
              >
                <option value="" disabled>
                  Select account…
                </option>
                {accounts.map((account) => (
                  <option key={account.id} value={account.id}>
                    {account.name}
                  </option>
                ))}
              </select>
            </label>
            {selected && !importSupported && (
              <p className="muted">
                CSV import for {selected.name} is not wired up yet — CommBank works today.
              </p>
            )}
          </div>

          <div className="panel">
            <h2>CSV file</h2>
            <label className="field">
              <span>File</span>
              <input
                key={file?.name ?? 'no-file'}
                type="file"
                accept=".csv,text/csv"
                onChange={(e) => {
                  setFile(e.target.files?.[0] ?? null)
                  setImportResult(null)
                }}
              />
            </label>
            {file && <p className="meta">Selected: {file.name}</p>}
          </div>

          <button type="submit" disabled={!canUpload}>
            {uploading ? 'Uploading…' : 'Upload'}
          </button>
        </form>
      )}

      {importResult && selected && (
        <div className="panel">
          <p className="import-result">
            Accepted {importResult.accepted} · Duplicate {importResult.duplicate} · Rejected{' '}
            {importResult.rejected}
          </p>
          <p>
            <Link to={`/accounts/${selected.id}`}>View {selected.name} transactions</Link>
          </p>
        </div>
      )}

      {!loading && accounts.length > 0 && (
        <>
          <h2>Accounts</h2>
          <ul className="list compact">
            {accounts.map((account) => (
              <li key={account.id}>
                <div>
                  <Link to={`/accounts/${account.id}`}>{account.name}</Link>
                  <span className="meta">
                    {account.hasImports ? 'has imports' : 'no imports yet'}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  )
}
