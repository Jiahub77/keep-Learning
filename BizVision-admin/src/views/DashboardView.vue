<script setup>
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listProducts } from '../api/products'
import { listArticles } from '../api/articles'
import { formatDateTime, formatMoney } from '../utils/format'

const loading = ref(true)
const error = ref('')
const productTotal = ref(0)
const articleTotal = ref(0)
const recentProducts = ref([])
const recentArticles = ref([])

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [products, articles] = await Promise.all([listProducts(1, 5), listArticles(1, 5)])
    productTotal.value = products.total
    recentProducts.value = products.list
    articleTotal.value = articles.total
    recentArticles.value = articles.list
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="space-y-5">
    <!-- 出错提示 -->
    <div
      v-if="error"
      class="flex items-start gap-3 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800"
    >
      <svg class="mt-0.5 h-4 w-4 flex-none" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.7">
        <path d="M10 6.5v4M10 14h.01" stroke-linecap="round" />
        <circle cx="10" cy="10" r="7.2" />
      </svg>
      <div class="flex-1">
        <p class="font-medium">后端接口没有正常返回</p>
        <p class="mt-0.5 text-xs leading-relaxed text-amber-700">{{ error }}</p>
      </div>
      <button class="btn-secondary h-8 !py-0 !text-xs" @click="load">重试</button>
    </div>

    <!-- 统计卡 -->
    <div class="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <RouterLink to="/products" class="card group p-5 transition-shadow hover:shadow-md">
        <div class="flex items-start justify-between">
          <p class="text-sm text-slate-500">商品总数</p>
          <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-indigo-50 text-indigo-600">
            <svg class="h-[18px] w-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M3.5 6.5 10 3l6.5 3.5v7L10 17l-6.5-3.5v-7ZM10 3v14M3.5 6.5 10 10l6.5-3.5" stroke-linejoin="round" />
            </svg>
          </span>
        </div>
        <p class="mt-3 text-3xl font-semibold tabular-nums text-slate-900">
          <span v-if="loading" class="inline-block h-8 w-16 animate-pulse rounded bg-slate-100 align-middle"></span>
          <span v-else>{{ productTotal }}</span>
        </p>
        <p class="mt-1 text-xs text-slate-400 group-hover:text-indigo-600">进入商品管理 →</p>
      </RouterLink>

      <RouterLink to="/articles" class="card group p-5 transition-shadow hover:shadow-md">
        <div class="flex items-start justify-between">
          <p class="text-sm text-slate-500">文章总数</p>
          <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-violet-50 text-violet-600">
            <svg class="h-[18px] w-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M5 3.5h7l3 3v10H5v-13ZM8 8.5h4M8 11.5h4M8 14.5h2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
        </div>
        <p class="mt-3 text-3xl font-semibold tabular-nums text-slate-900">
          <span v-if="loading" class="inline-block h-8 w-16 animate-pulse rounded bg-slate-100 align-middle"></span>
          <span v-else>{{ articleTotal }}</span>
        </p>
        <p class="mt-1 text-xs text-slate-400 group-hover:text-violet-600">进入文章管理 →</p>
      </RouterLink>

      <div class="card p-5">
        <div class="flex items-start justify-between">
          <p class="text-sm text-slate-500">库存预警</p>
          <span class="flex h-9 w-9 items-center justify-center rounded-lg bg-amber-50 text-amber-600">
            <svg class="h-[18px] w-[18px]" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
              <path d="M10 6.5v4M10 14h.01" stroke-linecap="round" />
              <circle cx="10" cy="10" r="7.2" />
            </svg>
          </span>
        </div>
        <p class="mt-3 text-3xl font-semibold tabular-nums text-slate-900">
          {{ loading ? '—' : recentProducts.filter((p) => p.stock <= 10).length }}
        </p>
        <p class="mt-1 text-xs text-slate-400">最近 5 件商品中库存 ≤ 10 的数量</p>
      </div>

      <div class="card flex flex-col justify-between p-5">
        <div>
          <p class="text-sm text-slate-500">快捷操作</p>
          <p class="mt-1 text-xs text-slate-400">常用入口</p>
        </div>
        <div class="mt-3 flex flex-wrap gap-2">
          <RouterLink to="/products" class="btn-secondary h-8 !px-2.5 !text-xs">新增商品</RouterLink>
          <RouterLink to="/articles" class="btn-secondary h-8 !px-2.5 !text-xs">写文章</RouterLink>
        </div>
      </div>
    </div>

    <!-- 最近数据 -->
    <div class="grid gap-4 lg:grid-cols-5">
      <div class="card overflow-hidden lg:col-span-3">
        <div class="flex items-center justify-between border-b border-slate-100 px-5 py-3.5">
          <h2 class="text-sm font-semibold text-slate-800">最新商品</h2>
          <RouterLink to="/products" class="text-xs text-indigo-600 hover:underline">全部 →</RouterLink>
        </div>
        <div class="divide-y divide-slate-100">
          <template v-if="loading">
            <div v-for="i in 3" :key="i" class="flex items-center gap-3 px-5 py-3.5">
              <div class="h-4 flex-1 animate-pulse rounded bg-slate-100"></div>
              <div class="h-4 w-16 animate-pulse rounded bg-slate-100"></div>
            </div>
          </template>
          <div v-else-if="recentProducts.length === 0" class="px-5 py-10 text-center text-sm text-slate-400">
            还没有商品
          </div>
          <template v-else>
            <div
              v-for="product in recentProducts"
              :key="product.id"
              class="flex items-center gap-3 px-5 py-3.5"
            >
              <div class="min-w-0 flex-1">
                <p class="truncate text-sm font-medium text-slate-800">{{ product.name }}</p>
                <p class="font-mono text-[11px] text-slate-400">#{{ product.id }}</p>
              </div>
              <p class="text-sm font-medium tabular-nums text-slate-700">{{ formatMoney(product.priceInCents) }}</p>
              <span
                class="badge w-14 justify-center"
                :class="
                  product.stock <= 0
                    ? 'bg-rose-50 text-rose-700'
                    : product.stock <= 10
                      ? 'bg-amber-50 text-amber-700'
                      : 'bg-slate-100 text-slate-600'
                "
              >
                库存 {{ product.stock }}
              </span>
            </div>
          </template>
        </div>
      </div>

      <div class="card overflow-hidden lg:col-span-2">
        <div class="flex items-center justify-between border-b border-slate-100 px-5 py-3.5">
          <h2 class="text-sm font-semibold text-slate-800">最新文章</h2>
          <RouterLink to="/articles" class="text-xs text-indigo-600 hover:underline">全部 →</RouterLink>
        </div>
        <div class="divide-y divide-slate-100">
          <template v-if="loading">
            <div v-for="i in 3" :key="i" class="px-5 py-3.5">
              <div class="h-4 w-3/4 animate-pulse rounded bg-slate-100"></div>
            </div>
          </template>
          <div v-else-if="recentArticles.length === 0" class="px-5 py-10 text-center text-sm text-slate-400">
            还没有文章
          </div>
          <template v-else>
            <div v-for="article in recentArticles" :key="article.id" class="px-5 py-3.5">
              <p class="truncate text-sm font-medium text-slate-800">{{ article.name }}</p>
              <p class="mt-0.5 text-xs text-slate-400">
                {{ article.author || '—' }} · {{ formatDateTime(article.createdAt) }}
              </p>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
