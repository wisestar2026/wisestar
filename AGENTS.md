# AGENTS.md

本文件面向在此仓库工作的 AI/自动化编码代理，给出必须遵守的构建、运行、数据库与提交约定。人类开发者请先读 `README.md` 与 `docs/开发规范.md`。

## 项目概览

wisestar（智慧星 AI 自习室）由开源项目 SurveyKing 改造而来，后端 Spring Boot 多模块（Maven / JDK 17），前端 React 19 + antd 6 + Vite。仓库统一在 `main` 分支开发，不新建分支。

## 模块与依赖方向

- `server/api`：唯一启动入口（`SurveyServerApplication`），Controller 层，端口 1991（preview）/ 7007（dev）。
- `server/shared`：统一响应/分页/异常/鉴权/常量/DTO/服务接口。
- `server/rdbms`：实体、MyBatis-Plus Mapper、Service 实现、数据库种子脚本。
- `server/ai`：AI 对话能力。
- `wisestar-client`：前端 SPA，开发端口 3000，`/api` 反向代理到后端。

依赖方向固定为 `api → rdbms → shared` 与 `api → ai`，禁止反向或跨层直连。

## 构建与运行

```bash
# 后端构建（必须 clean，防止旧 jar 残留）
cd server && mvn clean package -pl api -am -DskipTests

# 后端启动（preview，内置 H2，库缺失时自动恢复快照）
cd server && ./start-preview.sh

# 前端
cd wisestar-client && npm ci && npm run dev
```

- 默认账号：管理员 `admin / 123456`，学员 `a000001 / 123456`。
- 前端 `wisestar-client/.env.local`（gitignore）可设置 `API_TARGET`。
- 预览环境只有一个对外端口（前端 3000）。

## 数据库约定

- 两套幂等种子脚本必须同步修改：`server/rdbms/src/main/resources/scripts/init-h2.sql` 与 `init-mysql.sql`。
- 新增表用 `CREATE TABLE IF NOT EXISTS`；新增列用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS`；内置权限用 `INSERT ... WHERE NOT EXISTS` + 角色 `UPDATE` 收敛。
- 业务表统一审计列：`id / create_at / create_by / update_at / update_by / is_deleted`，实体继承 `BaseModel`。
- 新增权限点必须同时在 `shared/.../constant/PermissionConsts.java` 与两份脚本登记。
- H2 快照流程：停止后端 → `server/db-export.sh` → 提交 `server/db-snapshot/wisestar.sql`。H2 文件库为单进程独占，禁止启用 `AUTO_SERVER`。

## 代码约定（详见 docs/开发规范.md）

- 统一响应 `{code, data, message}`（成功 code=200）；分页 `{total, list, current, pageSize}`。
- Controller 只做校验与编排，业务写在 `ServiceImpl`，`Request → Model → View` 转换走 MapStruct。
- 鉴权：功能级 `@PreAuthorize("hasAuthority('模块:动作')")`；数据级 `@EnableDataPerm` / `CampusScopeService`。
- 日志统一用 `@Slf4j` 的 `log`，禁止 `System.out` 与 `printStackTrace`。
- 前端所有 HTTP 调用经 `src/api/request.js`，响应已由拦截器解包。

## 提交约定

- 只在 `main`；提交前逐文件 `git add <file>`（禁止 `git add -A` / `git add .`）。
- 提交信息格式：`<type>(<scope>): <中文描述>`，type ∈ `feat|fix|chore|refactor|style|docs|test`。
- `commit` 与 `push` 前需与用户确认；push 不带 `-o merge_request.*`。

## 会话收尾与交接

- 每次会话结束前（或交接给下一次对话前），按 `docs/会话收尾与交接清单.md` 执行：核对文档最新 → 构建/lint/提交/push 使主线最新 → 停后端导出 H2 快照并提交 → 重启预览自检。
- 数据库跨会话迁移依赖该清单第 3 步：`server/db-export.sh` 导出 `server/db-snapshot/wisestar.sql` 并提交，新环境由 `server/start-preview.sh` 自动恢复。

## 文档

- 规范：`docs/开发规范.md`
- 现状：`docs/项目结构梳理.md`、`docs/项目词典*.md`
- 过程记录：`docs/开发维护日志.md`；文档索引：`docs/README.md`
- 收尾与交接：`docs/会话收尾与交接清单.md`
- 变更结构/接口/权限后，同步更新对应词典与日志。
