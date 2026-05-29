import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  optimizeDeps: {
    include: ['vue', 'vue-router', 'pinia', 'element-plus', '@element-plus/icons-vue', 'axios', 'dayjs']
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3017,
    strictPort: true, // 强制使用3017，不尝试其他端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1000, // 降低警告阈值，强制优化
    rollupOptions: {
      output: {
        manualChunks: {
          // Vue核心库单独打包
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          // Element Plus UI库单独打包
          'element-plus': ['element-plus', '@element-plus/icons-vue'],
          // PDF相关库单独打包（按需加载）
          'pdf-vendor': ['vue-pdf-embed'],
          // AI相关库单独打包
          'echarts': ['echarts']
        }
      }
    }
  }
})
