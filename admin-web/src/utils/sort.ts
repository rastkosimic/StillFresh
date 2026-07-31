type SortValue = string | number | Date | null | undefined

function toTimestamp(value: SortValue): number {
  if (value == null) return 0
  if (typeof value === 'number') return value
  if (value instanceof Date) return value.getTime()
  const parsed = Date.parse(value)
  return Number.isNaN(parsed) ? 0 : parsed
}

/** Sort items newest-first using date fields, then id descending as fallback. */
export function sortByLatest<T extends { id?: number }>(
  items: T[],
  ...dateFields: Array<(item: T) => SortValue>
): T[] {
  return [...items].sort((a, b) => {
    for (const getDate of dateFields) {
      const diff = toTimestamp(getDate(b)) - toTimestamp(getDate(a))
      if (diff !== 0) return diff
    }
    return (b.id ?? 0) - (a.id ?? 0)
  })
}

export function sortOrdersByLatest<T extends { id: number; createdAt?: string; pickupBy?: string }>(
  items: T[]
): T[] {
  return sortByLatest(items, (o) => o.createdAt, (o) => o.pickupBy)
}
