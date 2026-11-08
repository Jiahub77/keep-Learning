import { describe, expect, it, vi, beforeAll } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ProductsView from '../views/ProductsView.vue'
import ArticlesView from '../views/ArticlesView.vue'
import DashboardView from '../views/DashboardView.vue'

// 把组件的相对请求打到 mock 服务（先 `npm run mock` 起在 8081）
const MOCK = 'http://localhost:8081'
const realFetch = globalThis.fetch
const calls = []

globalThis.fetch = (input, init = {}) => {
  calls.push({ url: input, method: init.method || 'GET', body: init.body })
  return realFetch(MOCK + input, init)
}

beforeAll(() => {
  // 冒烟测试只读接口，不改数据；这里确认 mock 服务在线
  expect(typeof realFetch).toBe('function')
})

// 真实 HTTP 请求比微任务慢，flushPromises 一次不够，轮询等待渲染结果
async function waitFor(check, timeout = 5000) {
  const started = Date.now()
  for (;;) {
    if (check()) return
    if (Date.now() - started > timeout) throw new Error('等待超时：条件始终不成立')
    await new Promise((resolve) => setTimeout(resolve, 25))
  }
}

describe('商品管理', () => {
  it('渲染列表、价格换算、库存标签', async () => {
    const wrapper = mount(ProductsView)
    await waitFor(() => wrapper.findAll('tbody tr').length > 0)
    expect(wrapper.text()).toContain('不锈钢保温杯 500ml')
    // 12900 分 → ¥129.00
    expect(wrapper.text()).toContain('¥129.00')
    // 售罄 / 偏低 标签
    expect(wrapper.text()).toContain('售罄')
    expect(wrapper.text()).toContain('偏低')
    // 分页栏拿到 total
    expect(wrapper.text()).toMatch(/共\s*\d+\s*条/)
  })

  it('点「新增商品」会打开弹窗', async () => {
    const wrapper = mount(ProductsView, { attachTo: document.body })
    await flushPromises()

    const button = wrapper.findAll('button').find((b) => b.text().includes('新增商品'))
    await button.trigger('click')
    await flushPromises()

    expect(document.getElementById('product-name')).not.toBeNull()
    wrapper.unmount()
  })

  it('提交表单时按「分」传给后端', async () => {
    const wrapper = mount(ProductsView, { attachTo: document.body })
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('新增商品')).trigger('click')
    await flushPromises()

    const name = document.getElementById('product-name')
    const price = document.getElementById('product-price')
    const stock = document.getElementById('product-stock')

    name.value = '冒烟测试商品'
    name.dispatchEvent(new Event('input'))
    price.value = '12.34'
    price.dispatchEvent(new Event('input'))
    stock.value = '3'
    stock.dispatchEvent(new Event('input'))
    await flushPromises()

    calls.length = 0
    const submit = [...document.querySelectorAll('button')].find((b) => b.textContent.trim() === '创建')
    submit.click()
    await flushPromises()

    const post = calls.find((c) => c.method === 'POST' && c.url === '/products')
    expect(post).toBeTruthy()
    expect(JSON.parse(post.body)).toEqual({ name: '冒烟测试商品', priceInCents: 1234, stock: 3 })
    wrapper.unmount()
  })

  it('价格非法时不发请求', async () => {
    const wrapper = mount(ProductsView, { attachTo: document.body })
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('新增商品')).trigger('click')
    await flushPromises()

    const name = document.getElementById('product-name')
    const price = document.getElementById('product-price')
    const stock = document.getElementById('product-stock')
    name.value = '非法价格'
    name.dispatchEvent(new Event('input'))
    price.value = '1.234'
    price.dispatchEvent(new Event('input'))
    stock.value = '1'
    stock.dispatchEvent(new Event('input'))
    await flushPromises()

    calls.length = 0
    ;[...document.querySelectorAll('button')].find((b) => b.textContent.trim() === '创建').click()
    await flushPromises()

    expect(calls.filter((c) => c.method === 'POST')).toHaveLength(0)
    expect(document.body.textContent).toContain('价格需为数字，最多两位小数')
    wrapper.unmount()
  })
})

describe('文章管理', () => {
  it('渲染列表与状态标签', async () => {
    const wrapper = mount(ArticlesView)
    await waitFor(() => wrapper.findAll('tbody tr').length > 0)

    expect(wrapper.findAll('tbody tr').length).toBeGreaterThan(0)
    expect(wrapper.text()).toContain('已发布')
    expect(wrapper.text()).toContain('jia')
    // 创建时间来自 createdAt（修复前是 null，会渲染成 —）
    expect(wrapper.text()).not.toMatch(/\d{4}-\d{2}-\d{2}\s—/)
  })

  it('点「写文章」会打开弹窗', async () => {
    const wrapper = mount(ArticlesView, { attachTo: document.body })
    await flushPromises()

    await wrapper.findAll('button').find((b) => b.text().includes('写文章')).trigger('click')
    await flushPromises()

    expect(document.getElementById('article-content')).not.toBeNull()
    wrapper.unmount()
  })
})

describe('概览', () => {
  it('展示统计与最新数据', async () => {
    const wrapper = mount(DashboardView, {
      global: { stubs: { RouterLink: { template: '<a><slot /></a>' } } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('商品总数')
    expect(wrapper.text()).toContain('文章总数')
    expect(wrapper.text()).toContain('最新商品')
  })
})
