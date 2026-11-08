import { reactive } from 'vue'

const toasts = reactive([])
let seed = 0

export function useToast() {
  function dismiss(id) {
    const index = toasts.findIndex((t) => t.id === id)
    if (index > -1) toasts.splice(index, 1)
  }

  function push(message, type = 'success') {
    const id = ++seed
    toasts.push({ id, message, type })
    setTimeout(() => dismiss(id), 3600)
  }

  return {
    toasts,
    dismiss,
    success: (message) => push(message, 'success'),
    error: (message) => push(message, 'error'),
    info: (message) => push(message, 'info'),
  }
}
