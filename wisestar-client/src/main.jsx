/**
 * main.jsx - 应用入口文件
 *
 * 职责:
 *   1. 将 App 根组件挂载到 HTML 中的 <div id="root">
 *   2. 使用 React.StrictMode 开发模式（检测潜在问题，生产环境无性能影响）
 *   3. 引入全局样式 index.css
 *
 * 这是整个 React 应用的起点，浏览器加载 index.html 后执行此文件。
 *
 * 技术栈依赖链:
 *   index.html → main.jsx → App.jsx → Router → AuthGuard → MainLayout → 各页面组件
 *
 * 挂载流程:
 *   1. 浏览器解析 index.html（<div id="root"> + /src/main.jsx 入口脚本）
 *   2. ReactDOM.createRoot(rootElement) 创建 React 18 并发模式根节点
 *   3. StrictMode 包裹 App（开发模式下双重渲染，用于暴露副作用问题）
 *   4. App 内部: ConfigProvider(中文) → BrowserRouter → 路由表
 */

import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';

// 获取 HTML 中的根节点 <div id="root">
const rootElement = document.getElementById('root');

// React 18 的新 API：createRoot 替代 ReactDOM.render
ReactDOM.createRoot(rootElement).render(
  // StrictMode: 开发模式下会额外检测副作用和已弃用的 API（生产环境不生效）
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
