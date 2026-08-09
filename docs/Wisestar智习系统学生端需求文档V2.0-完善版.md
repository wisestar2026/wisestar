# Wisestar 智习系统学生端产品需求文档 V2.0【完善版】

> 本文档在《Wisestar智习系统学生端产品需求文档 V2.0》（原 docx）基础上，结合项目当前已生成代码（后端 `server/`、前端 `wisestar-client/`）完善而成。
> 与「产品需求」并列补齐三块技术落地内容：
> ① **数据表全景**（已有 24 张表 + 学生端预设计 12 张新表）；
> ② **后端接口设计**（已有接口清单 + 学生端预设计接口及请求/响应示例）；
> ③ **前后端对接状态矩阵**（哪些已实现、哪些为 mock、哪些待开发）。
> 代码索引以 `docs/项目词典.md` 为准，本文档为需求与技术设计的一体化版本。

## 一、文档概述

### 1.1 文档目的

完整定义产品定位、功能架构、业务流程、交互规范、数据规则与技术实现参考，可直接交付 UI 设计、前端开发、后端开发、测试、项目存档。联动 `docs/项目词典.md`，实现「产品需求 - 功能模块 - 代码文件 - 方法接口 - 数据表」全维度对应。

### 1.2 适用范围与读者

产品经理、UI/UX 设计师、前端开发、后端开发、测试工程师。

### 1.3 版本迭代记录

| 版本 | 内容 |
|---|---|
| V1.0 | 基础功能 + 激励体系 |
| V1.1 | 修正学习币「单科上限、多科通兑」核心规则 |
| V1.2 | 新增后台绑定学科、学生不可自主增删学科 |
| V1.3 | 预留教材版本切换功能、优化权限校验 |
| V2.0 | 全面升级年轻化 UI 规范、补齐页面微动效、新增技术落地/代码映射/数据底层/接口链路、全量功能统计，全文档闭环定稿 |
| **V2.0 完善版（本文档）** | 结合已生成代码：落地全部已有表（24 张）与已有接口（50+ 端点）；新增学生端 12 张预设计表、4 组前端 API 封装设计、27 个预设计接口；标注前后端对接状态 |

### 1.4 核心术语定义

| 术语 | 定义 | 对应代码/表 |
|---|---|---|
| 题目 Template | 可作答题型载体（单选/多选/判断/填空），含题干、选项、标准答案、分值 | `Template.java` → `t_template` |
| 题库 Repo | 题目集合容器，题目通过 repoId 归属题库 | `Repo.java` → `t_repo` |
| 练习 Practice | 学员刷题单次会话（专项/套卷/随机） | `PracticeRecord.java` → `t_practice_record` |
| 错题 | is_correct=0 的题目明细，错题本/统计核心数据源 | `PracticeDetail.java` → `t_practice_detail` |
| 判分 Judge | 学生答案与标准答案比对得出对错/分值，双端语义对齐 | `AnswerJudgeUtil.evaluate` / `utils/practiceHelpers.js` |
| 交卷 Submit | 练习结束提交作答，后端复核判分落库 | `POST /api/practice/submit` |
| 学海积分 | 终身成长值：全学科累计、永久不清零、仅用于头衔/证书 | 学生端 mock `useStudentStore.js` |
| 学习币 | 学期消费币：分科产出、单科上限 3000、学期清零、多科通兑 | 学生端 mock `useStudentStore.js` |
| 知识点（学生端） | 章节下最小学习单元，掌握度% + 五级评级 | mock `useStudentStore.js` `chapters[].kps` |
| 纯净学习模式 | 迎检模式：激励模块 DOM 隐藏，仅保留学习功能 | mock `useStudentStore.js` `pureMode` |

---

## 二、产品定位与核心目标

面向中小学生的 AI 智习室学习系统，以**自主学习 + 错题巩固 + 激励成长**为核心。风格：书香少年感、现代化轻交互、卡片极简风；无重度游戏化、无闯关打怪、无弹窗刷屏、无排行榜攀比。

核心价值：① 知识点粒度学习 + 薄弱点智能识别，靶向补弱；② 双轨数值体系 + 五级头衔证书，正向激励；③ 多维度学情统计，学情可视。

---

## 三、用户角色与权限

### 3.1 系统角色总览

| 角色 | 职责 | 实现状态 |
|---|---|---|
| 校长 | 校区全局管理：学情监控、学员新增、商城商品管理、商品核销 | 角色体系已实现（t_role）；商城管理端功能**待开发** |
| 老师 | 教学内容管理：题目编辑、知识点编辑、试卷管理、练习册管理 | 题目/题库管理已实现（TemplateApi/RepoApi） |
| 学管师 | 学生督导：学员新增、学习任务分配、学生在线监督、学情监测 | 学员管理已实现（SystemApi user/create）；任务分配**待开发** |
| 学生 | 核心使用方：学习任务、自主学习、错题巩固、积分币兑换、学情查看 | 学生端页面已实现（前端 mock 原型），后端接口**待开发** |

### 3.2 学生角色核心权责

**可操作**：查看已绑定学科；知识点预习/练习/试炼/错题重做；查看每日任务并完成；查看学情与证书；学习币兑换商品。
**不可操作**：增删改绑定学科；增删改学习任务；修改题库/题目内容；修改奖励规则/数值体系；核销兑换商品。

---

## 四、整体功能架构

### 4.1 功能模块总览（8 大模块 32 项）

| # | 模块 | 核心功能 | 实现状态（2026-08-09） |
|---|---|---|---|
| 1 | 基础配置 | 学科切换、版本切换、模式切换、个人设置 | 前端 mock 已实现 |
| 2 | 数据数值 | 积分体系、学习币体系、数据统计、学期重置 | 前端 mock；后端待开发 |
| 3 | 自主学习 | 预习、小测、小节练习、章节测试、试炼、薄弱点识别与攻克 | 前端 mock 页面 + 练习落库后端已实现（PracticeApi） |
| 4 | 错题巩固 | 错题本、错题重做消灭 | 管理端错题库已实现（/api/practice/wrong-list）；学生端个人错题本待开发 |
| 5 | 任务体系 | 任务查看、任务奖励结算 | 前端 mock（今日待办）；后端待开发 |
| 6 | 荣誉成长 | 头衔晋升、证书解锁/管理、学情总览 | 前端 mock 已实现；后端待开发 |
| 7 | 商城兑换 | 币数查询、商品兑换、兑换记录 | 前端 mock 已实现；后端待开发 |
| 8 | 辅助技术 | 防刷机制、双端判分、动效交互、AI 讲解预留 | 双端判分已实现（AnswerJudgeUtil/practiceHelpers）；AI 后端能力就绪前端未接 |

---

## 五、核心业务流程

### 5.1 自主学习主流程

1. 学生登录 → 首页查看今日数据与待办任务
2. 切换学科 → 进入学海研习主页面（三栏）
3. 选章节 → 选知识点 → 查看知识点详情
4. 完成：预习 → 小测 → 小节练习 → 章节测试
5. 系统自动判分、计算奖励、标记薄弱知识点
6. 针对薄弱点专项攻克，或到错题本重做错题
7. 积分、学习币、头衔自动更新

### 5.2 练习交卷与判分流程（已实现）

**核心原则：前端仅即时展示，后端全权复核判分，杜绝数据篡改。**

1. 前端答题：渲染题目卡片，作答即时判分展示
2. 交卷触发：倒计时结束 / 手动交卷
3. 请求：`POST /api/practice/submit`，参数 `{mode, repoId, durationMs, items:[{questionId, answer}]}`
4. 后端：`PracticeApi` → `PracticeServiceImpl.submitPractice`
5. 复核：`templateService.list` 回源题目标准数据，逐题 `AnswerJudgeUtil.evaluate` 复核
6. 落库：`t_practice_record` 会话汇总 + `t_practice_detail` 逐题明细（错题 is_correct=0）
7. 返回：最终得分、正确率，前端展示结果与奖励、薄弱知识点更新

### 5.3 奖励发放流程（后端待开发，规则已定稿）

1. 完成学习行为 → 后端三重校验（学科/版本/奖励合法性）
2. 校验通过计算应发积分与学习币 → 校验单科 3000 上限（超出不发放）
3. 累加积分、累加对应学科学习币 → 积分达阈值自动晋升头衔/解锁证书
4. 前端顶部轻柔提示奖励到账

### 5.4 商城兑换流程（后端待开发，规则已定稿）

1. 进入荣誉商城浏览商品 → 点击兑换
2. 后端汇总本学期所有绑定学科币总和校验余额 → 自动多科合并扣款
3. 生成兑换记录与 6 位数字核销码 → 返回前端
4. 学生线下提交核销码给校长核销 → 校长后台核销 → 系统标记完成

---

## 六、功能模块详细需求

### 6.1 首页与个人中心

#### 学生首页（前端已实现 mock：`StudentHomePage.jsx`）

- 顶部磨砂通栏 + 上半部三卡（我的档案/学海研习/荣誉商城）+ 下半部今日数据总览
- 档案卡：头像/头衔/总积分/证书数，点击跳档案页
- 研习卡：书本插画 +「开启学海研习」，点击进研习页
- 商城卡：本学期可兑换总学习币，hover 展开各科明细，点击跳商城
- 今日数据总览：今日时长/完成知识点/获得积分/获得学习币 四模块轻图表
- 底部：今日待办任务快捷跳转列表

#### 我的档案荣誉页（前端已实现 mock：`ProfilePage.jsx`）

- 顶部个人信息大卡：头像、最高头衔、总学海积分
- 中部证书网格陈列墙：已解锁彩色 / 未解锁置灰，点击弹窗预览/下载/保存/打印
- 底部成长数据统计：累计知识点/达标章节/薄弱板块
- 规则：证书解锁永久保留、头衔永不降级

### 6.2 学海研习模块（前端已实现 mock：`StudyPage.jsx` + `KnowledgePage.jsx`）

- 左栏章节学海洲岛卡片导航：章节卡片（名称/渐变进度条/完成度），点击平滑展开知识点（掌握度% + 评级标签），可多开
- 中栏：未选知识点 → 学科整体进度大图 + 环形统计；选中 → 知识点简介/状态/学习引导/薄弱标记
- 右栏悬浮操作面板：知识点预习 / 专项练习湾 / 试炼检测 / 错题本 四大胶囊按钮 + AI 小鲸向导建议
- 知识点详情页四模式（?tab=preview|practice|trial|wrong）：
  - 预习：讲解要点卡，完成 币+5/积分+3
  - 练习/试炼：大圆角卡片选项，提交逐题判对错，顶部奖励提示 1.5s 自动消失
  - 错题：正确答案高亮 + 去练习重做
- 核心业务逻辑（后端待开发，规则已定稿）：
  - 知识点学习+小测：学完理论自动推送小测，完成即知识点学习完成
  - 小节练习：小节内知识点全部学完解锁，完成获奖励
  - 章节测试：小节练习全完成后解锁，测试后自动研判薄弱知识点清单
  - 薄弱点攻克：专项刷题正确率达标自动消除薄弱标记（币+25/积分+12）
  - 错题消灭：错题本重做正确自动清除该错题记录

### 6.3 错题巩固模块

- 个人错题本（管理端错题库已实现 `WrongQuestionPage.jsx` + `GET /api/practice/wrong-list`；学生端个人版待开发）：
  - 数据源 t_practice_detail is_correct=0，按题库/题型/关键词/时间筛选
  - 展示题干、用户答案、正确答案、错误次数，支持重做，重做正确自动移除

### 6.4 任务体系模块（前端 mock 已实现；后端待开发）

- 每日任务：学管师分配，类型=知识点练习/错题订正/有效学习时长，展示进度/状态/奖励
- 自动结算：完成 2 个知识点练习 币+8/积分+5；订正错题≥3 道 币+6/积分+3；有效学习 30 分钟 币+7/积分+4；每日 0 点重置

### 6.5 荣誉成长模块（前端 mock 已实现；后端待开发）

五级头衔：初探者(0)/勤学者(≥300)/深耕者(≥1600)/善思者(≥5500)/领航者(≥9000)，积分自动晋升、无降级、证书永久保留。

### 6.6 荣誉商城模块（前端 mock 已实现；后端待开发）

- 顶部数据区：可兑换总学习币大字 + 各科剩余币折叠明细 + 上限提示
- 商品网格：可兑换亮色 / 不足置灰
- 兑换记录：商品名/时间/核销码/核销状态，支持核销码复制

### 6.7 系统设置模块（前端已实现 mock）

双合规模式：常态（全展示）/ 纯净学习（激励模块 DOM 移除，仅留学科版本切换、预习、练习、试炼、错题本）；localStorage 持久化。

---

## 七、全局 UI 视觉与交互规范

（与 V2.0 原版一致，已在代码落地，要点如下）

- 视觉：极低饱和浅学海蓝渐变背景、大圆角磨砂玻璃卡片、柔和双层阴影、大量留白、无粗边框/实线分割
- 动效：卡片 hover 上浮+scale1.015/300ms；点击下压回弹；章节平滑展开收拢；页面淡入过渡；奖励顶部轻柔提示 1.5s 自动消失；数据加载骨架屏
- 顶部通栏：左 LOGO+学科胶囊 Tab（彩色圆角、默认第一个、横向滚动）+ 教材版本下拉（跟随学科、记忆上次选择）；右 头像/最高头衔/总学海积分/消息/设置（含纯净模式开关）；底部常驻小字「学海积分全学科永久累计 | 学习币单科限产、多科通兑、学期清零」
- 禁止清单：❌ 死板列表/直角模块/粗线条；❌ 游戏化弹窗/奖励爆炸/排行榜/PK；❌ 低幼卡通

---

## 八、核心数据体系

### 8.1 双轨数值体系（核心底层，不可修改）

| 数值 | 获取 | 特性 | 用途 |
|---|---|---|---|
| 学海积分 | 全学科学习行为累计 | 永久不清零、无上限、不分科、不学期重置 | 仅头衔晋升 + 证书 |
| 学习币 | 分学科独立产出 | 单科单学期上限 3000、学期清零 | 多科合并兑换商品 |

### 8.2 奖励体系（定稿，后端待开发）

| 行为 | 学习币 | 学海积分 |
|---|---|---|
| 预习（基础首学，7 天防刷） | +5 | +3 |
| 专项练习 | +12 | +6 |
| 试炼检测 | +20 | +10 |
| 全套错题订正 | +8 | +4 |
| 试炼正确率≥90% | +15 | +8 |
| 薄弱知识点复测达标 | +25 | +12 |
| 每日：完成 2 个知识点练习 | +8 | +5 |
| 每日：订正错题≥3 道 | +6 | +3 |
| 每日：有效学习 30 分钟 | +7 | +4 |
| 章节阶段（1/2/3/4/5/6+章，终身一次） | 80/100/120/150/180/200 | 40/50/60/75/90/100 |

### 8.3 防刷机制强制规则

- 同一知识点 7 天内重复学习零奖励（后端通过学习行为记录表统一拦截）
- 单科币满 3000 后不再产币，提示「本学科本学期学习币已达上限，可继续学习积累学海积分」

---

## 九、权限与数据安全

1. 学科权限：学生学科由后台 `t_user_subject_relation` 绑定，前端/学生不可增删改
2. 版本权限：学生可自由切换学科下所有可用教材版本，切换后学习进度完全独立
3. 任务权限：每日任务由学管师/老师后台分配，学生仅查看/执行/完成，任务完成自动结算
4. 后端三重校验（所有学习接口强制）：学科合法性、版本合法性、奖励合法性（7 天防刷）
5. 数据隔离：教材版本不同 → 章节/知识点/进度完全独立；学习币归属学科与版本无关；学海积分全学科统一

---

## 十、数据表全景

### 10.1 已有数据表（24 张，已建表落库）

> 建表脚本：`server/rdbms/src/main/resources/scripts/init-h2.sql`（云端 H2，`spring.sql.init.mode: always` 每次启动幂等执行）、`init-mysql.sql`（本地 MySQL 8）。除 `t_repo` 外均含 `is_deleted`（BaseModel @TableLogic 逻辑删除）。

#### 用户与权限

| 表 | 用途 | 关键字段 | 对应实体 | 状态 |
|---|---|---|---|---|
| t_user | 用户信息 | name/dept_id | User | ✅ 已实现 |
| t_account | 登录凭据（RSA） | auth_account/auth_secret/secret_salt/user_type/status | Account | ✅ 已实现 |
| t_role / t_user_role / t_role_permission / t_permission | 角色权限 | | Role/UserRole/RolePermission/Permission | ✅ 已实现 |

#### 题库与题目

| 表 | 用途 | 关键字段 | 对应实体 | 状态 |
|---|---|---|---|---|
| t_repo | 题库（**遗留表，无 is_deleted 列**） | name/mode/shared/tag/is_practice | Repo | ✅ 已实现 |
| t_template | 题目 | name/repo_id/question_type/template(JSON: children+attribute)/tag/subject/chapter/knowledge_point/difficulty | Template | ✅ 已实现 |
| t_repo_template | 题库-题目关联 | repo_id/template_id | RepoTemplate | ✅ 已实现 |
| t_user_repo | 题库分配（教师→学员） | user_id/repo_id | UserRepo | ✅ 已实现 |
| t_tag | 标签 | entity_id/name | Tag | ✅ 已实现 |

#### 学习行为

| 表 | 用途 | 关键字段 | 对应实体 | 状态 |
|---|---|---|---|---|
| t_practice_record | 练习会话汇总 | user_id/mode/repo_id/total_questions/correct_count/score/total_score/duration_ms | PracticeRecord | ✅ 已实现 |
| t_practice_detail | 逐题明细（错题唯一数据源） | practice_id/question_id/question_type/user_answer/is_correct/score | PracticeDetail | ✅ 已实现 |
| t_user_book | 用户练习册记录 | | UserBook | ✅ 已实现 |

#### 问卷/表单（surveyking 原生底座）

| 表 | 用途 | 对应实体 | 状态 |
|---|---|---|---|
| t_project / t_project_partner | 问卷项目/协作 | Project/ProjectPartner | ✅ 已实现 |
| t_answer / t_answer_detail | 答卷/明细 | Answer/AnswerDetail | ✅ 已实现 |

#### 系统基础

| 表 | 用途 | 对应实体 | 状态 |
|---|---|---|---|
| t_file | 附件 | File | ✅ 已实现 |
| t_comm_dict / t_comm_dict_item | 字典 | CommDict/CommDictItem | ✅ 已实现 |
| t_dashboard | 仪表盘配置 | Dashboard | ✅ 已实现 |
| t_dept / t_position / t_user_position | 组织/岗位 | Dept/Position/UserPosition | ✅ 已实现 |
| t_sys_info | 系统信息 | SysInfo | ✅ 已实现 |

### 10.2 学生端预设计新表（12 张，待建表开发）

> 设计原则：学习币归属学科、与版本无关；学海积分全学科统一；章节/知识点/进度按「学科+版本」独立。

#### 学科与版本

| 表 | 用途 | 关键字段 | 状态 |
|---|---|---|---|
| t_subject | 学科字典 | id/name/code/icon/theme_color/sort | ⏳ 待开发 |
| t_textbook_version | 教材版本（学科下多版本） | id/subject_id/name/status | ⏳ 待开发 |
| t_user_subject_relation | 用户-学科绑定（后台绑定，学生不可改） | id/user_id/subject_id | ⏳ 待开发 |

#### 数值与学习进度

| 表 | 用途 | 关键字段 | 状态 |
|---|---|---|---|
| t_user_points | 用户学海积分/头衔（终身） | user_id/points/title_level/title_name | ⏳ 待开发 |
| t_subject_semester | 学科学期学习币 | user_id/subject_id/semester/coins/reached_limit | ⏳ 待开发 |
| t_user_learning_record | 学习行为记录（7 天防刷 + 奖励领取） | user_id/subject_id/knowledge_point_id/action_type/coins/points/learned_at | ⏳ 待开发 |
| t_user_knowledge_progress | 知识点掌握度（学科+版本独立） | user_id/subject_id/version_id/chapter_id/knowledge_point_id/mastery/times | ⏳ 待开发 |
| t_user_weak_knowledge | 薄弱知识点研判 | user_id/subject_id/knowledge_point_id/status(active/conquered)/conquer_times | ⏳ 待开发 |

#### 任务与荣誉

| 表 | 用途 | 关键字段 | 状态 |
|---|---|---|---|
| t_user_task | 每日任务（学管师分配） | user_id/task_date/task_type/target/progress/status/coins/points/reward_issued | ⏳ 待开发 |
| t_user_certificate | 用户证书（解锁永久保留） | user_id/certificate_key/unlocked_at | ⏳ 待开发 |

#### 商城

| 表 | 用途 | 关键字段 | 状态 |
|---|---|---|---|
| t_exchange_goods | 商品（校长维护） | name/emoji/price/stock/status | ⏳ 待开发 |
| t_exchange_record | 兑换记录 | user_id/goods_id/coins_spent/verify_code(6位)/status(created/exchanged)/create_at | ⏳ 待开发 |

---

## 十一、后端接口设计

### 11.1 已有接口（已实现，可直接复用）

#### 认证与用户（`UserApi`）

| 方法与路径 | 说明 | 状态 |
|---|---|---|
| POST /api/public/login | RSA 加密密码登录，Set-Cookie sk-token | ✅ |
| POST /api/public/register | 注册 | ✅ |
| POST /api/public/logout | 登出 | ✅ |
| GET /api/currentUser | 当前登录用户 | ✅ |
| GET /api/userOverview | 用户概览 | ✅ |
| GET /api/user/list | 用户列表 | ✅ |
| GET /api/listUserTask / listHistoryTask | 用户任务列表/历史任务 | ✅ |

#### 题库/题目（`RepoApi` / `TemplateApi`）

| 方法与路径 | 说明 | 状态 |
|---|---|---|
| GET /api/repo/list | 题库列表 | ✅ |
| GET /api/repo/my | 我的题库（学员端分配可见） | ✅ |
| POST /api/repo/assign | 题库分配学员 | ✅ |
| GET /api/repo/book/list | 练习册列表 | ✅ |
| POST /api/repo/bind / unbind | 组题/解绑 | ✅ |
| POST /api/repo/import / GET /api/repo/export | 题目导入/导出 | ✅ |
| GET /api/template/list | 题目列表（学科/章节/难度/知识点筛选） | ✅ |
| GET /api/template/get | 题目详情 | ✅ |
| POST /api/template/create/update/delete | 题目 CRUD | ✅ |

#### 练习与判分（`PracticeApi`）★ 学生端已对接部分

| 方法与路径 | 说明 | 状态 |
|---|---|---|
| POST /api/practice/submit | 交卷：`{mode, repoId, durationMs, items:[{questionId, answer}]}`，后端复核判分落库 | ✅ |
| GET /api/practice/wrong-list | 错题聚合（题目×学员，筛选：题库/题型/关键词/时间） | ✅ |

#### 学情分析（`AnalysisApi`）★ 学生端可复用

| 方法与路径 | 说明 | 状态 |
|---|---|---|
| GET /api/analysis/knowledge-point/stats | 知识点掌握度统计 | ✅ |
| GET /api/analysis/knowledge-point/student-profile | 学员知识点画像 | ✅ |

#### 其他（ExerciseApi/ReportApi/SystemApi 等）

`GET /api/exercise/list`（练习题）、`GET /api/report/{shortId}`（报告）、SystemApi（系统/角色/用户管理）、SurveyApi/AnswerApi/ProjectApi（问卷底座）、DashboardApi（`GET /api/dashboard/list`）、FileApi（附件）。

### 11.2 学生端预设计接口（27 个，待开发）

> 统一约束：`@PreAuthorize("isAuthenticated()")`；分页返回 `PaginationResponse{total,list}`；所有学习接口执行后端三重校验（学科/版本/奖励合法性）；错误统一 `GlobalExceptionHandler` JSON。

#### S1. 档案与首页（`StudentApi` → `StudentServiceImpl`）

| # | 方法与路径 | 说明 | 请求 | 响应示例 |
|---|---|---|---|---|
| 1 | GET /api/student/profile | 个人档案：头衔/总积分/证书数 | - | `{name, avatar, title:{name,level}, points, certCount, certTotal}` |
| 2 | GET /api/student/subjects | 绑定学科列表（后台绑定） | - | `{list:[{subjectId, name, icon, themeColor}]}` |
| 3 | GET /api/student/versions?subjectId= | 学科可用教材版本 | subjectId | `{list:[{versionId, name}]}` |
| 4 | GET /api/student/today | 今日数据总览 | - | `{minutes, kps, points, coins}` |
| 5 | GET /api/student/coins | 本学期各科学习币 | - | `{total, list:[{subjectId, coins, limit:3000}]}` |
| 6 | GET /api/student/certificates | 证书墙（解锁状态） | - | `{list:[{key, name, certName, unlocked, needPoints}]}` |

#### S2. 研习与学习进度（`StudentStudyApi`）

| # | 方法与路径 | 说明 | 请求 | 响应示例 |
|---|---|---|---|---|
| 7 | GET /api/student/study/progress?subjectId=&versionId= | 章节→知识点进度（学科+版本独立） | subjectId, versionId | `{chapters:[{id,name,progress,kps:[{id,name,mastery,level}]}]}` |
| 8 | GET /api/student/knowledge/detail?kpId= | 知识点详情+学习引导 | kpId | `{id,name,desc,mastery,level,weak:bool}` |
| 9 | GET /api/student/knowledge/questions?kpId=&type=practice\|trial | 知识点取题（练习/试炼） | kpId, type | `{questions:[{id,q,type,options:[{key,title}]}]}`（不含答案） |
| 10 | GET /api/student/knowledge/preview?kpId= | 预习讲解内容 | kpId | `{points:[...]}` |
| 11 | GET /api/student/knowledge/weak-list | 个人薄弱知识点清单 | - | `{list:[{kpId,name,subjectId,conquerReward}]}` |
| 12 | POST /api/student/learning/complete | 学习完成奖励结算（预习/练习/试炼/错题订正） | `{kpId, actionType, durationMs}` | `{ok, coins, points, titleUpgraded}` |
| 13 | POST /api/student/weak/conquer | 薄弱点攻克提交（正确率达标消除标记） | `{kpId, correctRate}` | `{ok, weakCleared, coins, points}` |

#### S3. 错题与任务（`StudentWrongApi` / `StudentTaskApi`）

| # | 方法与路径 | 说明 | 请求 | 响应示例 |
|---|---|---|---|---|
| 14 | GET /api/student/wrong-list | 个人错题本（复用聚合口径，user 维度） | page, repoId, type, keyword, timeRange | `PaginationResponse{list:[{questionId, q, userAnswer, correctAnswer, wrongTimes, lastTime}]}` |
| 15 | POST /api/student/wrong/redo | 错题重做结果（正确→自动移除+奖励） | `{questionId, answer}` | `{ok, removed, coins, points}` |
| 16 | GET /api/student/tasks?date= | 每日任务列表 | date | `{list:[{id,type,target,progress,status,coins,points}]}` |
| 17 | POST /api/student/task/claim | 任务奖励领取（系统自动结算后前端确认展示） | `{taskId}` | `{ok, coins, points}` |

#### S4. 商城（`ExchangeApi`）

| # | 方法与路径 | 说明 | 请求 | 响应示例 |
|---|---|---|---|---|
| 18 | GET /api/exchange/goods | 商品列表 | - | `{list:[{id,name,emoji,price,stock}]}` |
| 19 | POST /api/exchange/exchange | 兑换（多科合并扣款，生成 6 位核销码） | `{goodsId}` | `{ok, goodsName, coinsSpent, verifyCode}` |
| 20 | GET /api/exchange/records | 兑换记录 | page | `PaginationResponse{list:[{goodsName, coins, verifyCode, status, createAt}]}` |
| 21 | POST /api/exchange/verify | 核销（校长后台） | `{recordId}` | `{ok, status:"exchanged"}` |

#### S5. 设置与辅助（`StudentSettingApi` / 复用）

| # | 方法与路径 | 说明 | 请求 | 响应示例 |
|---|---|---|---|---|
| 22 | GET /api/student/settings | 纯净模式状态（也可前端 localStorage） | - | `{pureMode}` |
| 23 | POST /api/student/settings | 保存纯净模式 | `{pureMode}` | `{ok}` |
| 24 | GET /api/student/stats | 学情总览（累计知识点/达标章节/薄弱板块） | - | `{kps, chapters, weak, points}` |
| 25 | POST /api/student/preview/complete | 预习完成（奖励结算，防刷校验入口） | `{kpId}` | `{ok, coins, points}` |
| 26 | POST /api/student/trial/submit | 试炼交卷（≥90% 额外奖励判定） | `{kpId, items}` | `{ok, score, rate, coins, points, bonus}` |
| 27 | POST /api/student/semester/rollover | 学期重置（学习币清零，系统定时任务触发） | - | `{ok}` |

> 说明：接口 25/26 与 12 存在业务重叠，落地时合并到 `POST /api/student/learning/complete`（actionType 区分 preview/practice/trial/wrong），27 由后端定时任务执行无需前端调用。

### 11.3 接口对接规则

- 登录态：复用 `sk-token` Cookie（RSA 登录链路不变）
- 判分：交卷一律走 `POST /api/practice/submit`（后端复核），前端 `practiceHelpers.js` 仅做即时展示，双端语义必须保持一致
- 奖励：所有奖励计算/发放仅在 `StudentServiceImpl` 内执行，前端只展示结果
- 错误码：复用 `GlobalExceptionHandler` 统一 JSON（`{code, message, data}`）

---

## 十二、前端接口封装与页面映射

### 12.1 已有 api 模块（已实现）

| 文件 | 职责 |
|---|---|
| api/request.js | axios 封装（统一前缀 /api、错误处理） |
| api/user.js | 登录注册/当前用户 |
| api/repo.js | 题库列表/分配/组题解绑/导入导出 |
| api/template.js | 题目列表/详情/CRUD |
| api/practice.js | 交卷 submitPractice、错题 listWrongQuestions |
| api/survey.js / api/answer.js / api/project.js / api/upload.js | 问卷底座/上传 |

### 12.2 预设计 api 模块（待开发，与 11.2 接口一一对应）

| 文件 | 导出方法 |
|---|---|
| api/student.js | getProfile / getSubjects / getVersions / getToday / getCoins / getCertificates / getStats |
| api/study.js | getStudyProgress / getKnowledgeDetail / getQuestions / getPreview / getWeakList / completeLearning / conquerWeak |
| api/task.js | getDailyTasks / claimTask |
| api/exchange.js | getGoods / exchangeGoods / getExchangeRecords / verifyExchange |
| api/settings.js | getStudentSettings / saveStudentSettings |

### 12.3 页面与接口接入矩阵

| 页面（已实现 mock） | mock 数据源 | 待接入接口 |
|---|---|---|
| StudentHomePage 首页三卡+今日总览 | SUBJECTS/TODAY/DAILY_TASKS | S1-1/2/4/5、S3-16 |
| StudentLayout 通栏（学科/版本/纯净模式） | SUBJECTS/versions/localStorage | S1-2/3、S5-22/23 |
| StudyPage 研习三栏 | chapters[].kps | S2-7/8/11 |
| KnowledgePage 四模式 | QUESTION_BANK/PREVIEW_CONTENT/REWARDS | S2-9/10/12/13、S4 错题 |
| ProfilePage 荣誉墙 | TITLES/GROWTH_STATS | S1-1/6、S5-24 |
| MallPage 商城 | GOODS/subjectCoins state | S1-5、S4-18/19/20 |
| （待开发）错题本/任务/兑换记录页 | - | S3-14/15/16/17、S4-20 |

---

## 十三、前后端对接状态矩阵与开发优先级

### 13.1 状态总览

| 领域 | 现状 | 缺口 |
|---|---|---|
| 登录/题库/题目/练习交卷/错题聚合 | ✅ 后端+前端完整实现 | 无 |
| 学生端页面视觉与交互 | ✅ 前端 mock 全量实现 | 数据为本地 mock |
| 学生端后端业务（档案/进度/奖励/任务/商城/证书） | ⏳ 仅规则定稿 | 12 张表 + 27 接口待开发 |
| 薄弱点研判与攻克 | ⏳ 规则定稿 | t_user_weak_knowledge + S2-11/13 |
| 每日任务分配与结算 | ⏳ 规则定稿 | t_user_task + S3-16/17 |
| 商城兑换/核销 | ⏳ 规则定稿 | t_exchange_goods/record + S4 |
| AI 讲解 | 🟡 后端能力就绪 | 前端入口接入（右栏 AI 小鲸向导） |

### 13.2 开发优先级建议

1. **P0（学习主链路）**：t_subject/t_textbook_version/t_user_subject_relation + S1-1/2/3 + S2-7/8/9/10/12（档案、研习、知识点练习接入真实数据与奖励结算）
2. **P1（数值闭环）**：t_user_points/t_subject_semester/t_user_learning_record（防刷）+ S1-4/5 + S3-14/15（个人错题本、奖励实时到账）
3. **P2（激励闭环）**：t_user_task/t_user_certificate + S1-6/S3-16/17 + 头衔证书解锁
4. **P3（商城闭环）**：t_exchange_goods/t_exchange_record + S4 四接口 + 校长核销管理端
5. **P4（学情增强）**：t_user_weak_knowledge + 薄弱点研判/攻克 + AI 讲解前端接入

---

## 十四、文档配套

- 代码索引：`docs/项目词典.md`
- 开发过程：`docs/开发维护日志.md`
- 规划路线：`docs/AI自习室系统开发路线图.md`
- 原版需求：附件《Wisestar智习系统学生端产品需求文档 V2.0》（docx），本完善版为其技术落地版，两者冲突时以本完善版为准（标注了实现状态）
