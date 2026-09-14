# 会话上下文摘要（压缩版，供后续任务快速参考，减少重新探索）

> 更新：2026-09-13 ｜ 用途：替代冗长对话历史，后续任务先读此文件 + docs/开发维护日志.md
> 本次已把默认分支 `main` 对齐到最新开发线，clone 即得最新进展，无需再手动切分支。

## 0. 最近进展（2026-09-13）

### 工作区与分支整理

- 本会话把 `/workspace` 顶层从误配的 MonkeyCode 平台仓库（wisestar2026/wisestarCode）换成 `wisestar.git` 克隆；原平台内容完整备份在 `/tmp/opencode/backup-wisestarCode-20260913-120558`（未删除，可还原）。
- 主线对齐：`main` 快进（ff）到 `260908-feat-teaching-research-platform` 最新提交（覆盖 2026-09-08 ~ 2026-09-13 全部功能），并删除已并入的功能分支，仓库只保留 `main`。
- 旧分支 `260908-feat-practice-submit-refactor`（2026-09-08，2 提交）**保留在 origin 不动**：其与最新线冲突 11 个文件且功能与最新线的练习/错题本重构重叠，未合并（用户确认保留）。
- 此后 `git clone` / `git pull` 默认分支即最新，不再依赖人工提醒切换分支。

### 预览数据持久化（H2 快照方案）

- 背景：云端预览环境无持久化卷，`server/api/wisestar.mv.db`（H2，已 gitignore）随容器重建必然丢失。
- 方案：`server/db-export.sh` 在后端停止时把 H2 全量（DDL+DML）导出到 `server/db-snapshot/wisestar.sql`（commit 进 git）；`server/start-preview.sh` 在库文件缺失且快照存在时先 `RunScript` 导入快照再启动（置 `--spring.sql.init.mode=never`）。
- 约定流程：**会话结束前「停后端 → 跑 server/db-export.sh → 提交快照」**，新环境用 `server/start-preview.sh` 启动即自动恢复。
- 注意：不要启用 H2 的 AUTO_SERVER（本容器主机名为 UUID，`InetAddress.getLocalHost()` 解析失败会让启动直接报错）。

### 工具链安装

- 本实例初始无 Java/Maven；已装 `openjdk-17-jdk` + Maven 3.8.7（`DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk maven`）。Node 22.22 / npm 10.9.4 预装。

### 功能进展（2026-09-08 ~ 2026-09-13）

- 教研平台、英语 AI 单元内容生成、任务发布、积分·学币账本、薄弱点·学习评价、专项练习/小节通关/预习例题配置、学币体系重构、在线时长宝箱、题目富内容渲染与学员端 3D 黏土图标。详见 `docs/开发维护日志.md` 第 43 节及 `.monkeycode/specs/`。

### 英语独立词库补全（人教版 PEP 四~六年级，2026-09-13）

- 素材：`.monkeycode-tmp-files/` 下《小学英语四至六年级单词与语法汇总.xlsx》（6 sheet = 四/五/六年级 上/下册），解析得 **36 单元 / 740 词**；生成器 `/tmp/opencode/gen_english.py`。
- 承载方式：落到**英语独立词库** `t_english_word` / `t_english_grammar`，**不走**通用知识链路（章节/小节/知识点/题目）。
- 新增 `t_english_word.term`（上册/下册）字段并打通全链路：模型 / View / Query / `EnglishWordService` / `EnglishWordManagerServiceImpl`（含导入 cell9）/ 学生端。学员端 `word-book` 由列表改**分页返回**（`PaginationResponse`）。
- 种子已写入 `init-h2.sql` 与 `init-mysql.sql`（含 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS term`）：740 词 + 36 语法；同时修复 `init-h2.sql` 6 条 `t_chapter` INSERT 列数不匹配 bug（此前使 live 库章节为 0）。
- 已应用到 live H2 并重导快照 `server/db-snapshot/wisestar.sql`：章节 **6**、英语单词 **740**、英语语法 **36**。API 验证：`/english/word/list`（term 筛选 355+385=740）、`/english/word-manager/list`、`/english/word/word-book`（分页）、`/english/word/study` 均 200。
- 单词导入模板新增第 10 列「学期」（cell9）；通用导入模板 5 个在 `/workspace/导入模板/`。

## 1. 环境与启动

- 项目根：`/workspace`（仓库直接位于工作区根）；前端 `wisestar-client/`（Vite 5，端口 3000，proxy `/api`→1991，`strictPort`）；后端 `server/`（Spring Boot 2.7.7 + Undertow，**JDK 17**，preview profile 用 H2 端口 1991；dev profile 用 MySQL 8 端口 7007）。
- 后端构建：`cd /workspace/server && MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m" mvn clean package -pl api -am -DskipTests`，产物 `api/target/wisestar-v1.9.0.jar`（**必须 clean**，增量 package 可能不重写 jar）。
- 启动后端（优先用脚本，可自动恢复快照）：
  - `server/start-preview.sh`
  - 或 `cd server/api && java -Xms256m -Xmx768m -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`
- 启动前端：`cd /workspace/wisestar-client && npm ci && npm run dev`；需 `wisestar-client/.env.local` 写 `API_TARGET=http://localhost:1991`（gitignore）。
- 登录：`admin / 123456`（RSA 加密后 POST `/api/public/login`，Cookie `sk-token` 持久 7 天）。

## 2. 关键约定

- 响应格式 `{code, data, message}`；分页 `PaginationResponse{total, list}`，前端取 `res.data.list`。
- 全局异常包装：越权 403 / 未登录 401 返回 **HTTP 200 + body.code**（测试断言解析 body.code）。
- **node fetch 手工 Cookie 头不可靠**（undici 不携带）——接口验证用 curl cookie jar，或自行管理 `set-cookie`。
- 权限：`t_role.authority`（逗号分隔权限点）+ `builtin`（内置不可删）；后端 `@PreAuthorize hasAuthority('module:action')`；前端菜单 `required` 过滤 + AuthGuard 路由校验 + `usePermission` 按钮级。
- 数据库：种子脚本 `init-h2.sql`/`init-mysql.sql` 幂等（`CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` + `INSERT ... WHERE NOT EXISTS` + 内置角色 `UPDATE` 收敛）；新增表/权限点需同时改 h2 + mysql + `PermissionConsts`。注意 mysql 脚本当前缺 `t_campus`/`t_user_campus`（仅 H2 有）。
- 学员端内容接口按订单权限过滤（`t_student_permission` expire_at > NOW）；题目默认剥答案防作弊（studyQuestions `exposeAnswer` 参数控制）。
- 学员端纯前端页面的视觉必须延续海洋童趣风格（浅蓝渐变 + 波浪 + 3D 圆角卡片），公共样式在 `src/pages/student/student.css`；管理端页面为裸 div + `Title level={4}`，不要用 Card 包裹。
- 练习绑定规则：练习仅支持绑定到小节（章节直绑已停用）；出题统一走 `StudentServiceImpl.studyQuestions`，按用途 `preview/special/trial` 收敛题源。

## 3. git

- **默认/唯一主线：`main`**，origin = `https://github.com/wisestar2026/wisestar.git`（HTTPS，公网可直连）；仓库无子模块。
- 旧分支 `260908-feat-practice-submit-refactor` 保留在 origin（未合并，勿误当最新）。
- 提交规范：逐文件 `git add <file>`，禁止 `git add -A` / `git add .`；本仓库远程为 GitHub，push 不要带 `-o merge_request.*`。
- 开发规范：新功能在 main 上开 `YYMMDD-feat-xxx`，完成 commit+push 后合回 main（详见 docs/开发维护日志.md 3.1）。
- git 身份（仓库级）：`zhanghaiyang / 15717876985@163.com`。
- 数据持久化：H2 快照 `server/db-snapshot/wisestar.sql` 需 commit；`*.mv.db` / `*.trace.db` 已在 `.gitignore`，勿入库。

## 4. 验证脚本（/tmp/opencode/，临时）

- `db-state.mjs`：登录后统计学员/题库/章节/知识点数量
- `student-test.mjs`：`create` 新建测试学员并列表
- 历史：`test-role-permission.mjs` / `test-api-permission.mjs` / `test-system-api.mjs` 等（可能已过期）

## 5. 已完成功能（详见开发维护日志 19-43 节）

角色权限管理、系统管理前端、按钮级权限、知识批量导入、学员端内容对接、练习管理、学员动态监控、首页统计真实化、积分商城、今日任务、研习完成度+星星、试炼/练习逐题、判分一致、错题查看与归因、错题本、教研平台、英语 AI 单元内容生成、任务发布、角色数据范围、学员积分·学币账本、薄弱点·学习评价、专项练习/小节通关/预习例题配置、学币体系重构（单科学期上限 10000、内容每学期一次幂等）、在线时长宝箱、每日签到、题目富内容渲染、学员端 3D 黏土图标、英语独立词库补全（PEP 四~六年级 740 词 + 册别字段）、数学知识结构与题目导入（827 题、含图片标签 281 题）、题目管理按标签筛选、覆盖优先组卷（专项练习/小节通关/章节测评）、教研平台题目 Label 式展示。

## 6. 未完成/待办

- 旧分支 `260908-feat-practice-submit-refactor` 的「专项练习交卷制重构」是否合并主线待定（当前保留）。
- mysql 种子脚本补 `t_campus`/`t_user_campus`（切 MySQL 前必须）。
- 题目难度/题型按练习设置前端下发（接口已支持）。
- 校区业务逻辑（占位）。
- 学员动态当前 10 秒轮询，可接 WebSocket 实现秒级实时。
- 问卷/答案后端接口与权限点保留（前端已删，可彻底清理）。
