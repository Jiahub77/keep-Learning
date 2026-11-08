<script setup>
import BaseModal from './BaseModal.vue'

defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '确认操作' },
  description: { type: String, default: '' },
  confirmText: { type: String, default: '确认' },
  loading: { type: Boolean, default: false },
  danger: { type: Boolean, default: true },
})

const emit = defineEmits(['confirm', 'close'])
</script>

<template>
  <BaseModal :open="open" :title="title" width="max-w-md" @close="emit('close')">
    <div class="flex gap-4">
      <div
        class="flex h-10 w-10 flex-none items-center justify-center rounded-full"
        :class="danger ? 'bg-rose-50 text-rose-600' : 'bg-indigo-50 text-indigo-600'"
      >
        <svg class="h-5 w-5" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.6">
          <path d="M10 6.5v4.2M10 14h.01" stroke-linecap="round" />
          <path d="M8.6 3.2 2.3 14.1a1.6 1.6 0 0 0 1.4 2.4h12.6a1.6 1.6 0 0 0 1.4-2.4L11.4 3.2a1.6 1.6 0 0 0-2.8 0Z" stroke-linejoin="round" />
        </svg>
      </div>
      <p class="pt-1.5 text-sm leading-relaxed text-slate-600">{{ description }}</p>
    </div>

    <template #footer>
      <button type="button" class="btn-secondary" @click="emit('close')">取消</button>
      <button
        type="button"
        :class="danger ? 'btn-danger' : 'btn-primary'"
        :disabled="loading"
        @click="emit('confirm')"
      >
        <span
          v-if="loading"
          class="h-3.5 w-3.5 animate-spin rounded-full border-2 border-white/40 border-t-white"
        ></span>
        {{ confirmText }}
      </button>
    </template>
  </BaseModal>
</template>
