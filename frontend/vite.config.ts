import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/settlement-reports': 'http://localhost:8085',
      '/health': 'http://localhost:8085',
      '/orders': 'http://localhost:8085',
      '/ready': 'http://localhost:8085',
    },
  },
  test: {
    globals: true,
    environment: 'jsdom',
  },
})