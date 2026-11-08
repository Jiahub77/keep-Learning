/**
 * 本地 Mock 服务：在没有 MySQL / Spring Boot 的情况下预览后台界面。
 * 接口契约与修复后的后端保持一致（包括 price_in_cents、createdAt、isOn 这些字段名）。
 *
 *   node mock/server.mjs          # 监听 8081
 *   VITE_PROXY_TARGET=http://localhost:8081 npm run dev
 */
import { createServer } from 'node:http'

const PORT = Number(process.env.PORT || 8081)

let productSeq = 0
const products = [
  ['不锈钢保温杯 500ml', 12900, 42],
  ['手冲咖啡壶套装', 25900, 8],
  ['陶瓷马克杯 350ml', 4900, 120],
  ['便携折叠水杯', 3900, 0],
  ['冷萃咖啡壶 600ml', 18900, 15],
  ['双层玻璃杯 300ml', 6900, 6],
  ['旅行电热水杯', 29900, 3],
].map(([name, price, stock]) => ({ id: ++productSeq, name, priceInCents: price, stock }))

let articleSeq = 0
const articles = [
  ['商品库存扣减为什么要用 SQL 条件更新', '先查后改在并发下会超卖，正确做法是把判断写进 UPDATE 的 WHERE 里：UPDATE products SET stock = stock - ? WHERE id = ? AND stock >= ?。', 'jia', 1],
  ['软删除的三种实现与坑', 'is_on 字段要记得在列表、详情、统计里都过滤，否则删掉的数据会「诈尸」。', 'jia', 1],
  ['MyBatis 注解开发速查', 'select / insert / update / delete 四个注解，加上 @Options 拿自增主键。', 'jia', 1],
  ['为什么 DTO 不能全是 final 字段', 'Jackson 反序列化需要无参构造 + setter，或者显式标注 @JsonCreator。', 'jia', 1],
  ['RESTful 路径设计的一点建议', '/article/add 和 /article/{id} 混用容易让人猜不到接口，统一成 /articles 会更顺。', 'jia', 1],
].map(([name, content, author, isOn]) => ({
  id: ++articleSeq,
  name,
  content,
  author,
  isOn,
  createdAt: new Date(Date.now() - (10 - articleSeq) * 36e5).toISOString().slice(0, 19),
}))

const json = (res, status, data) => {
  res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
  res.end(data === undefined ? '' : JSON.stringify(data))
}
const text = (res, status, message) => {
  res.writeHead(status, { 'Content-Type': 'text/plain; charset=utf-8' })
  res.end(message)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    const chunks = []
    req.on('data', (c) => chunks.push(c))
    req.on('end', () => {
      const raw = Buffer.concat(chunks).toString('utf8')
      if (!raw) return resolve(undefined)
      try {
        resolve(JSON.parse(raw))
      } catch {
        reject(new Error('BAD_JSON'))
      }
    })
    req.on('error', reject)
  })
}

function paginate(list, url) {
  const page = Number(url.searchParams.get('page') ?? 1)
  const size = Number(url.searchParams.get('size') ?? 10)
  if (!Number.isInteger(page) || page < 1) throw new Error('页码必须大于等于1')
  if (!Number.isInteger(size) || size < 1 || size > 100) throw new Error('每页数量必须在1到100之间')
  const offset = (page - 1) * size
  return { page, size, offset, list: list.slice(offset, offset + size) }
}

function validateProduct(body) {
  if (!body || typeof body !== 'object') throw new Error('请求体格式不正确')
  const name = typeof body.name === 'string' ? body.name.trim() : ''
  if (!name) throw new Error('商品名称不能为空')
  if (name.length > 100) throw new Error('商品名称不能超过100个字符')
  if (!Number.isInteger(body.priceInCents) || body.priceInCents < 0) throw new Error('价格必须填写且不能为负数')
  if (!Number.isInteger(body.stock) || body.stock < 0) throw new Error('库存必须填写且不能为负数')
  return { name, priceInCents: body.priceInCents, stock: body.stock }
}

function validateArticle(body) {
  if (!body || typeof body !== 'object') throw new Error('请求体格式不正确')
  const name = typeof body.name === 'string' ? body.name.trim() : ''
  const content = typeof body.content === 'string' ? body.content : ''
  const author = typeof body.author === 'string' ? body.author.trim() : ''
  if (!name) throw new Error('文章名称不能为空')
  if (name.length > 100) throw new Error('文章名称不能超过100个字符')
  if (!content.trim()) throw new Error('文章内容不能为空')
  if (!author) throw new Error('作者不能为空')
  if (author.length > 50) throw new Error('作者不能超过50个字符')
  return { name, content, author }
}

const server = createServer(async (req, res) => {
  const url = new URL(req.url, `http://${req.headers.host}`)
  const path = url.pathname
  const method = req.method

  try {
    /* ---------------- 商品 ---------------- */
    if (path === '/products' && method === 'GET') {
      const { page, size, list } = paginate([...products].sort((a, b) => b.id - a.id), url)
      return json(res, 200, { list, total: products.length, page, size })
    }

    if (path === '/products' && method === 'POST') {
      const data = validateProduct(await readBody(req))
      const product = { id: ++productSeq, ...data }
      products.push(product)
      return json(res, 201, product.id)
    }

    const stockMatch = path.match(/^\/products\/decrease-stock\/(\d+)$/)
    if (stockMatch && method === 'POST') {
      const body = await readBody(req)
      const quantity = typeof body === 'number' ? body : NaN
      if (!Number.isInteger(quantity) || quantity <= 0) return text(res, 400, '扣减库存必须大于 0')
      const product = products.find((p) => p.id === Number(stockMatch[1]))
      if (!product) return text(res, 404, '')
      if (product.stock < quantity) return text(res, 409, '库存不足')
      product.stock -= quantity
      return json(res, 200, product.id)
    }

    const productMatch = path.match(/^\/products\/(\d+)$/)
    if (productMatch) {
      const id = Number(productMatch[1])
      const index = products.findIndex((p) => p.id === id)
      if (method === 'GET') {
        if (index < 0) return text(res, 404, '')
        return json(res, 200, products[index])
      }
      if (method === 'PUT') {
        if (index < 0) return text(res, 404, '')
        const data = validateProduct(await readBody(req))
        products[index] = { id, ...data }
        return json(res, 200, id)
      }
      if (method === 'DELETE') {
        if (index < 0) return text(res, 404, '')
        products.splice(index, 1)
        return json(res, 200, id)
      }
    }

    /* ---------------- 文章 ---------------- */
    if (path === '/article/list' && method === 'GET') {
      const active = articles.filter((a) => a.isOn === 1).sort((a, b) => b.id - a.id)
      const { page, size, offset, list } = paginate(active, url)
      return json(res, 200, { list, page, size, offset, total: active.length })
    }

    if (path === '/article/add' && method === 'POST') {
      const data = validateArticle(await readBody(req))
      const article = {
        id: ++articleSeq,
        ...data,
        isOn: 1,
        createdAt: new Date().toISOString().slice(0, 19),
      }
      articles.push(article)
      return json(res, 201, article.id)
    }

    const articleMatch = path.match(/^\/article\/(\d+)$/)
    if (articleMatch) {
      const id = Number(articleMatch[1])
      const article = articles.find((a) => a.id === id && a.isOn === 1)
      if (method === 'GET') {
        if (!article) return text(res, 404, '')
        return json(res, 200, article)
      }
      if (method === 'PUT') {
        if (!article) return text(res, 404, '')
        const data = validateArticle(await readBody(req))
        Object.assign(article, data)
        return json(res, 200, id)
      }
      if (method === 'DELETE') {
        if (!article) return text(res, 404, '')
        article.isOn = 0
        return json(res, 200, id)
      }
    }

    return text(res, 404, `No handler for ${method} ${path}`)
  } catch (error) {
    if (error.message === 'BAD_JSON') return text(res, 400, '请求体格式不正确')
    return text(res, 400, error.message)
  }
})

server.listen(PORT, () => {
  console.log(`Mock API listening on http://localhost:${PORT}`)
})
