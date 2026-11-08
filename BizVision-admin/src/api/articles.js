import { request } from './http'

// 兼容修复前后的字段名：createdAt / creatAt、isOn / is_on
function normalize(row) {
  return {
    id: row.id,
    name: row.name,
    content: row.content ?? '',
    author: row.author ?? '',
    createdAt: row.createdAt ?? row.creatAt ?? null,
    isOn: row.isOn ?? row.is_on ?? 1,
  }
}

export async function listArticles(page = 1, size = 10) {
  const data = await request(`/article/list?page=${page}&size=${size}`)
  return {
    list: (data?.list ?? []).map(normalize),
    total: data?.total ?? 0,
    page: data?.page ?? page,
    size: data?.size ?? size,
  }
}

export async function getArticle(id) {
  return normalize(await request(`/article/${id}`))
}

export function createArticle(payload) {
  return request('/article/add', { method: 'POST', body: payload })
}

export function updateArticle(id, payload) {
  return request(`/article/${id}`, { method: 'PUT', body: payload })
}

export function deleteArticle(id) {
  return request(`/article/${id}`, { method: 'DELETE' })
}
