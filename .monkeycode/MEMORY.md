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
