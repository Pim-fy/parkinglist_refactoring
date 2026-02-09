// HTML과 다른 JSX파일을 연결하는 역할의 파일.
// 추가로 앱 전체에 적용되는 전역 설정을 관리.
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './index.css'          // 전역 스타일을 적용함.
import App from './App.jsx'   // App.jsx파일을 연결함.

createRoot(document.getElementById('root')).render(
  // document.getElementById('root')는 index.html에 있던 빈 div를 집어내는 코드임.
  <StrictMode>
    {/* 개발 중 코드가 잘못된 게 없는지 검사. */}
    <App />
  </StrictMode>,
)
