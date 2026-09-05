# Wisestar · 智慧星 AI 自习室系统

面向线下门店 K12 场景的自习室教学与管理系统，由开源项目「卷王 SurveyKing」(v1.10.0) 改造而来。

系统覆盖**教学管理后台**（管理员/教师/校长/学管师）与**学生自主学习端**两大使用面：后台负责题库、知识体系、练习布置、学员督学与数据统计；学生端提供研习（章节/小节/知识点）、试炼与专项练习、错题本、今日任务、学习币与星星成长激励。

## 仓库结构

```
wisestar/
├── server/                  # 后端 Spring Boot 多模块（Maven）
│   ├── api/                 # 启动模块（fat jar 构建入口，端口 1991）
│   ├── rdbms/               # 数据库访问与业务实现（MyBatis-Plus + H2/MySQL 种子）
│   └── shared/              # 共享常量（权限点/内置角色/权限树）与通用工具
├── wisestar-client/         # 学生端 + 管理端前端（React 19 / antd 6 / Vite）
└── docs/                    # 开发维护日志、需求文档、系统文档
```

> 原仓库根部的 `website/`（SurveyKing 官网）、`client/`、上游英文 README 等残留已在整合时移除；版本历史中仍保留。

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | React 19 / antd 6 / zustand / Vite（端口 3000，`/api` 代理到后端 1991） |
| 后端 | Spring Boot 2.7.7 + Undertow，Maven 多模块，JDK 17 |
| 数据库 | 本地开发 MySQL 8（库 `wisestar`）；云端预览 H2 文件库（`server/api/wisestar.mv.db`，preview profile） |
| 主要依赖 | MyBatis-Plus、RSA 登录加密（jsencrypt）、角色-权限点鉴权（`@PreAuthorize`） |

## 快速运行

```bash
# 1) 构建后端（注意必须 clean，防止旧 jar 残留）
cd server && mvn clean package -DskipTests

# 2) 启动后端（云端/本地均可：preview 内置 H2，自动建表播种）
cd server/api && java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview

# 3) 启动前端（另开终端；本地开发走 dev profile + MySQL 时后端端口为 7007）
cd wisestar-client && npm ci && npm run dev
```

默认管理员账号：`admin / 123456`（RSA 加密传输，登录后 Cookie `sk-token` 持久 7 天）。

前端如需代理到非默认后端端口，修改 `wisestar-client/.env.local` 的 `API_TARGET`。

## 运行环境与配置

- **云端（preview profile）**：H2 文件库，种子脚本幂等（`CREATE TABLE IF NOT EXISTS` + `ALTER ... ADD COLUMN IF NOT EXISTS` + 内置角色 `UPDATE` 收敛），重启即自动补齐结构与权限。
- **本地（dev profile）**：MySQL 8，`server/rdbms/src/main/resources/scripts/init-mysql.sql` 为完整种子脚本；H2 版为 `init-h2.sql`。**改动数据库结构或权限时两份脚本需同步修改**。
- 端口：前端 3000，后端 1991（preview）/ 7007（dev）。

## 文档

| 文档 | 内容 |
|------|------|
| `docs/开发维护日志.md` | 改造全过程逐节记录（背景/改动/踩坑/验证），后续开发优先阅读 |
| `docs/AI自习室系统开发路线图.md` | 五大板块蓝图与现状盘点，规划依据 |
| `docs/Wisestar智习系统学生端需求文档V2.0-完善版.md` | 学生端需求规格 |
| `docs/完整操作链路-登录答题评分.md` | 端到端操作链路说明 |
| `docs/项目词典.md` / `docs/源码讲解-问卷创建与管理模块.md` | 术语与源码导读 |
| `.monkeycode/` | AI 协作会话上下文与项目记忆（非产品文档） |

## 来源与许可

本项目由 [SurveyKing](https://github.com/javahuang/surveyking)（javahuang，MIT License）改造衍生；改动记录见 `docs/开发维护日志.md`。保留上游 LICENSE。
