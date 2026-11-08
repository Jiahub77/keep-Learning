import { request } from './http'

// 后端 ProductResponse 的 JSON 字段是 price_in_cents（下划线），这里统一成前端用的驼峰
function normalize(row) {
  return {
    id: row.id,
    name: row.name,
    priceInCents: row.priceInCents ?? row.price_in_cents ?? 0,
    stock: row.stock ?? 0,
  }
}

export async function listProducts(page = 1, size = 10) {
  const data = await request(`/products?page=${page}&size=${size}`)
  return {
    list: (data?.list ?? []).map(normalize),
    total: data?.total ?? 0,
    page: data?.page ?? page,
    size: data?.size ?? size,
  }
}

export async function getProduct(id) {
  return normalize(await request(`/products/${id}`))
}

export function createProduct(payload) {
  return request('/products', { method: 'POST', body: payload })
}

export function updateProduct(id, payload) {
  return request(`/products/${id}`, { method: 'PUT', body: payload })
}

export function deleteProduct(id) {
  return request(`/products/${id}`, { method: 'DELETE' })
}

// 注意：扣库存的请求体就是一个裸数字，不是 {"quantity": 1}
export function decreaseStock(id, quantity) {
  return request(`/products/decrease-stock/${id}`, { method: 'POST', body: quantity })
}
