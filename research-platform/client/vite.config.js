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
        '/api': {
          target: env.API_TARGET || 'http://127.0.0.1:7010',
          changeOrigin: true,
        },
      },
    },
  }
})
