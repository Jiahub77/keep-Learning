const BASE = import.meta.env.VITE_API_BASE || ''

export class ApiError extends Error {
  constructor(message, status) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function extractMessage(raw, status) {
  if (!raw) return `请求失败（HTTP ${status}）`
  try {
    const data = JSON.parse(raw)
    if (data && typeof data === 'object') {
      // Spring 默认错误体：{status, error, message, path}
      return data.message || data.error || `请求失败（HTTP ${status}）`
    }
  } catch {
    // 后端业务异常直接返回纯文本，比如「库存不足」
  }
  return raw.length > 200 ? `请求失败（HTTP ${status}）` : raw
}

/**
 * 统一请求封装：后端出错时返回纯文本（业务异常）或 JSON（框架异常），这里都归一化成 ApiError。
 * body 传原始值（比如扣库存要直接发一个数字 1），内部 JSON.stringify。
 */
export async function request(path, { method = 'GET', body } = {}) {
  let res
  try {
    res = await fetch(BASE + path, {
      method,
      headers: body !== undefined ? { 'Content-Type': 'application/json' } : undefined,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError('无法连接后端服务，请确认 Spring Boot 已启动', 0)
  }

  const raw = await res.text()
  if (!res.ok) {
    throw new ApiError(extractMessage(raw, res.status), res.status)
  }
  if (!raw) return null
  try {
    return JSON.parse(raw)
  } catch {
    return raw
  }
}
