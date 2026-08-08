/**
 * request.js - Axios 请求实例（全项目唯一的 HTTP 请求出口）
 *
 * 功能说明:
 * 1. 创建统一的 Axios 实例，设置 baseURL 为 /api（通过 Vite 代理转发到后端 localhost:1991）
 * 2. 响应拦截器自动解包后端返回的 { code, data, message } 结构，业务代码只需关心 data
 * 3. 统一处理各种 HTTP 错误状态码（401/403/404/500 等），并弹出 antd message 提示
 * 4. 开启 withCredentials，保证每次请求自动携带后端写入的 sk-token Cookie 维持登录态
 *
 * 后端返回格式约定:
 *   成功: { code: 200, data: {...} }
 *   失败: { code: 非200, message: "错误信息" }
 *
 * 被谁引用:
 *   - src/api/ 下的所有业务 API 文件（answer/project/repo/survey/template/user.js）
 *   - 各业务 API 文件被对应页面/组件引用，例如:
 *     ProjectListPage → api/project.js → request.js → POST /api/project/list
 *     QuestionListPage → api/template.js → request.js → GET /api/template/list
 *   - 注意: 文件上传类接口（api/upload.js、api/repo.js 的 importTemplate）不走本实例，
 *     而是使用原生 axios + multipart/form-data，因为拦截器只解包 JSON。
 *
 * 核心数据流:
 *   页面调用 API 函数 → request.get/post(...) → 后端返回 { code, data } → 拦截器解包
 *   → 成功: 直接 resolve response.data（业务代码取 .data 字段）
 *   → 失败: message.error 提示 + reject 错误对象
 */

import axios from 'axios';
import { message } from 'antd';

// ============================================================
// 创建 Axios 实例
// ============================================================
const request = axios.create({
  // 所有请求都发往 /api 路径，Vite 代理会将 /api 转发到后端 localhost:1991
  baseURL: '/api',
  // 请求超时时间：30 秒
  timeout: 30000,
  // 允许跨域请求携带 Cookie（用于维持后端 session/sk-token）
  withCredentials: true,
});

// ============================================================
// 响应拦截器：统一处理成功和失败
// ============================================================
request.interceptors.response.use(
  // ---- 请求成功（HTTP 2xx）----
  (response) => {
    // 后端标准响应格式 { code: 200, data: {...}, message: "..." }
    if (response.data && response.data.code !== undefined) {
      if (response.data.code === 200) {
        // 成功：直接返回 response.data（调用方通过 .data 获取业务数据）
        return response.data;
      }
      // 业务逻辑错误（如参数校验失败），弹出错误提示
      message.error(response.data.message || '请求失败');
      return Promise.reject(new Error(response.data.message));
    }
    // 非标准格式，直接返回原始数据
    return response.data;
  },

  // ---- 请求失败（HTTP 非 2xx 或网络错误）----
  (error) => {
    const status = error.response?.status;

    // 根据 HTTP 状态码分类处理
    if (status === 401) {
      // 未登录/登录过期 → 跳转到登录页
      message.error('登录已过期，请重新登录');
      window.location.href = '/login';
    } else if (status === 403) {
      // 无权限
      message.error('没有操作权限');
    } else if (status === 404) {
      // 资源不存在
      message.error('请求的资源不存在');
    } else if (status >= 500) {
      // 服务器内部错误
      message.error('服务器错误，请稍后重试');
    } else if (error.message === 'Network Error') {
      // 网络连接失败（后端未启动等）
      message.error('网络连接失败，请检查服务器是否启动');
    }

    // 继续抛出错误，让调用方可以进一步处理
    return Promise.reject(error);
  }
);

export default request;
