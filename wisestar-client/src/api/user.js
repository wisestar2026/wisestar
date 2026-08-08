/**
 * user.js - 用户相关 API 接口
 *
 * 包含:
 *   - 登录（含 RSA 密码加密流程）
 *   - 登出
 *   - 注册
 *   - 获取当前用户信息
 *   - 获取用户概览数据
 *   - 获取系统信息（含 RSA 公钥）
 *   - 获取可注册角色列表
 *
 * 登录流程说明:
 *   1. 调用 getSystemInfo() 获取 RSA 公钥（GET /api/system）
 *   2. 使用 jsencrypt 库用公钥加密密码
 *   3. 将加密后的密码发送给后端 /api/public/login
 *   4. 后端验证通过后通过 Set-Cookie 写入 sk-token，后续请求自动携带
 *
 * 调用方一览:
 *   - getSystemInfo : 仅被本文件的 login() 内部调用（登录时获取 RSA 公钥）
 *   - login         : useUserStore.login → LoginPage.onFinish
 *   - logout        : useUserStore.logout → MainLayout.handleLogout
 *   - register      : 注册页面（当前前端暂无注册页入口，接口预留）
 *   - getCurrentUser: useUserStore.fetchCurrentUser / login（App 挂载时恢复登录态）
 *   - getUserOverview: DashboardPage（仪表盘统计卡片）
 *   - getRegisterRoles: 注册页角色下拉（接口预留）
 */

import request from './request';
import JSEncrypt from 'jsencrypt';

// ============================================================
// 获取系统信息
// ============================================================
// 后端接口: GET /api/system
// 返回: { code: 200, data: { publicKey: "RSA公钥", name: "系统名称", ... } }
// 用途: 登录前获取 RSA 公钥，用于加密密码
// 调用方: 本文件 login() 内部（不在页面中直接调用）
export async function getSystemInfo() {
  return request.get('/system');
}

// ============================================================
// 登录
// ============================================================
// 后端接口: POST /api/public/login
// 参数: username - 用户名, password - 明文密码
// 流程:
//   1. 先调用 getSystemInfo() 获取 RSA 公钥
//   2. 用公钥加密密码（密码不会以明文传输）
//   3. 发送 POST /api/public/login，body: { username, password: "加密后的密码" }
// 返回: { code: 200, data: { ... } }（登录成功后后端 Set-Cookie 写入 sk-token）
// 调用方: useUserStore.login（LoginPage 表单提交时触发）
export async function login(username, password) {
  // 步骤1：获取 RSA 公钥（publicKey 为后端生成的 RSA 公钥字符串）
  const systemRes = await getSystemInfo();
  const publicKey = systemRes.data.publicKey;

  // 步骤2：使用 jsencrypt 库进行 RSA 加密
  // 为什么这么写：密码不能明文传输，防止被抓包泄露；
  // jsencrypt 的 encrypt() 返回加密后的 Base64 字符串
  const encrypt = new JSEncrypt();
  encrypt.setPublicKey(publicKey);
  const encryptedPwd = encrypt.encrypt(password);

  // 加密失败时（如公钥无效、encrypt 返回 false）抛出错误
  if (!encryptedPwd) {
    throw new Error('RSA 加密失败，请检查系统配置');
  }

  // 步骤3：发送登录请求（密码已加密）
  return request.post('/public/login', {
    username,
    password: encryptedPwd,
  });
}

// ============================================================
// 登出
// ============================================================
// 后端接口: POST /api/public/logout
// 调用后端登出接口，清除服务端 session / sk-token Cookie
// 调用方: useUserStore.logout（MainLayout 右上角"退出登录"）
export async function logout() {
  return request.post('/public/logout');
}

// ============================================================
// 注册
// ============================================================
// 后端接口: POST /api/public/register
// 参数: data - 注册表单数据 { username, password, ... }
// 调用方: 注册页面（前端暂未接入，接口预留）
export async function register(data) {
  return request.post('/public/register', data);
}

// ============================================================
// 获取当前登录用户信息
// ============================================================
// 后端接口: GET /api/currentUser
// 返回: { code: 200, data: { id, name, username, ... } }
// 用途: 页面刷新时恢复登录态，显示用户头像和名称
// 调用方:
//   - useUserStore.fetchCurrentUser（App.jsx 挂载时执行，用于恢复登录态）
//   - useUserStore.login（登录成功后二次拉取用户信息）
export async function getCurrentUser() {
  return request.get('/currentUser');
}

// ============================================================
// 获取用户概览统计数据
// ============================================================
// 后端接口: GET /api/userOverview
// 返回: { code: 200, data: { surveyCount, examCount, userCount, todayAnswerCount } }
//   surveyCount     - 我创建的问卷数量
//   examCount       - 我创建的考试数量
//   userCount       - 团队成员数量
//   todayAnswerCount - 今日答卷数量
// 调用方: DashboardPage（仪表盘 4 个统计卡片，组件挂载时调用一次）
export async function getUserOverview() {
  return request.get('/userOverview');
}

// ============================================================
// 分页查询用户列表（管理端：题库分配选学员）
// ============================================================
// 后端接口: GET /api/user/list
// 参数: { current, pageSize, name?: 姓名模糊搜索 }
// 返回: { code: 200, data: { list: [{ id, name, username, phone }], total } }
// 权限: system:user:list（admin 角色已含）
// 调用方: RepoAssignPage（题库分配管理页，选择学员）
export async function listUser(params) {
  return request.get('/user/list', { params });
}

// ============================================================
// 获取可注册的角色列表
// ============================================================
// 后端接口: GET /api/public/listRegisterRole
// 返回: { code: 200, data: [{ id, name, code, ... }] }
// 用途: 注册页面展示可选的用户角色（当前前端注册入口尚未实现，接口预留）
export async function getRegisterRoles() {
  return request.get('/public/listRegisterRole');
}
