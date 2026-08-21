/**
 * App.jsx - 应用根组件
 *
 * 职责:
 *   1. 配置 Ant Design 中文语言包
 *   2. 配置 React Router 路由表
 *   3. 应用初始化时恢复登录态（fetchCurrentUser）
 *
 * 被谁引用: main.jsx（ReactDOM.createRoot 挂载）
 * 依赖:
 *   - react-router-dom: BrowserRouter / Routes / Route
 *   - antd ConfigProvider: 全局组件文案中文化
 *   - useUserStore: 登录态全局状态
 *   - AuthGuard / MainLayout: 受保护路由的守卫与布局骨架
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
 *   /questions              → QuestionListPage（题目管理，受保护）
 *   /register               → RegisterPage（注册，公开）
 *   /system/users          → UserManagePage（用户管理，受保护）
 *   /system/depts          → DeptManagePage（部门管理，受保护）
 *   /system/positions      → PositionManagePage（岗位管理，受保护）
 *   /system/dicts          → DictManagePage（字典管理，受保护）
 *   /system/dict-items     → DictItemManagePage（字典条目管理，受保护）
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
import StudentLoginPage from './pages/student/StudentLoginPage';
import RegisterPage from './pages/login/RegisterPage';
import SurveyViewPage from './pages/survey/SurveyViewPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import ProjectListPage from './pages/project/list/ProjectListPage';
import ProjectEditPage from './pages/project/edit/ProjectEditPage';
import ProjectAnswersPage from './pages/project/answers/ProjectAnswersPage';
import AnswerListPage from './pages/answer/AnswerListPage';
import AnswerDetailPage from './pages/answer/AnswerDetailPage';
import RepoListPage from './pages/repo/RepoListPage';
import RepoDetailPage from './pages/repo/RepoDetailPage';
import RepoAssignPage from './pages/repo/RepoAssignPage';
import QuestionListPage from './pages/question/QuestionListPage';
import PracticeHomePage from './pages/practice/PracticeHomePage';
import PracticeSessionPage from './pages/practice/PracticeSessionPage';
import WrongQuestionPage from './pages/practice/WrongQuestionPage';
import StudentLayout from './pages/student/StudentLayout';
import StudentHomePage from './pages/student/StudentHomePage';
import StudyPage from './pages/student/StudyPage';
import KnowledgePage from './pages/student/KnowledgePage';
import ProfilePage from './pages/student/ProfilePage';
import MallPage from './pages/student/MallPage';
import WrongBookPage from './pages/student/WrongBookPage';
import ChapterManagePage from './pages/knowledge/ChapterManagePage';
import SectionManagePage from './pages/knowledge/SectionManagePage';
import KnowledgePointManagePage from './pages/knowledge/KnowledgePointManagePage';
import StudentManagePage from './pages/student/StudentManagePage';
import OrderManagePage from './pages/student/OrderManagePage';
import RoleManagePage from './pages/hr/RoleManagePage';
import UserManagePage from './pages/system/UserManagePage';
import DeptManagePage from './pages/system/DeptManagePage';
import PositionManagePage from './pages/system/PositionManagePage';
import DictManagePage from './pages/system/DictManagePage';
import DictItemManagePage from './pages/system/DictItemManagePage';

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
          <Route path="/student-login" element={<StudentLoginPage />} />

          {/* 注册页（POST /api/public/register，是否开放由后端配置） */}
          <Route path="/register" element={<RegisterPage />} />

          {/* 公开问卷填写页（任何人可访问） */}
          <Route path="/survey/:id" element={<SurveyViewPage />} />

          {/* 学生端（海底AI自习室 · 学海智习系统 V2.0 纯前端原型，独立全屏路由） */}
          {/* 公共布局 StudentLayout 提供海洋背景 + 顶部通栏（学科 Tab/版本/纯净模式） */}
          <Route
            path="/student"
            element={
              <AuthGuard>
                <StudentLayout />
              </AuthGuard>
            }
          >
            {/* 学生首页: 我的档案 / 学海研习 / 荣誉商城 三卡 + 今日数据总览 */}
            <Route index element={<StudentHomePage />} />
            {/* 学海研习主页面: 三栏（章节导航 / 主内容 / 快捷操作） */}
            <Route path="study" element={<StudyPage />} />
            {/* 知识点详情页: 预习 / 练习 / 试炼 / 错题（?tab=；?sectionId= 为后台配置内容真实模式） */}
            <Route path="knowledge" element={<KnowledgePage />} />
            <Route path="knowledge/:kpId" element={<KnowledgePage />} />
            {/* 错题本: 练习错题自动收录（/api/practice/wrong-list） */}
            <Route path="wrong" element={<WrongBookPage />} />
            {/* 我的档案荣誉墙: 证书陈列 + 成长统计 */}
            <Route path="profile" element={<ProfilePage />} />
            {/* 荣誉商城: 多科合并兑换 */}
            <Route path="mall" element={<MallPage />} />
          </Route>

          {/* ---- 受保护路由（需要登录） ---- */}
          {/* AuthGuard 包裹 MainLayout，所有子路由都受保护；adminOnly 仅允许系统用户进入管理端 */}
          <Route
            element={
              <AuthGuard adminOnly>
                <MainLayout />
              </AuthGuard>
            }
          >
            {/* 仪表盘 */}
            <Route path="/" element={<AuthGuard required={['home']}><DashboardPage /></AuthGuard>} />

            {/* 问卷列表 */}
            <Route
              path="/projects"
              element={
                <AuthGuard required={['project:list', 'project:detail', 'project:create', 'project:update', 'project:delete']}>
                  <ProjectListPage />
                </AuthGuard>
              }
            />

            {/* 问卷编辑器 */}
            <Route
              path="/projects/:id/edit"
              element={
                <AuthGuard required={['project:update']}><ProjectEditPage /></AuthGuard>
              }
            />

            {/* 答卷列表（指定问卷） */}
            <Route
              path="/projects/:id/answers"
              element={
                <AuthGuard required={['answer:list', 'answer:detail']}><ProjectAnswersPage /></AuthGuard>
              }
            />

            {/* 全局答案管理 */}
            <Route
              path="/answers"
              element={
                <AuthGuard required={['answer:list', 'answer:detail', 'answer:create', 'answer:update', 'answer:delete', 'answer:export', 'answer:upload']}>
                  <AnswerListPage />
                </AuthGuard>
              }
            />

            {/* 答卷详情 */}
            <Route
              path="/answers/:id"
              element={
                <AuthGuard required={['answer:detail']}><AnswerDetailPage /></AuthGuard>
              }
            />

            {/* 题库列表 */}
            <Route
              path="/repos"
              element={
                <AuthGuard required={['repo:list', 'repo:detail', 'repo:create', 'repo:update', 'repo:delete']}>
                  <RepoListPage />
                </AuthGuard>
              }
            />

            {/* 题库详情 */}
            <Route
              path="/repos/:id"
              element={
                <AuthGuard required={['repo:detail']}><RepoDetailPage /></AuthGuard>
              }
            />

            {/* 题库分配（老师给学员分配练习题库） */}
            <Route
              path="/repo-assign"
              element={
                <AuthGuard required={['repo:list', 'repo:update']}><RepoAssignPage /></AuthGuard>
              }
            />

            {/* 题目管理（全局） */}
            <Route
              path="/questions"
              element={
                <AuthGuard required={['template:list', 'template:create', 'template:update', 'template:delete']}>
                  <QuestionListPage />
                </AuthGuard>
              }
            />

            {/* 在线练习（选题页） */}
            <Route
              path="/practice"
              element={
                <AuthGuard required={['exercise:list']}><PracticeHomePage /></AuthGuard>
              }
            />

            {/* 在线练习答题页（按 mode + ids 渲染对应答题交互） */}
            <Route path="/practice/session" element={<PracticeSessionPage />} />

            {/* 错题库管理（题目 × 学员聚合错题列表） */}
            <Route
              path="/wrong-questions"
              element={
                <AuthGuard required={['repo:list', 'exercise:list']}><WrongQuestionPage /></AuthGuard>
              }
            />

            {/* ---- 知识管理板块（学科 → 章节 → 小节 → 知识点 三级管理） ---- */}
            <Route
              path="/knowledge/chapters"
              element={
                <AuthGuard required={['knowledge:list']}><ChapterManagePage /></AuthGuard>
              }
            />
            <Route
              path="/knowledge/sections"
              element={
                <AuthGuard required={['knowledge:list']}><SectionManagePage /></AuthGuard>
              }
            />
            <Route
              path="/knowledge/points"
              element={
                <AuthGuard required={['knowledge:list']}><KnowledgePointManagePage /></AuthGuard>
              }
            />

            {/* 学员管理（学员列表 / 订单管理） */}
            <Route
              path="/students"
              element={
                <AuthGuard required={['student:list', 'student:create', 'student:update', 'student:delete']}>
                  <StudentManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/orders"
              element={
                <AuthGuard required={['order:list', 'order:create', 'order:update', 'order:delete']}>
                  <OrderManagePage />
                </AuthGuard>
              }
            />

            {/* 人事管理（角色权限） */}
            <Route
              path="/hr/roles"
              element={
                <AuthGuard required={['system:role:list', 'system:role:create', 'system:role:update', 'system:role:delete']}>
                  <RoleManagePage />
                </AuthGuard>
              }
            />

            {/* 系统管理：用户/部门/岗位/字典/字典条目 */}
            <Route
              path="/system/users"
              element={
                <AuthGuard required={['system:user:list', 'system:user:create', 'system:user:update', 'system:user:delete']}>
                  <UserManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/depts"
              element={
                <AuthGuard required={['system:dept:list', 'system:dept:create', 'system:dept:update', 'system:dept:delete']}>
                  <DeptManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/positions"
              element={
                <AuthGuard required={['system:position:list', 'system:position:create', 'system:position:update', 'system:position:delete']}>
                  <PositionManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/dicts"
              element={
                <AuthGuard required={['system:dict:list', 'system:dict:create', 'system:dict:update', 'system:dict:delete']}>
                  <DictManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/dict-items"
              element={
                <AuthGuard required={['system:dictItem:list', 'system:dictItem:create', 'system:dictItem:update', 'system:dictItem:delete', 'system:dictItem:import']}>
                  <DictItemManagePage />
                </AuthGuard>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}
