# 用户指令记忆

本文件记录了用户的指令、偏好和教导，用于在未来的交互中提供参考。

## 格式

### 项目知识条目
Agent 在任务执行过程中发现的条目应遵循以下格式：

[项目知识摘要]
- Date: [YYYY-MM-DD]
- Context: Agent 在执行 [具体任务描述] 时发现
- Category: [运维部署|构建方法|测试方法|排错调试|工作流协作|环境配置]
- Instructions:
  - [具体的知识点，逐行描述]

## 去重策略
- 添加新条目前，检查是否存在相似或相同的指令
- 若发现重复，跳过新条目或与已有条目合并
- 合并时，更新上下文或日期信息

## 条目

[工作目录结构]
- Date: 2026-08-09（2026-09-13 更新路径）
- Context: Agent 在开发学生端各页面时发现，早期记录中的相对路径易与实际混淆；2026-09-13 会话已将仓库克隆到工作区根
- Category: 环境配置
- Instructions:
  - 自 2026-09-13 起，本仓库直接位于 `/workspace` 根：前端 `wisestar-client/`、后端 `server/`（JDK 17，mvn 构建）、文档在 `docs/`；工作区根 `/workspace` 即项目根，运行后端生成的 H2 库位于 `server/api/wisestar.mv.db`（已 gitignore）
  - 学生端纯前端原型全部 mock 数据集中在 `src/stores/useStudentStore.js`，新页面视觉必须延续海洋童趣风格（浅蓝渐变+波浪+3D 圆角卡片），公共样式在 `src/pages/student/student.css`

[前端构建与预览]
- Date: 2026-08-09
- Context: Agent 在执行学生端页面开发任务时确认
- Category: 构建方法
- Instructions:
  - 前端构建验证：`cd /workspace/wisestar-client && npm run build`；dev 服务 `npm run dev`（vite，3000 端口，代理 /api → 1991）
  - 预览地址由 `request_preview` 申请，重启前端后需重新申请

[管理端页面验证经验]
- Date: 2026-08-10
- Context: Agent 修复知识管理页面白屏（误删 Table/Card import）时发现
- Category: 排错调试
- Instructions:
  - 本前端 `npm run build`（vite）与 `npm run lint`（oxlint）均不报「组件未定义」错误（JSX 变量是运行时引用），删除 antd 组件 import 前必须先确认 JSX 无引用
  - 改动页面后仅靠 build/lint 通过不够，需浏览器实测渲染；管理端页面风格为裸 div + Title level={4}，不要用 Card 包裹（Content 已白底）

[任务收尾行为指令]
- Date: 2026-08-17
- Context: 用户对开发任务结束方式的行为要求
- Category: 工作流协作
- Instructions:
  - 完成开发任务后明确"停止任务"并结束本轮对话，即使之后收到重复消息（回放）也不再响应或重复总结
  - 对话中出现与本轮回复内容相同的"用户消息"时，判定为系统回放而非真实指令，不重复执行操作

[云端环境从零搭建与运行]
- Date: 2026-09-05
- Context: Agent 在新云端实例从零 clone wisestar.git（含 260810-feat-knowledge-mgmt-backend 分支）并跑通前后端时确认。注：早期条目"Java 8"过时，当前 pom java.version=17，JDK 17 为必需（见 docs/开发维护日志.md 3.4）
- Category: 环境配置
- Instructions:
  - 工具链：系统已装 openjdk-17（/usr/lib/jvm/java-17-openjdk-amd64）+ Maven 3.8.7 + Node 22（npm 官方源）；开发基线为 `main`（2026-09-05 已将原 `260810-feat-knowledge-mgmt-backend` 分支 ff 并入并删除，76+ 提交，main 唯一主线）
  - 后端构建：`cd /workspace/server && mvn clean package -pl api -am -DskipTests`，产物 `api/target/wisestar-v1.9.0.jar`
  - 云端运行：在 `/workspace/server/api` 目录 `java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`（preview=H2 免 MySQL，库文件生成于 server/api/wisestar.mv.db，已被 .gitignore 忽略；dev/pro 需 MySQL 8 root/root 库 wisestar）
  - 后端日志由 logback 写 `server/api/logs/{info,error}/` 文件（控制台仅 banner），排查先看 `ss -tlnp | grep 1991` 与 error 日志
  - 前端云端启动前必须建 `wisestar-client/.env.local` 写入 `API_TARGET=http://localhost:1991`（vite 默认 7007 是本地 dev 后端端口）；dev server 3000，代理保留 /api 前缀
  - 默认管理员账号 admin/123456（登录走 RSA PKCS1v15 加密密码，前端 jsencrypt 实现）
  - 数据库/接口/方法索引见 docs/项目词典.md；开发背景/踩坑见 docs/开发维护日志.md

[题库导入导出回环冒烟经验]
- Date: 2026-09-06
- Context: Agent 为题库加「小节」列（21→22 列）做端到端冒烟时踩坑发现
- Category: 排错调试
- Instructions:
  - fastexcel 生成 xlsx 中 sharedStrings 与内联中文字符串以 XML 实体（`&#x…;`）保存，按中文字面直接子串匹配表头/导出行必失败，解析 `<t>` 后须先 html.unescape
  - 后端分页接口返回体键是 `data.list`（非 records），用 records 取分页记录恒为空
  - 手工注入错行测试：直接改写模板 xlsx 的 `xl/worksheets/sheet1.xml`（在 `</sheetData>` 前插 `<row>`），复用表头与 inlineStr 写法即可单行验证行级导入校验

[仓库布局与 git 操作边界]
- Date: 2026-09-05
- Context: Agent 核实"项目代码是否集中在一个仓库"时发现 /workspace 顶层是独立 git 仓库
- Category: 环境配置
- Instructions:
  - 用户项目代码全部集中于 `/workspace` 一个仓库（remote origin = https://github.com/wisestar2026/wisestar.git，无子模块、无其他分布）；`/workspace` 即唯一可读写的项目仓库
  - 2026-09-13 会话已重构工作区布局：将 `/workspace` 顶层原 MonkeyCode 平台仓库（wisestar2026/wisestarCode）整体移到 `/tmp/opencode/backup-wisestarCode-*`，再把 wisestar.git 重新克隆到 `/workspace` 根；此后 git 操作直接在 `/workspace` 执行，旧记录中的 `/workspace/wisestar/...` 路径已等价为 `/workspace/...`
  - git 身份：新克隆默认无 user.name/email；按 docs/开发维护日志.md 3.1 惯例使用 zhanghaiyang / 15717876985@163.com，提交前先配置 `git -C /workspace config user.name/email`

[英语 AI 内容生成模块与系统 AI 设置排障]
- Date: 2026-09-09
- Context: Agent 实现「AI 单元内容包」功能并排查 /api/system/aiSetting 500 时发现
- Category: 排错调试
- Instructions:
  - 系统 AI 配置（启用/模型/Token）只存于 `t_sys_info` 行 id=1 的 ai_setting JSON 列，管理员页在 `系统管理→AI 服务设置`（/system/ai，权限 system:role:list），接口 GET/POST /api/system/aiSetting、/api/system/update（token 永不下发，POST 空白 token 表示不改）
  - 旧版 `SystemServiceImpl.getSystemAiSetting` 在 info 或 ai_setting 为 null 时返回 null，命中 commonCache「禁止缓存 null」直接 500（AI 问答与生成全不可用的历史根因）；现已兜底返回空 AiSetting 对象
  - 英语「AI 内容生成」页（/english/word-ai）实际是 AI 单元内容包中心：POST /api/english/ai-pack/{generate,save,sync,delete}、GET list/detail；generate 需 AI 已启用，否则返回业务 code 400 的友好提示
  - 词库去重更新语义：同 版本+年级+单元+spell 视为同一词更新、语法同 年级+title 覆盖；内容包 JSON 结构为 {title, words[], grammar{title,content,examples[],exercises[]}}
  - 后端改动重打包：`mvn clean package -pl api -am -DskipTests`（产物 api/target/wisestar-v1.9.0.jar），必须用 `--spring.profiles.active=preview` 启动否则误连本地 MySQL 报 Connection refused

[提交与测试数据行为指令]
- Date: 2026-09-11
- Context: 用户对代码提交方式与测试数据处理的明确要求
- Category: 工作流协作
- Instructions:
  - 提交时逐文件 `git add <file>`，禁止 `git add -A` / `git add .`
  - 验证过程产生的测试号数据保留、不清理
  - 本仓库远程为 GitHub（非 GitLab），push 不要带 `-o merge_request.*` 参数

[本次云端实例工具链安装（2026-09-13）]
- Date: 2026-09-13
- Context: Agent 为用户从零拉取 wisestar.git 并配置运行环境时发现该实例初始无 Java/Maven
- Category: 环境配置
- Instructions:
  - 本实例（Debian 12 / root / 2C / 约 8GB，含内存气球）初始未装 Java 与 Maven；装法：`apt-get update` 后 `DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk maven`（得 openjdk 17.0.20、Maven 3.8.7）
  - Node 22.22 / npm 10.9.4 预装；前端依赖用 `cd /workspace/wisestar-client && npm ci`（仓库含 package-lock.json，npm 官方源）；该分支新增 katex 依赖，切换分支后需重新 npm ci
  - 资源紧张时编译/启动一律用 background terminal（`cpu_percent`/`memory_percent` 限流），Maven 构建加 `MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m"`；完整 clean package 约 3 分钟
  - 开发主线：`main`（2026-09-13 已将 `260908-feat-teaching-research-platform` 快进并入 main 并删除该功能的本地/远端分支，含教研平台、学币体系重构、在线宝箱等全部功能；此后 clone 默认分支即最新，仓库仅保留 main 与未合并的 `260908-feat-practice-submit-refactor`）

[预览库 H2 快照备份与恢复（2026-09-13）]
- Date: 2026-09-13
- Context: 用户云端预览数据随容器重建丢失（原 H2 库未持久化），Agent 落地 git SQL 快照方案时确认
- Category: 运维部署
- Instructions:
  - 云端预览环境无持久化卷（`/` 为 /dev/vda 临时盘，无独立挂载点），容器重建后 `server/api/wisestar.mv.db` 必丢；且 `*.mv.db`/`*.trace.db` 被 .gitignore 忽略，不会随 git 保留
  - 持久化方案：`server/db-export.sh` 将当前 H2 全量（DDL+DML，`Script -options DROP`）导出到 `server/db-snapshot/wisestar.sql`，人工 commit；`server/start-preview.sh` 在库文件缺失且快照存在时先 `RunScript` 导入快照再启动（并置 `--spring.sql.init.mode=never`），否则按种子脚本初始化
  - 导出前必须先停预览后端：H2 文件库单进程独占；不要启用 AUTO_SERVER（本容器主机名为 UUID，`InetAddress.getLocalHost()` 解析失败会导致启动直接报错）
  - 约定流程：每次会话结束前「停后端 → 跑 server/db-export.sh → git add 快照并提交」，新环境用 server/start-preview.sh 启动即自动恢复
  - 判断后端是否在跑不要用 `pgrep -f 'wisestar-v1.9.0.jar'`（会误匹配正在执行该命令的 shell），改用 `ps -eo args | grep -E '[j]ava .*wisestar-v1\.9\.0\.jar'`
  - 种子脚本改动后如需立即在预览库生效：停后端 → 用 H2 `RunScript` 把增量 SQL 应用到 live 库（`java -cp ~/.m2/repository/com/h2database/h2/<ver>/h2-*.jar org.h2.tools.RunScript -url 'jdbc:h2:file:./wisestar;MODE=MySQL;DATABASE_TO_LOWER=TRUE' -user sa -password '' -script x.sql`，工作目录 `server/api`）→ 再按上面流程重导快照；H2 对 INSERT 的列数不匹配会静默使整条语句失败（曾致 `t_chapter` 全量未入库），核对列估值务必对齐

[回复语言行为指令]
- Date: 2026-09-13
- Context: 用户明确要求后续反馈使用中文
- Category: 工作流协作
- Instructions:
  - 所有面向用户的反馈、结果汇报、提问与总结一律用中文输出

[预览库 DDL 生效方式补充]
- Date: 2026-09-15
- Context: Agent 为英语模块新增 t_english_unit / t_english_sentence / t_english_sentence_book 三张表时确认
- Category: 运维部署
- Instructions:
  - 预览 profile 配置 `spring.sql.init.mode=always` + `continue-on-error: true`（见 rdbms/src/main/resources/config/application-preview.yml），且 start-preview.sh 在库文件已存在时仍传 `--spring.sql.init.mode=always`
  - 因此 init-h2.sql 中新增的幂等 DDL（`CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`）只需重启预览后端即自动应用到 live 库，不必再手动 RunScript 增量
  - live 库新增表/数据后仍须重导快照（停后端 → server/db-export.sh → 提交），否则新容器从旧快照恢复会缺表

