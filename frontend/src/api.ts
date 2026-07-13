import type { Account, AccountType, Category, ImportResult, Rule, Transaction } from './types'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(path, init)
  if (!response.ok) {
    let message = response.statusText
    try {
      const body = (await response.json()) as { message?: string }
      if (body.message) {
        message = body.message
      }
    } catch {
      // ignore parse errors
    }
    throw new Error(message)
  }
  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

export const api = {
  listAccounts: () => request<Account[]>('/api/accounts'),
  createAccount: (name: string, type: AccountType) =>
    request<Account>('/api/accounts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, type }),
    }),
  getAccount: (id: string) => request<Account>(`/api/accounts/${id}`),
  deleteAccount: (id: string) =>
    request<void>(`/api/accounts/${id}`, { method: 'DELETE' }),
  importCsv: async (accountId: string, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return request<ImportResult>(`/api/accounts/${accountId}/import`, {
      method: 'POST',
      body: form,
    })
  },
  listTransactions: (accountId: string) =>
    request<Transaction[]>(`/api/accounts/${accountId}/transactions`),
  changeCategory: (transactionId: string, categoryId: string) =>
    request<Transaction>(`/api/transactions/${transactionId}/category`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ categoryId }),
    }),
  listCategories: () => request<Category[]>('/api/categories'),
  createCategory: (name: string) =>
    request<Category>('/api/categories', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name }),
    }),
  listRules: () => request<Rule[]>('/api/rules'),
  createRule: (pattern: string, categoryId: string) =>
    request<Rule>('/api/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pattern, categoryId }),
    }),
}
