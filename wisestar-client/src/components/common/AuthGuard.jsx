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

import { Navigate, useNavigate } from 'react-router-dom';
import { Result, Button } from 'antd';
import useUserStore from '../../stores/useUserStore';

// adminOnly=true 时（管理端路由）：仅系统用户（userType=SysUser）可访问，
// 学员（userType=Student）访问管理端时重定向到学员端 /student。
// required=[权限点...] 时（路由级权限）：用户拥有任一点才放行，否则显示 403。
export default function AuthGuard({ children, adminOnly = false, required }) {
  const { user, isLoggedIn, loading } = useUserStore();
  const navigate = useNavigate();

  // 正在加载用户信息（如页面首次加载时），暂时不渲染任何内容
  if (loading) {
    return null;
  }

  // 未登录 → 重定向到登录页
  // replace={true} 表示替换浏览器历史记录，避免用户按"返回"回到受保护页
  if (!isLoggedIn) {
    return <Navigate to="/login" replace />;
  }

  // 管理端路由：学员访问 → 重定向到学员端
  if (adminOnly && user?.userType !== 'SysUser') {
    return <Navigate to="/student" replace />;
  }

  // 路由级权限校验：无 required 或拥有任一权限点 → 放行
  if (required && required.length > 0) {
    const authorityList = user?.authorityList || [];
    const hasAny = required.some((p) => authorityList.includes(p));
    if (!hasAny) {
      return (
        <Result
          status="403"
          title="403"
          subTitle="您没有权限访问该页面"
          extra={
            <Button type="primary" onClick={() => navigate('/')}>
              返回首页
            </Button>
          }
        />
      );
    }
  }

  // 已登录 → 正常渲染子组件
  return children;
}
