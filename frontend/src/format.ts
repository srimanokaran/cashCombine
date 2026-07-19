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
