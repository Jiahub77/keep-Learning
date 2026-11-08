<script setup>
import { useToast } from '../composables/useToast'

const { toasts, dismiss } = useToast()

const styles = {
  success: { wrap: 'border-emerald-200 bg-emerald-50 text-emerald-800', dot: 'bg-emerald-500' },
  error: { wrap: 'border-rose-200 bg-rose-50 text-rose-800', dot: 'bg-rose-500' },
  info: { wrap: 'border-slate-200 bg-white text-slate-700', dot: 'bg-slate-400' },
}
</script>

<template>
  <Teleport to="body">
    <div class="pointer-events-none fixed inset-x-0 bottom-0 z-50 flex flex-col items-center gap-2 p-6 sm:items-end">
      <TransitionGroup
        enter-active-class="transition duration-200 ease-out"
        enter-from-class="translate-y-2 opacity-0"
        enter-to-class="translate-y-0 opacity-100"
        leave-active-class="transition duration-150 ease-in"
        leave-from-class="opacity-100"
        leave-to-class="opacity-0"
        move-class="transition duration-200"
      >
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border px-4 py-3 shadow-lg"
          :class="styles[toast.type]?.wrap || styles.info.wrap"
        >
          <span class="mt-1.5 h-1.5 w-1.5 flex-none rounded-full" :class="styles[toast.type]?.dot || 'bg-slate-400'"></span>
          <p class="flex-1 text-sm leading-relaxed">{{ toast.message }}</p>
          <button class="opacity-60 hover:opacity-100" aria-label="关闭提示" @click="dismiss(toast.id)">
            <svg class="h-3.5 w-3.5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 5l10 10M15 5L5 15" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
