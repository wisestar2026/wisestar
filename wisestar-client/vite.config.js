/**
 * vite.config.js - Vite 构建工具配置文件
 *
 * 配置项说明:
 *   plugins      - Vite 插件列表（@vitejs/plugin-react 提供 React JSX 支持和 HMR 热更新）
 *   server.port  - 开发服务器端口（3000）
 *   server.allowedHosts - 允许访问的主机名白名单
 *   server.proxy - API 代理配置（/api/* → 后端 1991）
 *   build        - 产物拆分策略（见下方 manualChunks 注释）
 *   preview      - 生产预览服务；自带 gzip 与长期缓存响应头，低配电脑访问更快
 *
 * 代理转发示例:
 *   前端请求:  http://localhost:3000/api/system
 *                ↓ Vite 代理
 *   后端接收:  http://localhost:1991/api/system
 */

import { existsSync, statSync, createReadStream } from 'node:fs';
import { join, resolve } from 'node:path';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

/**
 * 预览服务插件：在 Vite 内部静态中间件之前挂上压缩/缓存中间件。
 * 注意 Vite 5 只认插件里的 `configurePreviewServer` 钩子，写在 preview 配置里无效。
 */
function previewPrecompressed() {
  return {
    name: 'wisestar:preview-precompressed',
    configurePreviewServer(server) {
      server.middlewares.use(staticCompression());
    },
  };
}

// 静态资源 MIME 表：preview 中间件直接回文件时要带上正确的 Content-Type
const MIME = {
  '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.webp': 'image/webp',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
};

/**
 * 预览服务的压缩与缓存中间件。
 *
 * 低配电脑（如 i7-2620M 双核）主要卡在「下载 + 解析 2.5MB JS」，
 * 这里做两件事：
 *   1. 命中 dist 下预压好的 .gz 时直接回 gzip（构建脚本预先压好，零运行时 CPU 开销）
 *   2. 带 hash 的静态资源加 `immutable` 长缓存，二次访问只校验 index.html
 * 只处理静态文件，不碰 /api 代理。
 */
function staticCompression() {
  const distDir = resolve(process.cwd(), 'dist');
  return (req, res, next) => {
    const rawUrl = (req.url || '').split('?')[0];
    const ext = rawUrl.slice(rawUrl.lastIndexOf('.'));
    if (!MIME[ext]) {
      return next();
    }
    // 防路径穿越：解析后必须仍在 dist 内
    const file = resolve(join(distDir, rawUrl));
    if (!file.startsWith(distDir) || !existsSync(file)) {
      return next();
    }
    const gzFile = `${file}.gz`;
    const accept = String(req.headers['accept-encoding'] || '');
    const useGzip = accept.includes('gzip') && existsSync(gzFile);
    const target = useGzip ? gzFile : file;

    // 直接 writeHead，避免被后面的静态中间件覆盖成 no-cache
    res.writeHead(200, {
      'Content-Type': MIME[ext],
      'Content-Length': statSync(target).size,
      // 文件名带内容哈希，改动即换名，可以放心长缓存
      'Cache-Control': 'public, max-age=31536000, immutable',
      ...(useGzip ? { 'Content-Encoding': 'gzip', Vary: 'Accept-Encoding' } : {}),
    });
    if (req.method === 'HEAD') {
      res.end();
      return;
    }
    createReadStream(target).pipe(res);
  };
}

export default defineConfig(({ mode }) => {
  // 加载环境变量（.env / .env.local），用于覆盖 API 代理目标
  const env = loadEnv(mode, process.cwd(), '');

  return {
    // React 插件：提供 JSX 编译和 HMR（热模块替换）
    plugins: [react(), previewPrecompressed()],

    // 开发服务器配置
    server: {
      // 监听端口
      port: 3000,

      // 端口被占用时直接失败，而不是自动漂移到 3001/3002（避免预览域名与实际端口错位）
      strictPort: true,

      // 监听所有网络接口（0.0.0.0），确保外部预览代理可以连接
      host: '0.0.0.0',

      // 允许访问的主机名（安全策略：只允许指定域名访问，防止 DNS 重绑定攻击）
      allowedHosts: ['3000-a81c0562a5b99c55.monkeycode-ai.online', '.monkeycode-ai.online'],

      // API 反向代理
      proxy: {
        // 匹配所有 /api 开头的请求
        '/api': {
          // 转发目标：后端 Spring Boot 服务地址
          // 默认 localhost:1991（后端 preview 服务），可通过 .env.local 中 API_TARGET 覆盖
          target: env.API_TARGET || 'http://localhost:1991',
          // 修改请求头中的 Host 为目标地址（避免后端校验 Host 失败）
          changeOrigin: true,
          // 注意: 不设置 rewrite，保留 /api 前缀，后端接口路径本身就是 /api/xxx
        },
      },
    },

    // 生产构建：拆包，避免一个 2.4MB 大包让低配电脑一次性解析
    build: {
      // 单 chunk 超过 400KB 时给个提醒，便于发现回退
      chunkSizeWarningLimit: 400,
      rollupOptions: {
        output: {
          /**
           * 手动分包策略：
           *   vendor-react / vendor-router / vendor-antd / vendor-icons —— 版本稳定，长缓存命中率高
           *   vendor-katex / vendor-crypto —— 只有公式页、登录动作才需要
           * 业务页面由 App.jsx 的 React.lazy 自动切成按路由的小 chunk。
           */
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined;
            }
            if (id.includes('react-router')) {
              return 'vendor-router';
            }
            if (id.includes('/react/') || id.includes('react-dom') || id.includes('/scheduler/')) {
              return 'vendor-react';
            }
            // antd 及其 rc-* 依赖不强行聚块：交给 Rollup 按引用关系拆，
            // 只有首屏真正用到的组件进初始包，其余跟随各自路由 chunk 按需加载
            if (id.includes('katex')) {
              return 'vendor-katex';
            }
            if (id.includes('jsencrypt')) {
              return 'vendor-crypto';
            }
            return undefined;
          },
        },
      },
    },

    // 生产构建预览服务：用单个打包产物替代开发期的大量模块请求，适合公网预览
    preview: {
      port: 3000,
      strictPort: true,
      host: '0.0.0.0',
      allowedHosts: ['.monkeycode-ai.online'],
      proxy: {
        '/api': {
          target: env.API_TARGET || 'http://localhost:1991',
          changeOrigin: true,
        },
      },
    },
  };
});
