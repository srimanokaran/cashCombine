import type {
  Account,
  AccountType,
  Category,
  CategoryReanalysisResult,
  CategoryTrendsResponse,
  DateRange,
  ExpenseDashboard,
  ExpenseSearchResult,
  ExpenseTransaction,
  ImportBatch,
  ImportResult,
  MonthlyCashflow,
  QueryExpansion,
  Rule,
  Transaction,
} from './types'

function withDateRange(path: string, range?: DateRange) {
  if (!range?.from && !range?.to) {
    return path
  }
  const params = new URLSearchParams()
  if (range.from) {
    params.set('from', range.from)
  }
  if (range.to) {
    params.set('to', range.to)
  }
  return `${path}?${params.toString()}`
}

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
  ensureFixedAccounts: (() => {
    let inFlight: Promise<Account[]> | null = null
    return () => {
      if (!inFlight) {
        inFlight = request<Account[]>('/api/accounts/ensure-fixed', { method: 'POST' }).finally(() => {
          inFlight = null
        })
      }
      return inFlight
    }
  })(),
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
  listImports: (accountId: string) =>
    request<ImportBatch[]>(`/api/accounts/${accountId}/imports`),
  deleteImport: (accountId: string, importId: string) =>
    request<void>(`/api/accounts/${accountId}/imports/${importId}`, { method: 'DELETE' }),
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
  deleteCategory: (id: string) =>
    request<void>(`/api/categories/${id}`, { method: 'DELETE' }),
  listRules: () => request<Rule[]>('/api/rules'),
  createRule: (pattern: string, categoryId: string) =>
    request<Rule>('/api/rules', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ pattern, categoryId }),
    }),
  deleteRule: (id: string) =>
    request<void>(`/api/rules/${id}`, { method: 'DELETE' }),
  getExpenseDashboard: (range?: DateRange) =>
    request<ExpenseDashboard>(withDateRange('/api/dashboard/expenses', range)),
  getMonthlyCashflow: () => request<MonthlyCashflow[]>('/api/dashboard/monthly'),
  getCategoryTrends: (range?: DateRange) =>
    request<CategoryTrendsResponse>(withDateRange('/api/dashboard/category-trends', range)),
  listExpenseTransactions: (categoryId: string, range?: DateRange) =>
    request<ExpenseTransaction[]>(
      withDateRange(`/api/dashboard/expenses/categories/${categoryId}/transactions`, range),
    ),
  listIncomeTransactions: (categoryId: string, range?: DateRange) =>
    request<ExpenseTransaction[]>(
      withDateRange(`/api/dashboard/income/categories/${categoryId}/transactions`, range),
    ),
  reanalyseExpenses: () =>
    request<CategoryReanalysisResult>('/api/dashboard/expenses/reanalyse', { method: 'POST' }),
  searchExpenses: (params: { q: string; expand?: string[]; from?: string; to?: string }, signal?: AbortSignal) => {
    const query = new URLSearchParams({ q: params.q })
    if (params.expand && params.expand.length > 0) {
      query.set('expand', params.expand.join(','))
    }
    if (params.from) {
      query.set('from', params.from)
    }
    if (params.to) {
      query.set('to', params.to)
    }
    return request<ExpenseSearchResult>(`/api/search/expenses?${query.toString()}`, { signal })
  },
  startAiSearch: (signal?: AbortSignal) =>
    request<{ available: boolean }>('/api/search/ai/start', { method: 'POST', signal }),
  searchExpensesAi: (query: string, signal?: AbortSignal) =>
    request<ExpenseSearchResult>('/api/search/expenses/ai', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query }),
      signal,
    }),
  expandSearch: (q: string) =>
    request<QueryExpansion>(`/api/search/expenses/expand?${new URLSearchParams({ q }).toString()}`),
}
