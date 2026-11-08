<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute } from 'vue-router'
import ToastHost from './components/ToastHost.vue'

const route = useRoute()
const sidebarOpen = ref(false)

const nav = [
  {
    name: 'dashboard',
    to: '/dashboard',
    label: '概览',
    icon: 'M3.5 10.5 10 4l6.5 6.5M5 9.5V16h10V9.5',
  },
  {
    name: 'products',
    to: '/products',
    label: '商品管理',
    icon: 'M3.5 6.5 10 3l6.5 3.5v7L10 17l-6.5-3.5v-7ZM10 3v14M3.5 6.5 10 10l6.5-3.5',
  },
  {
    name: 'articles',
    to: '/articles',
    label: '文章管理',
    icon: 'M5 3.5h7l3 3v10H5v-13ZM8 8.5h4M8 11.5h4M8 14.5h2',
  },
]

const title = computed(() => route.meta.title || 'BizVision')
const subtitle = computed(() => route.meta.subtitle || '')
</script>

<template>
  <div class="min-h-screen bg-slate-50">
    <!-- 移动端遮罩 -->
    <div
      v-if="sidebarOpen"
      class="fixed inset-0 z-30 bg-slate-900/30 lg:hidden"
      @click="sidebarOpen = false"
    ></div>

    <!-- 侧边栏 -->
    <aside
      class="fixed inset-y-0 left-0 z-30 flex w-64 flex-col border-r border-slate-200 bg-white transition-transform duration-200 lg:translate-x-0"
      :class="sidebarOpen ? 'translate-x-0' : '-translate-x-full'"
    >
      <div class="flex h-16 items-center gap-3 border-b border-slate-100 px-5">
        <div class="flex h-9 w-9 items-center justify-center rounded-xl bg-gradient-to-br from-indigo-500 to-violet-500 text-sm font-bold text-white shadow-sm">
          B
        </div>
        <div class="leading-tight">
          <p class="text-sm font-semibold text-slate-900">BizVision</p>
          <p class="text-[11px] text-slate-400">后台管理系统</p>
        </div>
      </div>

      <nav class="flex-1 space-y-1 p-3">
        <p class="px-3 pb-1 pt-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">内容运营</p>
        <RouterLink
          v-for="item in nav"
          :key="item.name"
          :to="item.to"
          class="group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-colors"
          :class="
            route.name === item.name
              ? 'bg-indigo-50 text-indigo-700'
              : 'text-slate-600 hover:bg-slate-50 hover:text-slate-900'
          "
          @click="sidebarOpen = false"
        >
          <svg
            class="h-[18px] w-[18px] flex-none"
            :class="route.name === item.name ? 'text-indigo-600' : 'text-slate-400 group-hover:text-slate-500'"
            viewBox="0 0 20 20"
            fill="none"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <path :d="item.icon" />
          </svg>
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="border-t border-slate-100 p-3">
        <div class="rounded-lg bg-slate-50 px-3 py-2.5">
          <p class="text-[11px] font-medium text-slate-500">接口地址</p>
          <p class="mt-0.5 font-mono text-[11px] text-slate-400">/products · /article</p>
        </div>
        <p class="mt-2 px-1 text-[11px] text-slate-400">Vue 3 · Tailwind CSS</p>
      </div>
    </aside>

    <!-- 主区域 -->
    <div class="lg:pl-64">
      <header class="sticky top-0 z-20 border-b border-slate-200 bg-white/85 backdrop-blur">
        <div class="flex h-16 items-center gap-3 px-4 sm:px-6 lg:px-8">
          <button class="btn-ghost -ml-2 h-9 w-9 !p-0 lg:hidden" aria-label="打开菜单" @click="sidebarOpen = true">
            <svg class="h-5 w-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M3.5 5.5h13M3.5 10h13M3.5 14.5h13" stroke-linecap="round" />
            </svg>
          </button>

          <div class="min-w-0 flex-1">
            <h1 class="truncate text-[15px] font-semibold text-slate-900">{{ title }}</h1>
            <p class="truncate text-xs text-slate-500">{{ subtitle }}</p>
          </div>

          <div class="hidden items-center gap-2 sm:flex">
            <span class="badge bg-emerald-50 text-emerald-700">
              <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
              本地开发环境
            </span>
          </div>
        </div>
      </header>

      <main class="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <RouterView v-slot="{ Component }">
          <Transition
            enter-active-class="transition duration-200 ease-out"
            enter-from-class="translate-y-1 opacity-0"
            enter-to-class="translate-y-0 opacity-100"
            mode="out-in"
          >
            <component :is="Component" />
          </Transition>
        </RouterView>
      </main>
    </div>

    <ToastHost />
  </div>
</template>
