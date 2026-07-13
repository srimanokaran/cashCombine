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

export interface ExpenseDashboard {
  totalExpenses: number
  expenseTransactionCount: number
  categories: CategorySpend[]
}

export interface ApiError {
  message: string
}

/** Account types that currently have a CSV parser wired on the backend. */
export const IMPORTABLE_ACCOUNT_TYPES: AccountType[] = ['COMMBANK']
