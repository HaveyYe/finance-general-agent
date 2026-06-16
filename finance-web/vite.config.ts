import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // 将 /agent 请求代理到 mcp-gateway
      '/agent': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
      // 将 /mcp 请求代理到统一 MCP Gateway，供直接工具调用页面使用
      '/mcp': {
        target: 'http://localhost:9000',
        changeOrigin: true,
      },
      '/knowledge': {
        target: 'http://localhost:8091',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
  },
})
