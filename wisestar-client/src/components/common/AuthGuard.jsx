/**
 * AuthGuard.jsx - 路由守卫组件
 *
 * 作用: 保护需要登录才能访问的页面，未登录时自动跳转到登录页
 *
 * 工作原理:
 *   1. 从 useUserStore 读取 isLoggedIn 和 loading 状态
 *   2. loading 为 true 时：不渲染任何内容（等待 fetchCurrentUser 完成）
 *      —— 为什么返回 null 而不是显示加载动画：避免未登录用户闪屏看到受保护内容
 *   3. isLoggedIn 为 false 时：自动重定向到 /login
 *   4. isLoggedIn 为 true 时：正常渲染子组件（children）
 *
 * 被谁引用: App.jsx 路由表中，作为受保护路由组的父级包裹层
 * 使用方式（在路由配置中）:
 *   <Route element={<AuthGuard><MainLayout /></AuthGuard>}>
 *     <Route path="/" element={<DashboardPage />} />
 *   </Route>
 *   嵌套的二级路由均通过 <Outlet /> 渲染在 MainLayout 的内容区
 *
 * 数据来源: useUserStore（zustand 全局状态）
 *   登录态来源链: App.jsx 挂载 → fetchCurrentUser() → GET /api/currentUser → set isLoggedIn
 */

import { Navigate } from 'react-router-dom';
import useUserStore from '../../stores/useUserStore';

export default function AuthGuard({ children }) {
  const { isLoggedIn, loading } = useUserStore();

  // 正在加载用户信息（如页面首次加载时），暂时不渲染任何内容
  if (loading) {
    return null;
  }

  // 未登录 → 重定向到登录页
  // replace={true} 表示替换浏览器历史记录，避免用户按"返回"回到受保护页
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  // 已登录 → 正常渲染子组件
  return children;
}
