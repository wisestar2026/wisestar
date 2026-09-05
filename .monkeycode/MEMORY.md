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
- Date: 2026-08-09
- Context: Agent 在开发学生端各页面时发现，早期记录中的相对路径易与实际混淆
- Category: 环境配置
- Instructions:
  - 本仓库实际位于 `/workspace/wisestar`：前端 `wisestar-client/`、后端 `server/api`（Java 8，mvn 构建）、文档在 `docs/`；工作区根目录 `/workspace` 下的 wisestar.mv.db 等是数据库文件，勿当项目根
  - 学生端纯前端原型全部 mock 数据集中在 `src/stores/useStudentStore.js`，新页面视觉必须延续海洋童趣风格（浅蓝渐变+波浪+3D 圆角卡片），公共样式在 `src/pages/student/student.css`

[前端构建与预览]
- Date: 2026-08-09
- Context: Agent 在执行学生端页面开发任务时确认
- Category: 构建方法
- Instructions:
  - 前端构建验证：`cd /workspace/wisestar/wisestar-client && npm run build`；dev 服务 `npm run dev`（vite，3000 端口，代理 /api → 1991）
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
  - 工具链：系统已装 openjdk-17（/usr/lib/jvm/java-17-openjdk-amd64）+ Maven 3.8.7 + Node 22（npm 官方源）；最新开发基线为分支 `260810-feat-knowledge-mgmt-backend`（领先 main 61 提交）
  - 后端构建：`cd /workspace/wisestar/server && mvn clean package -pl api -am -DskipTests`，产物 `api/target/wisestar-v1.9.0.jar`
  - 云端运行：在 `server/api` 目录 `java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`（preview=H2 免 MySQL，库文件生成于 server/api/wisestar.mv.db，已被 .gitignore 忽略；dev/pro 需 MySQL 8 root/root 库 wisestar）
  - 后端日志由 logback 写 `server/api/logs/{info,error}/` 文件（控制台仅 banner），排查先看 `ss -tlnp | grep 1991` 与 error 日志
  - 前端云端启动前必须建 `wisestar-client/.env.local` 写入 `API_TARGET=http://localhost:1991`（vite 默认 7007 是本地 dev 后端端口）；dev server 3000，代理保留 /api 前缀
  - 默认管理员账号 admin/123456（登录走 RSA PKCS1v15 加密密码，前端 jsencrypt 实现）
  - 数据库/接口/方法索引见 docs/项目词典.md；开发背景/踩坑见 docs/开发维护日志.md
