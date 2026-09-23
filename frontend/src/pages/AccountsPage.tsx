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
  const [dragging, setDragging] = useState(false)

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
      <p className="lede">
        Choose an account, select a CSV, then upload. CommBank and Qantas Money (NAB credit card)
        exports are supported.
      </p>

      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="lede">Loading…</p>
      ) : (
        <form className="import-form" onSubmit={onUpload}>
          <div className="panel">
            <h2>Account</h2>
            <div className="account-picker" role="radiogroup" aria-label="Bank or card">
              {accounts.map((account) => (
                <button
                  key={account.id}
                  type="button"
                  role="radio"
                  aria-checked={accountId === account.id}
                  className={`account-option${accountId === account.id ? ' selected' : ''}`}
                  data-type={account.type}
                  onClick={() => {
                    setAccountId(account.id)
                    setImportResult(null)
                  }}
                >
                  <span>{account.name}</span>
                  <span className="option-meta">
                    {IMPORTABLE_ACCOUNT_TYPES.includes(account.type)
                      ? account.hasImports
                        ? 'Ready · has imports'
                        : 'Ready to import'
                      : 'Coming soon'}
                  </span>
                </button>
              ))}
            </div>
            {selected && !importSupported && (
              <p className="muted" style={{ marginTop: '0.85rem', marginBottom: 0 }}>
                CSV import for {selected.name} is not wired up yet — CommBank and NAB credit card
                work today.
              </p>
            )}
            {selected?.type === 'NAB_CREDIT_CARD' && importSupported && (
              <p className="muted" style={{ marginTop: '0.85rem', marginBottom: 0 }}>
                Use Export → CSV from qantasmoney.com (not the PDF statement). Merchants count toward
                Expenses by category; CommBank card payments are treated as funds between accounts.
              </p>
            )}
          </div>

          <div className="panel">
            <h2>CSV file</h2>
            <p className="muted" style={{ marginTop: 0 }}>
              Multi-month or multi-year exports are fine — upload as many days as you like. Re-uploading
              an overlapping range skips duplicates.
            </p>
            <label
              className={`file-drop${file ? ' has-file' : ''}${dragging ? ' dragging' : ''}`}
              onDragEnter={(e) => {
                e.preventDefault()
                setDragging(true)
              }}
              onDragOver={(e) => {
                e.preventDefault()
                setDragging(true)
              }}
              onDragLeave={(e) => {
                e.preventDefault()
                setDragging(false)
              }}
              onDrop={(e) => {
                e.preventDefault()
                setDragging(false)
                const dropped = e.dataTransfer.files?.[0] ?? null
                setFile(dropped)
                setImportResult(null)
              }}
            >
              <input
                key={file?.name ?? 'no-file'}
                className="file-input"
                type="file"
                accept=".csv,text/csv"
                onChange={(e) => {
                  setFile(e.target.files?.[0] ?? null)
                  setImportResult(null)
                }}
              />
              <span className="file-drop-icon" aria-hidden="true">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
                  <path
                    d="M12 16V4m0 0 4 4m-4-4L8 8"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                  <path
                    d="M4 14v4a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-4"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                  />
                </svg>
              </span>
              {file ? (
                <>
                  <span className="file-drop-title">{file.name}</span>
                  <span className="file-drop-hint">Click to replace · CSV ready</span>
                </>
              ) : (
                <>
                  <span className="file-drop-title">Drop your CSV here</span>
                  <span className="file-drop-hint">
                    or <span className="file-browse">browse files</span> · .csv only
                  </span>
                </>
              )}
            </label>
            {file && (
              <button
                type="button"
                className="file-clear"
                onClick={() => {
                  setFile(null)
                  setImportResult(null)
                }}
              >
                Clear file
              </button>
            )}
          </div>

          <button type="submit" disabled={!canUpload}>
            {uploading ? 'Uploading…' : 'Upload'}
          </button>
        </form>
      )}

      {importResult && selected && (
        <div className="panel result-panel" style={{ marginTop: '1rem' }}>
          <p className="import-result">
            Accepted {importResult.accepted} · Duplicate {importResult.duplicate} · Rejected{' '}
            {importResult.rejected}
          </p>
          <p style={{ margin: 0 }}>
            <Link to={`/accounts/${selected.id}`}>View {selected.name} transactions →</Link>
          </p>
        </div>
      )}

      {!loading && accounts.length > 0 && (
        <div className="account-links">
          <h2>Jump to ledger</h2>
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
        </div>
      )}
    </section>
  )
}
