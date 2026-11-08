<script setup>
import { computed } from 'vue'

const props = defineProps({
  page: { type: Number, required: true },
  size: { type: Number, required: true },
  total: { type: Number, default: 0 },
  sizeOptions: { type: Array, default: () => [10, 20, 50, 100] },
})

const emit = defineEmits(['update:page', 'update:size'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))
const from = computed(() => (props.total === 0 ? 0 : (props.page - 1) * props.size + 1))
const to = computed(() => Math.min(props.page * props.size, props.total))

function go(page) {
  const target = Math.min(Math.max(1, page), totalPages.value)
  if (target !== props.page) emit('update:page', target)
}
</script>

<template>
  <div class="flex flex-col gap-3 border-t border-slate-100 px-4 py-3 sm:flex-row sm:items-center sm:justify-between">
    <div class="flex items-center gap-3 text-xs text-slate-500">
      <span>共 <span class="font-semibold text-slate-700">{{ total }}</span> 条</span>
      <span class="text-slate-300">|</span>
      <span>当前 {{ from }}–{{ to }}</span>
    </div>

    <div class="flex items-center gap-3">
      <label class="flex items-center gap-2 text-xs text-slate-500">
        每页
        <select
          class="rounded-md border border-slate-300 bg-white py-1 pl-2 pr-6 text-xs text-slate-700 focus:border-indigo-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          :value="size"
          @change="emit('update:size', Number($event.target.value))"
        >
          <option v-for="option in sizeOptions" :key="option" :value="option">{{ option }}</option>
        </select>
      </label>

      <div class="flex items-center gap-1">
        <button class="btn-secondary h-8 !px-2" :disabled="page <= 1" aria-label="上一页" @click="go(page - 1)">
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M12 5l-5 5 5 5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
        <span class="px-1 text-xs text-slate-600">
          <span class="font-semibold text-slate-800">{{ page }}</span> / {{ totalPages }}
        </span>
        <button class="btn-secondary h-8 !px-2" :disabled="page >= totalPages" aria-label="下一页" @click="go(page + 1)">
          <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
            <path d="M8 5l5 5-5 5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>
