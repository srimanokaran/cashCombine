export function formatMoney(value: number | string) {
  return Number(value).toLocaleString('en-AU', {
    style: 'currency',
    currency: 'AUD',
  })
}

/** Formats an ISO date (YYYY-MM-DD) as day + English month abbr + 2-digit year, e.g. 19 Jul 26. */
export function formatDate(value: string) {
  const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(value) ? `${value}T00:00:00` : value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const day = date.getDate()
  const month = date.toLocaleString('en-AU', { month: 'short' })
  const year = String(date.getFullYear()).slice(-2)
  return `${day} ${month} ${year}`
}

/** Formats YYYY-MM as English month abbr + 2-digit year, e.g. Jul 26. */
export function formatMonth(value: string) {
  const match = /^(\d{4})-(\d{2})$/.exec(value)
  if (!match) {
    return value
  }
  const date = new Date(Number(match[1]), Number(match[2]) - 1, 1)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const month = date.toLocaleString('en-AU', { month: 'short' })
  const year = String(date.getFullYear()).slice(-2)
  return `${month} ${year}`
}

/** Inclusive calendar-month bounds for a YYYY-MM value. */
export function monthDateRange(month: string): { from: string; to: string } | null {
  const match = /^(\d{4})-(\d{2})$/.exec(month)
  if (!match) {
    return null
  }
  const year = Number(match[1])
  const monthIndex = Number(match[2])
  if (monthIndex < 1 || monthIndex > 12) {
    return null
  }
  const lastDay = new Date(year, monthIndex, 0).getDate()
  const mm = String(monthIndex).padStart(2, '0')
  return {
    from: `${year}-${mm}-01`,
    to: `${year}-${mm}-${String(lastDay).padStart(2, '0')}`,
  }
}
