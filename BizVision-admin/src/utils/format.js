// 价格：数据库里存的是「分」，界面用「元」
export function centsToYuan(cents) {
  const value = Number(cents ?? 0)
  return (value / 100).toFixed(2)
}

export function yuanToCents(input) {
  const value = Number(input)
  if (!Number.isFinite(value)) return null
  return Math.round(value * 100)
}

export function formatMoney(cents) {
  return `¥${centsToYuan(cents)}`
}

export function formatDateTime(value) {
  if (!value) return '—'
  return String(value).replace('T', ' ').slice(0, 16)
}

export function formatNumber(value) {
  return Number(value ?? 0).toLocaleString('zh-CN')
}
