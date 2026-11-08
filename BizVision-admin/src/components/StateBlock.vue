<script setup>
defineProps({
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '暂无数据' },
  emptyHint: { type: String, default: '' },
  rows: { type: Number, default: 5 },
})

defineEmits(['retry'])
</script>

<template>
  <div>
    <!-- 加载骨架 -->
    <div v-if="loading" class="divide-y divide-slate-100">
      <div v-for="row in rows" :key="row" class="flex items-center gap-4 px-4 py-4">
        <div class="h-4 w-10 animate-pulse rounded bg-slate-100"></div>
        <div class="h-4 flex-1 animate-pulse rounded bg-slate-100"></div>
        <div class="h-4 w-16 animate-pulse rounded bg-slate-100"></div>
        <div class="h-4 w-12 animate-pulse rounded bg-slate-100"></div>
      </div>
    </div>

    <!-- 错误 -->
    <div v-else-if="error" class="flex flex-col items-center gap-3 px-6 py-14 text-center">
      <div class="flex h-11 w-11 items-center justify-center rounded-full bg-rose-50 text-rose-600">
        <svg class="h-5 w-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
          <circle cx="10" cy="10" r="7.2" />
          <path d="M10 6.4v4.2M10 13.6h.01" stroke-linecap="round" />
        </svg>
      </div>
      <p class="text-sm font-medium text-slate-800">数据加载失败</p>
      <p class="max-w-md text-xs leading-relaxed text-slate-500">{{ error }}</p>
      <button class="btn-secondary mt-1" @click="$emit('retry')">重新加载</button>
    </div>

    <!-- 空 -->
    <div v-else-if="empty" class="flex flex-col items-center gap-2 px-6 py-16 text-center">
      <div class="flex h-11 w-11 items-center justify-center rounded-full bg-slate-100 text-slate-400">
        <svg class="h-5 w-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M3.5 6.5h13M3.5 10h13M3.5 13.5h8" stroke-linecap="round" />
        </svg>
      </div>
      <p class="text-sm font-medium text-slate-700">{{ emptyText }}</p>
      <p v-if="emptyHint" class="text-xs text-slate-500">{{ emptyHint }}</p>
      <slot name="empty-action" />
    </div>

    <slot v-else />
  </div>
</template>
