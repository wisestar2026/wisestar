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
 *   1. 调用 getSystemInfo() 获取 RSA 公钥
 *   2. 使用 jsencrypt 库用公钥加密密码
 *   3. 将加密后的密码发送给后端 /api/public/login
 *   4. 后端验证后返回 Cookie（sk-token），后续请求自动携带
 */

import request from './request';
import JSEncrypt from 'jsencrypt';

// ============================================================
// 获取系统信息
// ============================================================
// 返回: { code: 200, data: { publicKey: "RSA公钥", name: "系统名称", ... } }
// 用途: 登录前获取 RSA 公钥，用于加密密码
export async function getSystemInfo() {
  return request.get('/system');
}

// ============================================================
// 登录
// ============================================================
// 参数: username - 用户名, password - 明文密码
// 流程:
//   1. 先调用 getSystemInfo() 获取 RSA 公钥
//   2. 用公钥加密密码（密码不会以明文传输）
//   3. 发送 POST /api/public/login，body: { username, password: "加密后的密码" }
// 返回: { code: 200, data: { ... } }
export async function login(username, password) {
  // 步骤1：获取 RSA 公钥
  const systemRes = await getSystemInfo();
  const publicKey = systemRes.data.publicKey;

  // 步骤2：使用 jsencrypt 库进行 RSA 加密
  const encrypt = new JSEncrypt();
  encrypt.setPublicKey(publicKey);
  const encryptedPwd = encrypt.encrypt(password);

  // 加密失败时（如公钥无效）抛出错误
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
// 调用后端登出接口，清除服务端 session
export async function logout() {
  return request.post('/public/logout');
}

// ============================================================
// 注册
// ============================================================
// 参数: data - 注册表单数据 { username, password, ... }
export async function register(data) {
  return request.post('/public/register', data);
}

// ============================================================
// 获取当前登录用户信息
// ============================================================
// 返回: { code: 200, data: { id, name, username, ... } }
// 用途: 页面刷新时恢复登录态，显示用户头像和名称
export async function getCurrentUser() {
  return request.get('/currentUser');
}

// ============================================================
// 获取用户概览统计数据
// ============================================================
// 返回: { code: 200, data: { surveyCount, examCount, userCount, todayAnswerCount } }
// 用途: 仪表盘页面展示统计数据
export async function getUserOverview() {
  return request.get('/userOverview');
}

// ============================================================
// 获取可注册的角色列表
// ============================================================
// 用途: 注册页面展示可选的用户角色
export async function getRegisterRoles() {
  return request.get('/public/listRegisterRole');
}
