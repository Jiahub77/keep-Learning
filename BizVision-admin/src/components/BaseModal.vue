<script setup>
import { onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  open: { type: Boolean, default: false },
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  width: { type: String, default: 'max-w-lg' },
})

const emit = defineEmits(['close'])

function onKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

watch(
  () => props.open,
  (open) => {
    if (open) {
      document.addEventListener('keydown', onKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', onKeydown)
      document.body.style.overflow = ''
    }
  }
)

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-150 ease-out"
      enter-from-class="opacity-0"
      enter-to-class="opacity-100"
      leave-active-class="transition duration-100 ease-in"
      leave-from-class="opacity-100"
      leave-to-class="opacity-0"
    >
      <div v-if="open" class="fixed inset-0 z-40 flex items-start justify-center overflow-y-auto p-4 sm:p-6">
        <div class="fixed inset-0 bg-slate-900/40 backdrop-blur-[2px]" @click="emit('close')"></div>

        <div
          class="relative my-8 w-full rounded-2xl border border-slate-200 bg-white shadow-xl"
          :class="width"
        >
          <div class="flex items-start justify-between gap-4 border-b border-slate-100 px-6 py-4">
            <div>
              <h2 class="text-base font-semibold text-slate-900">{{ title }}</h2>
              <p v-if="subtitle" class="mt-0.5 text-xs text-slate-500">{{ subtitle }}</p>
            </div>
            <button
              type="button"
              class="btn-ghost -mr-2 h-8 w-8 !p-0"
              aria-label="关闭"
              @click="emit('close')"
            >
              <svg class="h-4 w-4" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M5 5l10 10M15 5L5 15" stroke-linecap="round" />
              </svg>
            </button>
          </div>

          <div class="px-6 py-5">
            <slot />
          </div>

          <div v-if="$slots.footer" class="flex items-center justify-end gap-2 border-t border-slate-100 px-6 py-4">
            <slot name="footer" />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>
