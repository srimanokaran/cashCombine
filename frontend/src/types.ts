export type AccountType = 'COMMBANK' | 'ING' | 'NAB_CREDIT_CARD'

export type CategoryAssignmentSource = 'RULE' | 'MANUAL'

export interface Account {
  id: string
  name: string
  type: AccountType
  hasImports: boolean
}

export interface Category {
  id: string
  name: string
}

export interface Rule {
  id: string
  pattern: string
  categoryId: string
}

export interface Transaction {
  id: string
  accountId: string
  date: string
  amount: number
  description: string
  balance: number
  categoryId: string
  categoryAssignmentSource: CategoryAssignmentSource
}

export interface ImportResult {
  id: string
  accepted: number
  duplicate: number
  rejected: number
}

export interface ImportBatch {
  id: string
  accountId: string
  filename: string | null
  importedAt: string
  accepted: number
  duplicate: number
  rejected: number
}

export interface CategorySpend {
  categoryId: string
  categoryName: string
  amount: number
  percent: number
  transactionCount: number
}

export interface ExpenseTransaction {
  id: string
  accountId: string
  accountName: string
  date: string
  amount: number
  description: string
}

export interface ExpenseDashboard {
  totalExpenses: number
  expenseTransactionCount: number
  categories: CategorySpend[]
  totalIncome: number
  incomeTransactionCount: number
  incomeCategories: CategorySpend[]
}

export interface CategoryReanalysisResult {
  examined: number
  updated: number
  skippedManual: number
}

export interface ApiError {
  message: string
}

/** Account types that currently have a CSV parser wired on the backend. */
export const IMPORTABLE_ACCOUNT_TYPES: AccountType[] = ['COMMBANK', 'NAB_CREDIT_CARD']
