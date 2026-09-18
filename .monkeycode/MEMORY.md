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

[工作目录与仓库布局]
- Date: 2026-08-09（2026-09-13 更新）
- Context: Agent 早期开发与核实仓库分布时确认，2026-09-13 会话已将仓库克隆到工作区根
- Category: 环境配置
- Instructions:
  - 自 2026-09-13 起本仓库直接位于 `/workspace` 根（即项目根）：前端 `wisestar-client/`、后端 `server/`（JDK 17，mvn 构建）、文档 `docs/`；后端 H2 库 `server/api/wisestar.mv.db`（已 gitignore）
  - 用户项目代码全部集中于 `/workspace` 一个仓库（origin = https://github.com/wisestar2026/wisestar.git，无子模块）；原顶层 MonkeyCode 平台仓库已移到 `/tmp/opencode/backup-wisestarCode-*`，旧记录 `/workspace/wisestar/...` 已等价为 `/workspace/...`
  - git 身份：仓库级 zhanghaiyang / 15717876985@163.com（提交前先 `git -C /workspace config user.name/email`）
  - 学生端纯前端原型 mock 数据集中在 `src/stores/useStudentStore.js`，新页面视觉必须延续海洋童趣风格（浅蓝渐变+波浪+3D 圆角卡片），公共样式 `src/pages/student/student.css`

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

[云端环境搭建与工具链]
- Date: 2026-09-05（2026-09-13 更新）
- Context: Agent 在新云端实例从零 clone 并跑通前后端；2026-09-13 会话再次确认工具链与资源约束（注：早期"Java 8"记录过时，pom java.version=17，JDK 17 必需）
- Category: 环境配置
- Instructions:
  - 工具链与安装：openjdk-17（/usr/lib/jvm/java-17-openjdk-amd64）+ Maven 3.8.7 + Node 22（npm 官方源）；初始无 Java/Maven 时 `apt-get update` 后 `DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-17-jdk maven`；前端依赖 `cd /workspace/wisestar-client && npm ci`（含 package-lock.json；新增 katex 依赖后需重跑）
  - 资源约束（Debian 12 / root / 2C / 约 8GB 含内存气球）：编译/启动一律用 background terminal 限流，Maven 加 `MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m"`，完整 clean package 约 3 分钟
  - 后端构建：`cd /workspace/server && mvn clean package -pl api -am -DskipTests`，产物 `api/target/wisestar-v1.9.0.jar`
  - 运行：在 `/workspace/server/api` 执行 `java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`（preview=H2 免 MySQL；dev/pro 需 MySQL 8 root/root 库 wisestar）；日志写 `server/api/logs/{info,error}/`（控制台仅 banner），排查先 `ss -tlnp | grep 1991` 与 error 日志
  - 前端云端启动前必须建 `wisestar-client/.env.local` 写 `API_TARGET=http://localhost:1991`（vite 默认 7007 是本地 dev 后端端口）；dev server 3000，代理保留 /api 前缀
  - 默认管理员 admin/123456（RSA PKCS1v15 加密密码，前端 jsencrypt 实现）；数据库/接口/方法索引见 docs/项目词典.md，开发背景见 docs/开发维护日志.md
  - 开发主线：`main`（2026-09-13 已把 `260908-feat-teaching-research-platform` ff 并入并删除，含教研平台/学币重构/在线宝箱；仓库仅保留 main 与未合并的 `260908-feat-practice-submit-refactor`）

[题库导入导出回环冒烟经验]
- Date: 2026-09-06
- Context: Agent 为题库加「小节」列（21→22 列）做端到端冒烟时踩坑发现
- Category: 排错调试
- Instructions:
  - fastexcel 生成 xlsx 中 sharedStrings 与内联中文字符串以 XML 实体（`&#x…;`）保存，按中文字面直接子串匹配表头/导出行必失败，解析 `<t>` 后须先 html.unescape
  - 后端分页接口返回体键是 `data.list`（非 records），用 records 取分页记录恒为空
  - 手工注入错行测试：直接改写模板 xlsx 的 `xl/worksheets/sheet1.xml`（在 `</sheetData>` 前插 `<row>`），复用表头与 inlineStr 写法即可单行验证行级导入校验

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
  - 预览 profile 配置 `spring.sql.init.mode=always` + `continue-on-error: true`（见 rdbms/.../config/application-preview.yml），且 start-preview.sh 在库文件已存在时仍传 `--spring.sql.init.mode=always`；因此 init-h2.sql 中新增的幂等 DDL（`CREATE TABLE IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS`）只需重启预览后端即自动应用到 live 库，不必手动 RunScript 增量；但 live 库新增表/数据后仍须重导快照（停后端 → server/db-export.sh → 提交），否则新容器从旧快照恢复会缺表

[回复语言行为指令]
- Date: 2026-09-13
- Context: 用户明确要求后续反馈使用中文
- Category: 工作流协作
- Instructions:
  - 所有面向用户的反馈、结果汇报、提问与总结一律用中文输出

[架构与文档整理工作流指令]
- Date: 2026-09-17
- Context: 用户要求对整个项目做文档整理、词典更新、结构梳理，并评估 1000~5000 并发的架构稳定性
- Category: 工作流协作
- Instructions:
  - 架构级改动（影响运行时、部署、并发模型、存储/缓存选型）必须先产出说明文档供用户评审，评审通过后再实施；不要直接改代码
  - 整理既有说明文档时优先做「分类索引 + 新建现行文档」，物理移动/归档需先给出方案待确认
  - 项目词典颗粒度要求：细到每个类 + 每个公开方法，含模块/包归属与依赖关系；明细拆分到 `docs/项目词典-*`，总入口为 `docs/项目词典.md`
  - 文档体系与「何时更新哪份文档」的约定见 `docs/README.md`；结构主线见 `docs/项目结构梳理.md`；架构方案见 `docs/架构稳定性评估与改造方案.md`

[字典生成方法（可复用）]
- Date: 2026-09-17
- Context: Agent 为生成「每类每方法」词典编写源码清单脚本时确认
- Category: 构建方法
- Instructions:
  - 后端类/方法清单由 Python 脚本遍历 `server/**/*.java` 提取（模块/包/类型/类级 Javadoc/注入字段/公开方法签名），再转 Markdown；接口清单由 Controller 的 `@RequestMapping` 类路径 + 方法映射 + `@PreAuthorize` 提取
  - 源码类级 Javadoc 普遍包含 HTML（`<p><b><ul><pre>`）与 `{@code}`，转文档时需先做 HTML→Markdown 清洗
  - 词典类文档属于「随代码同步维护」的现行文档，改动相关代码后需同步更新（映射见 `docs/README.md` 第一节）

[题目配图存储与标准模板图片列]
- Date: 2026-09-16
- Context: Agent 将 623 道数学题（含 394 张配图）导入题库、并为导入模板增加「图片」列时确认
- Category: 排错调试
- Instructions:
  - 题目配图存 `attribute.examImages`（List<String> URL 数组），题库列表据此显示「含配图」标记、练习页据此渲染配图
  - 后端 `/api/file/create` 返回的 FileView 实际不含 previewUrl（FileViewMapper 未映射、File 实体也无该字段）；图片直接引用 `/api/file?id=<fileId>`，且 `GET /api/file?id=` 无需登录即可访问，`<img>` 可直接加载
  - 标准单表模板现为 30 列（第 30 列可选「图片」，多张用换行分隔）；解析在 `RepoServiceImpl.parseStandardRow`（COL_IMAGE），导出/模板在 `STANDARD_HEADERS`/`standardRowOf`

