/**
 * App.jsx - 应用根组件
 *
 * 职责:
 *   1. 配置 Ant Design 中文语言包
 *   2. 配置 React Router 路由表
 *   3. 应用初始化时恢复登录态（fetchCurrentUser）
 *
 * 路由结构:
 *   /login                  → LoginPage（公开，无需登录）
 *   /survey/:id             → SurveyViewPage（公开，填写问卷无需登录）
 *   /                       → DashboardPage（受 AuthGuard 保护）
 *   /projects               → ProjectListPage（受保护）
 *   /projects/:id/edit      → ProjectEditPage（问卷编辑器，受保护）
 *   /projects/:id/answers   → ProjectAnswersPage（指定问卷的答卷列表，受保护）
 *   /answers                → AnswerListPage（全局答案管理，受保护）
 *   /answers/:id            → AnswerDetailPage（答卷详情，受保护）
 *   /repos                  → RepoListPage（题库列表，受保护）
 *   /repos/:id              → RepoDetailPage（题库详情&题目管理，受保护）
 *   /templates              → 模板管理（待实现）
 *   /system                 → 系统管理（待实现）
 *
 * 认证流程:
 *   1. App 组件挂载 → useEffect 调用 fetchCurrentUser()
 *   2. fetchCurrentUser 请求 GET /api/currentUser
 *   3. 成功 → isLoggedIn = true → 正常渲染受保护页面
 *   4. 失败 → isLoggedIn = false → AuthGuard 重定向到 /login
 */

import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN'; // Ant Design 中文语言包
import { useEffect } from 'react';
import useUserStore from './stores/useUserStore';
import AuthGuard from './components/common/AuthGuard';
import MainLayout from './components/layout/MainLayout';
import LoginPage from './pages/login/LoginPage';
import SurveyViewPage from './pages/survey/SurveyViewPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import ProjectListPage from './pages/project/list/ProjectListPage';
import ProjectEditPage from './pages/project/edit/ProjectEditPage';
import ProjectAnswersPage from './pages/project/answers/ProjectAnswersPage';
import AnswerListPage from './pages/answer/AnswerListPage';
import AnswerDetailPage from './pages/answer/AnswerDetailPage';
import RepoListPage from './pages/repo/RepoListPage';
import RepoDetailPage from './pages/repo/RepoDetailPage';
import QuestionListPage from './pages/question/QuestionListPage';

export default function App() {
  const { fetchCurrentUser, isLoggedIn } = useUserStore();

  // ============================================================
  // 应用初始化：尝试恢复登录态
  // ============================================================
  // 原理: 之前登录时后端在 Cookie 中写入了 sk-token，
  //       刷新页面后 fetchCurrentUser() 携带 Cookie 验证身份
  useEffect(() => {
    fetchCurrentUser();
  }, []); // 空依赖数组 → 仅在组件首次挂载时执行一次

  return (
    // ConfigProvider: 为所有 Ant Design 组件设置中文语言
    <ConfigProvider locale={zhCN}>
      {/* BrowserRouter: React Router 的 HTML5 History 模式路由 */}
      <BrowserRouter>
        <Routes>
          {/* ---- 公开路由（无需登录） ---- */}
          {/* 登录页 */}
          <Route path="/login" element={<LoginPage />} />

          {/* 公开问卷填写页（任何人可访问） */}
          <Route path="/survey/:id" element={<SurveyViewPage />} />

          {/* ---- 受保护路由（需要登录） ---- */}
          {/* AuthGuard 包裹 MainLayout，所有子路由都受保护 */}
          <Route
            element={
              <AuthGuard>
                <MainLayout />
              </AuthGuard>
            }
          >
            {/* 仪表盘 */}
            <Route path="/" element={<DashboardPage />} />

            {/* 问卷列表 */}
            <Route path="/projects" element={<ProjectListPage />} />

            {/* 问卷编辑器 */}
            <Route path="/projects/:id/edit" element={<ProjectEditPage />} />

            {/* 答卷列表（指定问卷） */}
            <Route path="/projects/:id/answers" element={<ProjectAnswersPage />} />

            {/* 全局答案管理 */}
            <Route path="/answers" element={<AnswerListPage />} />

            {/* 答卷详情 */}
            <Route path="/answers/:id" element={<AnswerDetailPage />} />

            {/* 题库列表 */}
            <Route path="/repos" element={<RepoListPage />} />

            {/* 题库详情 */}
            <Route path="/repos/:id" element={<RepoDetailPage />} />

            {/* 题目管理（全局） */}
            <Route path="/questions" element={<QuestionListPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}
