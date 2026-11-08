import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发时把 /products 和 /article 代理到 Spring Boot，避免跨域；
// 打包后把 dist 丢到 Spring Boot 的 static 目录，同源访问，也不需要跨域配置。
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const target = env.VITE_PROXY_TARGET || 'http://localhost:8080'

  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/products': { target, changeOrigin: true },
        '/article': { target, changeOrigin: true },
      },
    },
    test: {
      environment: 'jsdom',
      include: ['src/**/*.spec.js'],
    },
  }
})
