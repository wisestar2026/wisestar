# 智慧星教研平台

独立部署的教研工作台，覆盖班课方案、课次备课、真题组卷、资源导航与文档导出。与智慧星 AI 自习室共用账号体系与只读题库接口，代码、进程、数据库完全独立。

## 快速开始

```bash
# 后端（H2 文件库，首次启动自动建表）
cd research-platform/server
mvn clean package -DskipTests
./start-preview.sh

# 前端
cd research-platform/client
npm install
npm run dev
```

- 前端：http://localhost:3100
- 后端：http://localhost:7010/api/health
- 内置管理员：`admin / 123456`（远端服务不可用时也能登录）
- 老师账号：与 AI 自习室同一套账号密码，角色由远端返回的 `admin/principal/teacher/consultant` 映射

## 当前范围（阶段一）

- 独立工程、独立数据库、统一响应/异常/分页/审计
- 登录：本地管理员 + 远端只读校验 + JWT + 三级角色数据范围
- 班课方案：新建、编辑、删除、提交审核、审核发布/驳回、撤回、流程日志
- 同一学期 + 学科 + 年级 + 班级类型只允许一个已发布方案

后续阶段：课次备课与知识点卡片、区域真题入库与考点分析、智能组卷与打印模板、资源导航、教师版/家长版导出。

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SERVER_PORT` | `7010` | 后端端口 |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | H2 文件库 | 生产改用 `--spring.profiles.active=mysql` |
| `OPEN_RESEARCH_BASE_URL` | `http://127.0.0.1:1991` | AI 自习室地址 |
| `RESEARCH_OPEN_TOKEN` | `dev-research-token` | 服务令牌，生产必须覆盖 |
| `RESEARCH_JWT_SECRET` | 开发密钥 | JWT 签名密钥，至少 32 字符 |
| `RESEARCH_COOKIE_PATH` | `/` | 生产设为 `/research` |
| `VITE_BASE_PATH` | `/` | 生产设为 `/research/` |
| `VITE_API_BASE` | 开发 `/api`，生产 `${BASE_PATH}api` | 前端接口前缀 |

## 部署

参考 `deploy/nginx.example.conf`：单域名下教研前端挂 `/research/`、后端挂 `/research/api/`，AI 自习室沿用 `/` 与 `/api/`。远端只读接口 `/open/research/v1/**` 只在回环地址暴露。

## 文档

- 需求：`.monkeycode/specs/2026-11-23-teaching-research-platform-v2/requirements.md`
- 设计：`.monkeycode/specs/2026-11-23-teaching-research-platform-v2/design.md`
- 接口契约：`contract/open-research-v1.yaml`
- 开发约定：`AGENTS.md`
