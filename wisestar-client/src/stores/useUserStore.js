/**
 * useUserStore.js - 用户状态管理（Zustand）
 *
 * 使用 Zustand 管理全局用户状态，替代 Redux（更轻量、更简单）
 *
 * 状态:
 *   user       - 当前登录用户对象（null 表示未登录）
 *   isLoggedIn - 是否已登录的布尔标记
 *   loading    - 是否正在加载用户信息（App 挂载恢复登录态时置 true，AuthGuard 据此等待）
 *
 * 方法:
 *   fetchCurrentUser() - 页面刷新时尝试从后端恢复登录态
 *   login(username, password) - 执行登录并更新状态
 *   logout() - 执行登出并清空状态
 *
 * 被谁引用（本项目唯一的全局状态源）:
 *   - App.jsx            : fetchCurrentUser（挂载时恢复登录态）、isLoggedIn（控制路由渲染）
 *   - AuthGuard.jsx      : isLoggedIn / loading（路由守卫判断）
 *   - MainLayout.jsx     : user（顶栏用户名）、logout（退出登录）
 *   - LoginPage.jsx      : login（表单提交）
 *   - DashboardPage.jsx  : user（欢迎语）
 *
 * 核心数据流:
 *   App 挂载 → fetchCurrentUser → GET /api/currentUser（携带 sk-token Cookie）
 *   → 成功: user + isLoggedIn=true → AuthGuard 放行受保护路由
 *   → 失败: 状态清空 → AuthGuard 重定向 /login
 *
 * 使用方式（在组件中）:
 *   const { user, isLoggedIn, login, logout } = useUserStore();
 */

import { create } from 'zustand';
import { getCurrentUser, login as loginApi, logout as logoutApi } from '../api/user';

const useUserStore = create((set, get) => ({
  // ---- 状态 ----
  user: null,        // 当前登录用户信息
  isLoggedIn: false, // 登录状态标记
  loading: false,    // 加载中标记

  // ============================================================
  // 从后端获取当前用户（用于页面刷新时恢复登录态）
  // ============================================================
  // 原理: 之前登录时后端在 Cookie 中写入了 sk-token，
  //       刷新页面后通过 getCurrentUser() 携带 Cookie 验证身份
  fetchCurrentUser: async () => {
    try {
      const res = await getCurrentUser();
      // 后端返回用户数据 → 恢复登录状态
      set({ user: res.data, isLoggedIn: true, loading: false });
    } catch {
      // Cookie 失效或未登录 → 清空状态
      set({ user: null, isLoggedIn: false, loading: false });
    }
  },

  // ============================================================
  // 登录
  // ============================================================
  // 流程:
  //   1. 调用 loginApi（内部会执行 RSA 加密 + POST 登录）
  //   2. 登录成功后再次调用 getCurrentUser 获取用户信息
  //   3. 更新 Zustand 状态
  login: async (username, password) => {
    // 发送登录请求（密码在 api/user.js 中已 RSA 加密）
    await loginApi(username, password);
    // 登录成功后获取用户信息
    const res = await getCurrentUser();
    // 更新全局状态
    set({ user: res.data, isLoggedIn: true });
  },

  // ============================================================
  // 登出
  // ============================================================
  // 流程:
  //   1. 调用后端登出接口（清除服务端 session/Cookie）
  //   2. 清空前端状态
  logout: async () => {
    await logoutApi();
    set({ user: null, isLoggedIn: false });
  },
}));

export default useUserStore;
