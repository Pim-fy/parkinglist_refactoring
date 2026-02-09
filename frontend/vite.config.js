import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {          // api로 시작하는 요청 처리
        target: 'http://localhost:8080',    // http://localhost:8080로 재작성되어 요청
        changeOrigin: true,   // CORS 에러를 피하기 위해 origin을 변경
      },
    }
  }
})
