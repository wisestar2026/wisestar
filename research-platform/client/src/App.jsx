import { lazy, Suspense } from 'react'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Spin } from 'antd'

const LoginPage = lazy(() => import('./pages/LoginPage'))
const MainLayout = lazy(() => import('./layouts/MainLayout'))
const PlanListPage = lazy(() => import('./pages/PlanListPage'))

const basename = (import.meta.env.BASE_URL || '/').replace(/\/$/, '') || '/'

const Loading = () => (
  <div className="page-loading">
    <Spin />
  </div>
)

export default function App() {
  return (
    <BrowserRouter basename={basename}>
      <Suspense fallback={<Loading />}>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route path="/" element={<MainLayout />}>
            <Route index element={<Navigate to="/plans" replace />} />
            <Route path="plans" element={<PlanListPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/plans" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
