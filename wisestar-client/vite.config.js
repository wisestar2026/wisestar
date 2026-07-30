/**
 * vite.config.js - Vite 构建工具配置文件
 *
 * 配置项说明:
 *   plugins      - Vite 插件列表（@vitejs/plugin-react 提供 React JSX 支持和 HMR 热更新）
 *   server.port  - 开发服务器端口（3000）
 *   server.allowedHosts - 允许访问的主机名白名单
 *     - *.monkeycode-ai.online : 通配符，允许 monkeycode-ai.online 所有子域名
 *     - localhost : 本地开发默认允许
 *   server.proxy - API 代理配置
 *     - 将前端 /api/* 请求转发到后端 http://localhost:1991
 *     - 解决前后端分离开发时的跨域问题
 *
 * 代理转发示例:
 *   前端请求:  http://localhost:3000/api/system
 *                ↓ Vite 代理
 *   后端接收:  http://localhost:1991/api/system
 *
 * 注: 生产环境不使用 Vite dev server，由 nginx 等反向代理处理
 */

import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  // React 插件：提供 JSX 编译和 HMR（热模块替换）
  plugins: [react()],

  // 开发服务器配置
  server: {
    // 监听端口
    port: 3000,

    // 监听所有网络接口（0.0.0.0），确保外部预览代理可以连接
    host: '0.0.0.0',

    // 允许访问的主机名（安全策略：只允许指定域名访问，防止 DNS 重绑定攻击）
    allowedHosts: ['3000-a81c0562a5b99c55.monkeycode-ai.online', '.monkeycode-ai.online'],

    // API 反向代理
    proxy: {
      // 匹配所有 /api 开头的请求
      '/api': {
        // 转发目标：后端 Spring Boot 服务地址
        target: 'http://localhost:7007',
        // 修改请求头中的 Host 为目标地址（避免后端校验 Host 失败）
        changeOrigin: true,
        // 注意: 不设置 rewrite，保留 /api 前缀，后端接口路径本身就是 /api/xxx
      },
    },
  },
});
