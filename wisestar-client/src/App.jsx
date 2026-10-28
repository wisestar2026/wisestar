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
import { lazy, Suspense, useEffect } from 'react';
import useUserStore from './stores/useUserStore';
import AuthGuard from './components/common/AuthGuard';
const MainLayout = lazy(() => import('./components/layout/MainLayout'));
const LoginPage = lazy(() => import('./pages/login/LoginPage'));
const StudentLoginPage = lazy(() => import('./pages/student/StudentLoginPage'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));
const RepoListPage = lazy(() => import('./pages/repo/RepoListPage'));
const RepoDetailPage = lazy(() => import('./pages/repo/RepoDetailPage'));
const QuestionListPage = lazy(() => import('./pages/question/QuestionListPage'));
const MathPaperManagePage = lazy(() => import('./pages/exercise/MathPaperManagePage'));
const PracticeHomePage = lazy(() => import('./pages/practice/PracticeHomePage'));
const PracticeSessionPage = lazy(() => import('./pages/practice/PracticeSessionPage'));
const WrongQuestionPage = lazy(() => import('./pages/practice/WrongQuestionPage'));
const StudentLayout = lazy(() => import('./pages/student/StudentLayout'));
const StudentHomePage = lazy(() => import('./pages/student/StudentHomePage'));
const StudyPage = lazy(() => import('./pages/student/StudyPage'));
const KnowledgePage = lazy(() => import('./pages/student/KnowledgePage'));
const KnowledgeDetectPage = lazy(() => import('./pages/student/KnowledgeDetectPage'));
const EnglishDiagnosePage = lazy(() => import('./pages/student/EnglishDiagnosePage'));
const StudentMyArchivePage = lazy(() => import('./pages/student/StudentMyArchivePage'));
const ProfilePage = lazy(() => import('./pages/student/ProfilePage'));
const MallPage = lazy(() => import('./pages/student/MallPage'));
const WrongBookPage = lazy(() => import('./pages/student/WrongBookPage'));
const EnglishCenterPage = lazy(() => import('./pages/student/EnglishCenterPage'));
const EnglishWordLearnPage = lazy(() => import('./pages/student/EnglishWordLearnPage'));
const EnglishWordPracticePage = lazy(() => import('./pages/student/EnglishWordPracticePage'));
const EnglishReadingPage = lazy(() => import('./pages/student/EnglishReadingPage'));
const EnglishReadAloudPage = lazy(() => import('./pages/student/EnglishReadAloudPage'));
const EnglishSentenceLearnPage = lazy(() => import('./pages/student/EnglishSentenceLearnPage'));
const EnglishGrammarLearnPage = lazy(() => import('./pages/student/EnglishGrammarLearnPage'));
const EnglishGrammarPracticePage = lazy(() => import('./pages/student/EnglishGrammarPracticePage'));
const EnglishUnitPracticePage = lazy(() => import('./pages/student/EnglishUnitPracticePage'));
const EnglishScenarioPracticePage = lazy(() => import('./pages/student/EnglishScenarioPracticePage'));
const EnglishPatternScenarioPage = lazy(() => import('./pages/student/EnglishPatternScenarioPage'));
const EnglishReviewPage = lazy(() => import('./pages/student/EnglishReviewPage'));
const MallGoodsManagePage = lazy(() => import('./pages/system/MallGoodsManagePage'));
const ChapterManagePage = lazy(() => import('./pages/knowledge/ChapterManagePage'));
const SectionManagePage = lazy(() => import('./pages/knowledge/SectionManagePage'));
const KnowledgePointManagePage = lazy(() => import('./pages/knowledge/KnowledgePointManagePage'));
const PracticeStrategyPage = lazy(() => import('./pages/knowledge/PracticeStrategyPage'));
const StudentManagePage = lazy(() => import('./pages/student/StudentManagePage'));
const StudentArchivePage = lazy(() => import('./pages/student/StudentArchivePage'));
const OrderManagePage = lazy(() => import('./pages/student/OrderManagePage'));
const RoleManagePage = lazy(() => import('./pages/hr/RoleManagePage'));
const UserManagePage = lazy(() => import('./pages/system/UserManagePage'));
const DeptManagePage = lazy(() => import('./pages/system/DeptManagePage'));
const PositionManagePage = lazy(() => import('./pages/system/PositionManagePage'));
const DictManagePage = lazy(() => import('./pages/system/DictManagePage'));
const DictItemManagePage = lazy(() => import('./pages/system/DictItemManagePage'));
const AiSettingPage = lazy(() => import('./pages/system/AiSettingPage'));
const WordManagePage = lazy(() => import('./pages/english/WordManagePage'));
const WordAiManagePage = lazy(() => import('./pages/english/WordAiManagePage'));
const ScenarioPackManagePage = lazy(() => import('./pages/english/ScenarioPackManagePage'));
const UnitPaperManagePage = lazy(() => import('./pages/english/UnitPaperManagePage'));
const SentencePackManagePage = lazy(() => import('./pages/english/SentencePackManagePage'));
const StudentSupervisionPage = lazy(() => import('./pages/student/StudentSupervisionPage'));
const HandRaisePage = lazy(() => import('./pages/student/HandRaisePage'));
const TaskAssignmentPage = lazy(() => import('./pages/student/TaskAssignmentPage'));
const TeachingResearchPlatformPage = lazy(() => import('./pages/exercise/TeachingResearchPlatformPage'));
const SentenceManagePage = lazy(() => import('./pages/english/SentenceManagePage'));
const VerifyPage = lazy(() => import('./pages/mall/VerifyPage'));
const CampusManagePage = lazy(() => import('./pages/admin/CampusManagePage'));
const UnitManagePage = lazy(() => import('./pages/english/UnitManagePage'));
const EnglishSectionManagePage = lazy(() => import('./pages/english/SectionManagePage'));
const EnglishGrammarManagePage = lazy(() => import('./pages/english/GrammarManagePage'));
const WordImageReviewPage = lazy(() => import('./pages/english/WordImageReviewPage'));


// 路由懒加载骨架：纯 DOM，不引入 antd，首屏不用等额外 chunk
function RouteFallback() {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        fontFamily: 'system-ui, sans-serif',
        fontSize: 14,
        color: '#8a94a6',
      }}
    >
      正在加载…
    </div>
  );
}

export default function App() {
  const { fetchCurrentUser } = useUserStore();

  // ============================================================
  // 应用初始化：尝试恢复登录态
  // ============================================================
  // 原理: 之前登录时后端在 Cookie 中写入了 sk-token，
  //       刷新页面后 fetchCurrentUser() 携带 Cookie 验证身份
  useEffect(() => {
    fetchCurrentUser();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 空依赖数组 → 仅在组件首次挂载时执行一次

  return (
    // ConfigProvider: 为所有 Ant Design 组件设置中文语言
    // cssVar：把主题变量交给 CSS 变量，减少旧机器上 antd 运行时的样式哈希计算
    <ConfigProvider locale={zhCN} theme={{ cssVar: true }}>
      {/* BrowserRouter: React Router 的 HTML5 History 模式路由 */}
      <BrowserRouter>
        <Suspense fallback={<RouteFallback />}>
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
            {/* 知识点检测: 勾选单元自动组卷 + 薄弱点诊断报告（诊断性质，不发奖） */}
            <Route path="detect" element={<KnowledgeDetectPage />} />
            {/* 英语学习诊断: 三档诊断卷 + 家长向可打印报告 */}
            <Route path="diagnose" element={<EnglishDiagnosePage />} />
            {/* 我的学习档案: 学习目标/知识点/当日情况/上课记录/学期报告（只读可打印） */}
            <Route path="archive" element={<StudentMyArchivePage />} />
            {/* 我的档案荣誉墙: 证书陈列 + 成长统计 */}
            <Route path="profile" element={<ProfilePage />} />
            {/* 荣誉商城: 多科合并兑换 */}
            <Route path="mall" element={<MallPage />} />
            {/* 英语学习中心: 单元进度 + 智能复习 */}
            <Route path="english" element={<EnglishCenterPage />} />
            {/* 英语单词卡片学习（?unit=） */}
            <Route path="english/word" element={<EnglishWordLearnPage />} />
            {/* 英语单词选择题练习（?unit=） */}
            <Route path="english/practice" element={<EnglishWordPracticePage />} />
            {/* 英语句子学习·连词成句（?unit=） */}
            <Route path="english/sentence" element={<EnglishSentenceLearnPage />} />
            {/* 英语重点语法（只读，?unit=） */}
            <Route path="english/grammar" element={<EnglishGrammarLearnPage />} />
            {/* 英语语法练习（?unit=，按正确率回写语法掌握度） */}
            <Route path="english/grammar-practice" element={<EnglishGrammarPracticePage />} />
            {/* 英语单元练习（?unit=，仅本单元题目） */}
            <Route path="english/unit-practice" element={<EnglishUnitPracticePage />} />
            {/* 英语情景对话练习（?unit=，AI 按单元单词实时生成） */}
            <Route path="english/scenario" element={<EnglishScenarioPracticePage />} />
            {/* 英语重点句式情景练习（?unit=，AI 按单元重点句式实时生成） */}
            <Route path="english/pattern-scenario" element={<EnglishPatternScenarioPage />} />
            {/* 英语智能复习（单词 / 重点句子 / 语法 三类） */}
            <Route path="english/reading" element={<EnglishReadingPage />} />
            <Route path="english/read-aloud" element={<EnglishReadAloudPage />} />
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
                <AuthGuard required={['repo:list']}>
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

            {/* 题目管理（习题管理·理科：仅数学） */}
            <Route
              path="/questions"
              element={
                <AuthGuard required={['template:list']}>
                  <QuestionListPage lockedSubject="数学" />
                </AuthGuard>
              }
            />

            {/* 数学组卷（通用/按学员错题 → 打印卷 + 答案解析） */}
            <Route
              path="/math-paper"
              element={
                <AuthGuard required={['template:list']}>
                  <MathPaperManagePage />
                </AuthGuard>
              }
            />

            {/* 教研平台（学科+年级→章节→小节→知识点；名称即改 + 知识点直绑题目编辑/加绑） */}
            <Route
              path="/exercise/list"
              element={
                <AuthGuard required={['repo:list']}>
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
            <Route
              path="/knowledge/strategy"
              element={
                <AuthGuard required={['knowledge:strategy']}><PracticeStrategyPage /></AuthGuard>
              }
            />

            {/* 学员管理（学员列表 / 订单管理） */}
            <Route
              path="/students"
              element={
                <AuthGuard required={['student:list']}>
                  <StudentManagePage />
                </AuthGuard>
              }
            />
            {/* 学员档案（学习规划/上课记录/学期报告，可打印） */}
            <Route
              path="/students/:studentId/archive"
              element={
                <AuthGuard required={['student:archive']}>
                  <StudentArchivePage />
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
            {/* 举手提醒：学员做题举手（求助讲解/题干有误/答案有误），学管师与教研在此处理 */}
            <Route
              path="/student/hand-raise"
              element={
                <AuthGuard required={['handraise:list']}>
                  <HandRaisePage />
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
                <AuthGuard required={['mall:list']}>
                  <MallGoodsManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/orders"
              element={
                <AuthGuard required={['order:list']}>
                  <OrderManagePage />
                </AuthGuard>
              }
            />

            {/* 行政管理（角色权限）：菜单 key 为 /admin/roles，此处保持一致 */}
            <Route
              path="/admin/roles"
              element={
                <AuthGuard required={['system:role:list']}>
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
                <AuthGuard required={['system:user:list']}>
                  <UserManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/depts"
              element={
                <AuthGuard required={['system:dept:list']}>
                  <DeptManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/positions"
              element={
                <AuthGuard required={['system:position:list']}>
                  <PositionManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/dicts"
              element={
                <AuthGuard required={['system:dict:list']}>
                  <DictManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/dict-items"
              element={
                <AuthGuard required={['system:dictItem:list']}>
                  <DictItemManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/system/ai"
              element={
                <AuthGuard required={['system:ai']}>
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
              path="/english/word-image"
              element={
                <AuthGuard required={['english:word:update']}>
                  <WordImageReviewPage />
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
              path="/english/word-ai"
              element={
                <AuthGuard required={['english:word:ai']}>
                  <WordAiManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/scenario-pack"
              element={
                <AuthGuard required={['english:word:ai']}>
                  <ScenarioPackManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/unit-paper"
              element={
                <AuthGuard required={['template:list']}>
                  <UnitPaperManagePage />
                </AuthGuard>
              }
            />
            <Route
              path="/english/sentence-pack"
              element={
                <AuthGuard required={['english:word:ai']}>
                  <SentencePackManagePage />
                </AuthGuard>
              }
            />
            {/* 英语题目（复用题目管理，锁定学科=英语，呈现形式与理科一致） */}
            <Route
              path="/english/questions"
              element={
                <AuthGuard required={['template:list']}>
                  <QuestionListPage lockedSubject="英语" />
                </AuthGuard>
              }
            />

            {/* 积分管理 */}
            <Route
              path="/mall/verify"
              element={
                <AuthGuard required={['mall:list']}>
                  <VerifyPage />
                </AuthGuard>
              }
            />

            {/* 行政管理 */}
            <Route
              path="/admin/campus"
              element={
                <AuthGuard required={['campus:list']}>
                  <CampusManagePage />
                </AuthGuard>
              }
            />
          </Route>
          </Routes>
        </Suspense>
      </BrowserRouter>
    </ConfigProvider>
  );
}
