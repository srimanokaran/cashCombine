export type AccountType = 'COMMBANK'

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

export interface ApiError {
  message: string
}
