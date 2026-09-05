# 会话上下文摘要（压缩版，供后续任务快速参考，减少重新探索）

> 更新：2026-09-05 ｜ 用途：替代冗长对话历史，后续任务先读此文件 + docs/开发维护日志.md

## 0. 最近进展（2026-09-05：云端复现修复 + 全仓库整合收尾）

**整合已完成：main 为唯一主线（76 提交，root 762e10d → 429139f），功能分支已删。**

- 三段收尾提交（已推送 origin/main）：
  - `93c4a50` fix: 知识管理/督学/英语模块云端编译与运行缺陷（SurveySchema 类型/StudentTask::getCreateTime/EnglishWordApi 重复映射/英语表审计列/StudentRecord 对齐 DDL/student:supervision 权限点注册）
  - `6f72426` docs: 会话上下文与项目记忆
  - `429139f` chore: 清理上游 SurveyKing 残留（website/client/image/.gitee/根 application.properties/migration SQL/scripts H2 库）并重写根 README
- git 身份（仓库级）：zhanghaiyang / 15717876985@163.com（docs 日志 3.1 惯例；云端 git 全局无身份）
- **云端 push 偶发异常**：403 wisestarCode / non-fast-forward 为瞬态（GitHub 重定向/TLS），重试即可；origin=wisestar.git

- 云端用 openjdk-17 + Maven 3.8.7 构建；分支含 3 处编译/映射缺陷已修复（见 SESSION 会话记录）：
  - ① rdbms：SurveySchema 类型/取 answer 列；StudentTask::getCreateTime 修正
  - ② api：EnglishWordApi 重复 /record 映射删除（学生端重构版为 /study /record 权威）
  - ③ 英语表 H2 缺 BaseModel 审计列 → h2/mysql 种子补列 + admin 补 english:* 权限点
- 本次新增修复（已随 fat jar wisestar-v1.9.0.jar 重新构建验证）：
  - `student:supervision` 权限点从未注册给任何角色 → 常量 PermissionConsts（权限点常量 + ADMIN_AUTHORITY + 权限树"督学"叶子）+ h2/mysql 种子 admin 收敛同步补齐
  - StudentRecord 实体字段 createTime/updateTime/deleted 与 DDL create_at/update_at/is_deleted 不一致 → 实体对齐 DDL
- 复测全 200：login / supervision/online-students / english word-manager create+list / subject create
- 后端预览运行中：终端 term_1788580550762_20（1991，preview profile，H2 文件库 wisestar.mv.db 在 server/api 目录，已 gitignore）
- 构建注意：**api fat jar 增量 package 不重写 jar**（maven-jar-plugin up-to-date 跳过 + repackage 保留），必须 `mvn clean package -pl api -am -DskipTests`


## 1. 环境与启动

- 项目：`/workspace/wisestar`；前端 `wisestar-client`（vite，3000 端口，proxy `/api`→1991）；后端 `server`（Spring Boot 2.7.7，**Java 8**，preview profile 用 H2 文件库，端口 1991）
- 启动后端：`cd server/api && java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`（后台终端管理）
- 启动前端：`cd wisestar-client && npm run dev`
- 构建：后端 `cd server && mvn clean package -DskipTests`（**必须 clean**，防 ~/.m2 旧 jar）；前端 `cd wisestar-client && npm run lint / build`
- 登录：admin/123456（RSA 加密后 POST /api/public/login，Cookie sk-token 持久 7 天；JWT 密钥固定 wisestar.jwt.secret）

## 2. 关键约定

- 响应格式 `{code, data, message}`；分页 `PaginationResponse{total, list}`，**前端取 `res.data.list`**
- 全局异常包装：越权 403 / 未登录 401 返回 **HTTP 200 + body.code**（测试断言解析 body.code）
- **node fetch 手工 Cookie 头不可靠**（undici 不携带）——接口验证用 curl cookie jar
- 权限：`t_role.authority`（逗号分隔权限点）+ `builtin`（内置不可删）；后端 `@PreAuthorize hasAuthority('module:action')`；前端菜单 `required` 过滤 + AuthGuard 路由校验 + `usePermission` 按钮级
- 数据库：种子脚本 `init-h2.sql`/`init-mysql.sql` 幂等（`CREATE TABLE IF NOT EXISTS` + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` + `INSERT ... WHERE NOT EXISTS` + 内置角色 `UPDATE` 收敛）；新增表需同时改 h2+mysql
- 权限点/内置角色变更：`PermissionConsts`（shared）+ 两个 SQL 种子同步，重启后旧库自动收敛
- 学员端内容接口按订单权限过滤（`t_student_permission` expire_at>NOW）；题目默认剥答案防作弊（studyQuestions `exposeAnswer` 参数控制）

## 3. git

- **主线：`main`（76 提交，root `762e10d` → `429139f`）**；origin = `https://github.com/wisestar2026/wisestar.git`（HTTPS，公网可直连）
- 原功能分支 `260810-feat-knowledge-mgmt-backend` 已 ff 并入 main 并删除（本地+远端）；仓库无子模块
- 开发规范：新功能先在 main 上开 `YYMMDD-feat-xxx` 分支，完成 commit+push 后合回 main（参考 docs/开发维护日志.md 3.1）
- 最近推送：`429139f`（2026-09-05 整合收尾）；工作区干净
- 注意：`.gitignore` 已补 `*.mv.db` 与 `/application.properties`，勿再将 H2 运行库/生成文件入库

## 4. 验证脚本（/tmp/opencode/）

- `test-role-permission.mjs`：回归（18/18，角色/权限树/内置角色）
- `test-api-permission.mjs`：接口级权限
- `test-system-api.mjs`：系统管理 CRUD（25/25）
- `test-knowledge-import.mjs` / `test-study-api.mjs` / `test-student-perm.mjs`（过期）等

## 5. 已完成功能（详见开发维护日志 19-37 节）

角色权限管理、系统管理前端（用户/部门/岗位/字典/条目）、按钮级权限、知识批量导入（名称层级归属+模板下载）、学员端内容对接（study 接口/真实呈现/即时判分）、学员端登录页、底部导航、刷新不登出、练习管理（导入/编辑/组题）、分页 20、学员动态监控、首页统计真实化、积分商城、今日任务（绑定学员/批量/内容展示/完成判定）、研习完成度+星星、试炼/练习逐题（答题指示器/判断题/填空题/提交答案）、判分一致（字母/序号映射）、错题查看+归因、预习开始练习、错题本修复

## 6. 未完成/待办

- 题目难度/题型按练习设置前端下发（接口已支持参数）
- 问卷/答案后端接口与权限点保留（前端已删，可彻底清理）
- 校区业务逻辑（占位）
- 秒级实时（学员动态当前 10 秒轮询，可接 WebSocket）
- 任务完成奖励发放（当前按练习得分聚合学习币/积分）
