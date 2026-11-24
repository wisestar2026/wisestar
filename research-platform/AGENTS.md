# research-platform AGENTS.md

本文件面向在此目录工作的 AI/自动化编码代理。改动前先读根目录 `AGENTS.md` 与 `.monkeycode/specs/2026-11-23-teaching-research-platform-v2/` 下的需求与设计。

## 定位

教研平台是独立部署的 Web 系统，与 AI 自习室（`server/` + `wisestar-client/`）代码、进程、数据库分离。两套系统只通过只读接口通信：

- 本系统调用 AI 自习室：`OPEN_RESEARCH_BASE_URL` + `X-Research-Token`
- 契约唯一真源：`contract/open-research-v1.yaml`
- 禁止跨库 JOIN、直连对方数据库、反向写入

## 技术栈与端口

- 后端：Spring Boot 2.7.7 / JDK 17 / MyBatis-Plus 3.5.0 / H2（预览）或 MySQL 8（生产），包名 `cn.wisestar.research`，端口 7010，context-path `/api`
- 前端：React 19 / antd 6 / Vite 5 / zustand，端口 3100，开发时 `/api` 反代到 7010
- 生产路径：`VITE_BASE_PATH=/research/`、API `/research/api`、Cookie `research-token` 且 `Path=/research`

## 构建与启动

```bash
cd research-platform/server
mvn clean package -DskipTests
./start-preview.sh

cd research-platform/client
npm install
npm run dev
npm run lint
npm run build
```

AI 自习室一侧的改动仍需按其根目录 `AGENTS.md` 构建：`cd server && mvn clean package -pl api -am -DskipTests`。

## 目录职责

- `server/src/main/java/cn/wisestar/research/common`：统一响应 `R`、分页、异常、审计字段与自动填充、MyBatis-Plus 配置
- `auth`：本地管理员、JWT、鉴权过滤器、远端账号校验
- `plan`：班课方案实体、服务、接口、DTO
- `flow`：流程日志（创建/修改/提交/审核/撤回/删除）
- `system`：健康检查
- `server/src/main/resources/db`：H2 与 MySQL 两份幂等脚本，必须同步修改
- `client/src/api`：所有 HTTP 调用统一走 `request.js`（已解包统一响应）

## 业务约定

- 角色三级：`ADMIN`（全部数据）、`SUPERVISOR`（全部数据 + 审核）、`TEACHER`（仅本人创建的数据）
- 方案状态：`DRAFT → PENDING → APPROVED / REJECTED`；撤回 `APPROVED → DRAFT`
- 同一 `semester + subject + grade + classType` 只允许一个已发布方案：发布时写 `published_key='1'`，其余为 NULL，靠唯一索引兜底
- 学员账号不能登录教研平台；远端服务不可用时仅本地管理员可登录
- 审核通过/驳回必须写 `t_research_flow_log`，不要绕过 `FlowLogService`
- 审计列统一 `create_at/create_by/update_at/update_by/is_deleted`，实体继承 `BaseEntity`，不手写时间与操作人

## 部署

- nginx 示例：`deploy/nginx.example.conf`（单域名路径反代）
- 配置全部走环境变量：`DB_URL/DB_USER/DB_PASSWORD`、`OPEN_RESEARCH_BASE_URL`、`RESEARCH_OPEN_TOKEN`、`RESEARCH_JWT_SECRET`、`SERVER_PORT`、`RESEARCH_COOKIE_PATH`
- 生产首次部署：`mysql < server/src/main/resources/db/init-mysql.sql`，然后用 `application-mysql.yml` 启动
- 附件与导出文件以后统一落 `server/data`，单独挂载备份

## 常见坑

- 本系统后端源码按 JDK 17 编译；AI 自习室 `api` 模块仍按 1.8 编译，不要把 record / switch 表达式写进 `server/api`
- `R` 统一响应在 AI 侧由 `CustomResponseBodyAdvice` 自动包装，本系统由 Controller 显式返回 `R`
- JWT 密钥至少 32 字符，生产必须覆盖默认值
- 前端直接访问 `/login` 需要 nginx 的 SPA 兜底，否则刷新 404
- 1991/7010 只在内网监听，`/open/research/**` 不走 nginx；上线前用安全组确认公网访问不到 1991
