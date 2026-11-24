import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [react()],
    server: {
      port: 3100,
      host: '0.0.0.0',
      allowedHosts: ['.monkeycode-ai.online'],
      proxy: {
        // 推荐入口是主站 3000 的 /research/（由 wisestar-client 反代到这里）；
        // 这里再兜一层，保证直接访问 3100 时 /research/api 也能打到 7010。
        '/research/api': {
          target: env.API_TARGET || 'http://127.0.0.1:7010',
          changeOrigin: true,
          rewrite: (p) => p.replace(/^\/research/, ''),
        },
        '/api': {
          target: env.API_TARGET || 'http://127.0.0.1:7010',
          changeOrigin: true,
        },
      },
    },
  }
})
