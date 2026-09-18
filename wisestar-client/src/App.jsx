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
 *   /                       → DashboardPage（受 AuthGuard 保护）
 *   /repos                  → RepoListPage（题库列表，受保护）
 *   /repos/:id              → RepoDetailPage（题库详情&题目管理，受保护）
 *   /questions              → QuestionListPage（题目管理，受保护）
 *   /register               → 重定向到 /login（管理端不允许自主注册）
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

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN'; // Ant Design 中文语言包
import { useEffect } from 'react';
import useUserStore from './stores/useUserStore';
import AuthGuard from './components/common/AuthGuard';
import MainLayout from './components/layout/MainLayout';
import LoginPage from './pages/login/LoginPage';
import StudentLoginPage from './pages/student/StudentLoginPage';
import DashboardPage from './pages/dashboard/DashboardPage';
import RepoListPage from './pages/repo/RepoListPage';
import RepoDetailPage from './pages/repo/RepoDetailPage';
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
import EnglishCenterPage from './pages/student/EnglishCenterPage';
import EnglishWordLearnPage from './pages/student/EnglishWordLearnPage';
import EnglishSentenceLearnPage from './pages/student/EnglishSentenceLearnPage';
import EnglishReviewPage from './pages/student/EnglishReviewPage';
import MallGoodsManagePage from './pages/system/MallGoodsManagePage';
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
import AiSettingPage from './pages/system/AiSettingPage';
import WordStudyPage from './pages/english/WordStudyPage';
import WordBookManagePage from './pages/english/WordBookManagePage';
import WordManagePage from './pages/english/WordManagePage';
import WordAiManagePage from './pages/english/WordAiManagePage';
import StudentSupervisionPage from './pages/student/StudentSupervisionPage';
import TaskAssignmentPage from './pages/student/TaskAssignmentPage';
import TeachingResearchPlatformPage from './pages/exercise/TeachingResearchPlatformPage';
import SentenceManagePage from './pages/english/SentenceManagePage';
import VerifyPage from './pages/mall/VerifyPage';
import CampusManagePage from './pages/admin/CampusManagePage';
import UnitManagePage from './pages/english/UnitManagePage';
import EnglishSectionManagePage from './pages/english/SectionManagePage';
import EnglishGrammarManagePage from './pages/english/GrammarManagePage';


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

          {/* 管理端不允许自主注册：/register 直接回到登录页 */}
          <Route path="/register" element={<Navigate to="/login" replace />} />


          {/* 学生端（海洋智学 · 学海智习系统 V2.0 纯前端原型，独立全屏路由） */}
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
            {/* 英语学习中心: 单元进度 + 智能复习 */}
            <Route path="english" element={<EnglishCenterPage />} />
            {/* 英语单词卡片学习（?unit=） */}
            <Route path="english/word" element={<EnglishWordLearnPage />} />
            {/* 英语句子学习·连词成句（?unit=） */}
            <Route path="english/sentence" element={<EnglishSentenceLearnPage />} />
            {/* 英语智能复习（单词 + 句子混合队列） */}
            <Route path="english/review" element={<EnglishReviewPage />} />
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

            {/* 练习列表 */}
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

            {/* 题目管理（全局） */}
            <Route
              path="/questions"
              element={
                <AuthGuard required={['template:list', 'template:create', 'template:update', 'template:delete']}>
                  <QuestionListPage />
                </AuthGuard>
              }
            />

            {/* 教研平台（学科+年级→章节→小节→知识点；名称即改 + 知识点直绑题目编辑/加绑） */}
            <Route
              path="/exercise/list"
              element={
                <AuthGuard required={['repo:list', 'template:list']}>
                  <TeachingResearchPlatformPage />
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
              path="/students/activity"
              element={
                <AuthGuard required={['student:supervision']}>
                  <StudentSupervisionPage />
                </AuthGuard>
              }
            />
            {/* 学员督学：学管师/老师/管理员实时查看在线学员学习位置与题目答案解析 */}
            <Route
              path="/student/supervision"
              element={
                <AuthGuard required={['student:supervision']}>
                  <StudentSupervisionPage />
                </AuthGuard>
              }
            />
            {/* 任务发布：学管师向学员下发纯文本任务（每日最多 3 条），学员端首页今日任务卡片展示 */}
            <Route
              path="/student/task-assignment"
              element={
                <AuthGuard required={['task:list']}>
                  <TaskAssignmentPage />
                </AuthGuard>
              }
            />
            <Route
              path="/mall/goods"
              element={
                <AuthGuard required={['mall:list', 'mall:create', 'mall:update', 'mall:delete']}>
                  <MallGoodsManagePage />
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

            {/* 行政管理（角色权限）：菜单 key 为 /admin/roles，此处保持一致 */}
            <Route
              path="/admin/roles"
              element={
                <AuthGuard required={['system:role:list', 'system:role:create', 'system:role:update', 'system:role:delete']}>
                  <RoleManagePage />
                </AuthGuard>
              }
            />
            {/* 兼容旧路径 /hr/roles（历史书签） */}
            <Route path="/hr/roles" element={<Navigate to="/admin/roles" replace />} />

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
            <Route
              path="/system/ai"
              element={
                <AuthGuard required={['system:role:list']}>
                  <AiSettingPage />
                </AuthGuard>
              }
            />

            {/* 英语板块 */}
            <Route
              path="/english/unit"
              element={
                <AuthGuard required={['english:unit:list']}>
                  <UnitManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/section"
              element={
                <AuthGuard required={['english:section:list']}>
                  <EnglishSectionManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/grammar"
              element={
                <AuthGuard required={['english:grammar:list']}>
                  <EnglishGrammarManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/word"
              element={
                <AuthGuard required={['english:word:list']}>
                  <WordManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/sentence"
              element={
                <AuthGuard required={['english:sentence:list']}>
                  <SentenceManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/study"
              element={
                <AuthGuard>
                  <WordStudyPage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/word-book"
              element={
                <AuthGuard>
                  <WordBookManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/word-ai"
              element={
                <AuthGuard required={['english:word:ai']}>
                  <WordAiManagePage />
                </AuthGuard>
              }
            />

            {/* 积分管理 */}
            <Route
              path="/mall/verify"
              element={
                <AuthGuard>
                  <VerifyPage />
                </AuthGuard>
              }
            />

            {/* 行政管理 */}
            <Route
              path="/admin/campus"
              element={
                <AuthGuard>
                  <CampusManagePage />
                </AuthGuard>
              }
            />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}
