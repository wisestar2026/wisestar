# 英语后台内容管理与前端对接 需求文档

Feature Name: 2026-09-18-english-admin-hub
Created: 2026-09-18
Status: 已确认（2026-09-18）
Related: `../student-english-hub/requirements.md`、`../student-english-hub/design.md`

## Introduction

在现有英语模块（词库 `t_english_word`、句库 `t_english_sentence`、单元目录 `t_english_unit`、语法库 `t_english_grammar`、AI 内容包 `t_english_ai_pack`）基础上，补齐管理端的内容组织能力与学员端的内容消费链路：

- 将英语内容层级由「版本 → 年级 → 学期/册别 → 单元」扩展为「版本 → 年级 → 学期/册别 → 单元 → 小节」，单词与句子均可归属到小节。
- 将语法提升为独立板块：新增管理端语法管理，并把语法库扩展到与词库/句库一致的版本/年级/学期/单元/小节维度。
- 收敛管理端入口：全部英语内容与配置统一归属「英语板块」菜单，修正现有路由与菜单选中不一致问题。
- 学员端按管理端维护的层级消费内容。

本功能只做内容组织与展示对接，不改变现有复习算法、学币发放与在线时长口径。

## Glossary

- **版本**：教材版本，取值如人教版、苏教版、北师大版、外研版。
- **年级**：一至六年级。
- **学期**：册别，取值上册、下册。
- **单元**：教材单元，如 `Unit 1 Helping at home`，由 `t_english_unit` 目录维护。
- **小节**：单元下的教学分段（如 `Section A`、`Section B`、`Part 1`），由 `t_english_section` 目录维护。
- **单词**：词库记录，字段含拼写、音标、释义、图片、音频、例句。
- **句子**：句库记录，字段含英文、中文释义、音频。
- **语法**：语法条目，字段含标题、讲解内容、例句、练习题。
- **英语板块**：管理端左侧菜单中承载全部英语相关内容与配置的菜单分组。
- **小节目录**：维护某「版本 + 年级 + 学期 + 单元」下小节名称与排序的目录数据。

## Requirements

### R1 英语内容层级扩展为五级

**User Story:** AS 管理员，I want 在单元下维护小节并组织单词与句子，SO THAT 英语内容粒度与真实教学进度一致。

#### Acceptance Criteria

1. WHEN 管理员维护小节，the system SHALL 按「版本 + 年级 + 学期 + 单元」维度新增、编辑与删除小节，并保存小节名称与排序。
2. WHEN 管理员查看小节列表，the system SHALL 按「版本 + 年级 + 学期 + 单元」过滤并返回该单元下的小节。
3. WHEN 管理员保存单词或句子，the system SHALL 接受可选的所属小节字段并随记录持久化。
4. WHEN 管理员筛选单词或句子，the system SHALL 支持按小节过滤，且小节候选项随所选版本、年级、学期、单元联动。
5. IF 同一「版本 + 年级 + 学期 + 单元」下已存在同名小节，the system SHALL 拒绝保存并返回明确提示。
6. WHILE 某单词或句子未指定小节，the system SHALL 将其归入该单元的「未分节」分组并正常展示。

### R2 单词管理按五级层级组织

**User Story:** AS 管理员，I want 按版本/年级/学期/单元/小节维护单词，SO THAT 快速定位与批量维护词库。

#### Acceptance Criteria

1. WHEN 管理员进入单词管理，the system SHALL 提供版本、年级、学期、单元、小节五级筛选与拼写关键字筛选。
2. WHEN 管理员新增或编辑单词，the system SHALL 提供版本、年级、学期、单元、小节、拼写、音标、释义、图片、音频、例句字段。
3. WHEN 管理员选择版本、年级、学期或单元，the system SHALL 联动刷新可选单元或小节候选项。
4. WHEN 管理员查看单词列表，the system SHALL 展示版本、年级、学期、单元、小节与操作列。
5. WHEN 管理员批量导入单词，the system SHALL 支持包含小节列的模板并按「版本 + 年级 + 学期 + 单元 + 小节 + 拼写」去重更新。
6. WHILE 单词缺少音频，the system SHALL 允许保存并在学员端回退浏览器语音合成。

### R3 句库管理按五级层级组织

**User Story:** AS 管理员，I want 按版本/年级/学期/单元/小节维护句子，SO THAT 句库与词库保持同一层级口径。

#### Acceptance Criteria

1. WHEN 管理员进入句库管理，the system SHALL 提供版本、年级、学期、单元、小节五级筛选与中英文关键字筛选。
2. WHEN 管理员新增或编辑句子，the system SHALL 提供版本、年级、学期、单元、小节、英文、中文、音频、排序字段。
3. WHEN 管理员查看句子列表，the system SHALL 展示版本、年级、学期、单元、小节、英文、中文、音频试听与操作列。
4. WHEN 管理员批量导入句子，the system SHALL 支持包含小节列的模板并按「版本 + 年级 + 学期 + 单元 + 小节 + 英文」去重更新。
5. WHILE 句子缺少音频，the system SHALL 允许保存并在学员端回退浏览器语音合成。

### R4 语法独立板块

**User Story:** AS 管理员，I want 独立维护语法内容，SO THAT 语法不再只依赖 AI 同步且可按教材层级组织。

#### Acceptance Criteria

1. WHEN 管理员进入语法管理，the system SHALL 按版本、年级、学期、单元、小节与标题关键字分页展示语法条目。
2. WHEN 管理员新增或编辑语法，the system SHALL 提供版本、年级、学期、单元、小节、标题、讲解内容、例句、练习题与排序字段。
3. WHEN 管理员删除语法，the system SHALL 从语法库移除该条目并保留操作记录。
4. WHEN AI 内容包同步语法，the system SHALL 写入版本、年级、学期、单元与小节字段，并按「版本 + 年级 + 学期 + 单元 + 小节 + 标题」判定新增或更新。
5. WHEN 管理员查看语法列表，the system SHALL 展示版本、年级、学期、单元、小节、标题、更新时间与操作列。
6. WHERE 语法条目，the system SHALL 保存讲解内容、例句与练习题为独立字段。

### R5 英语后台入口与路由收敛

**User Story:** AS 管理员，I want 英语相关菜单与页面集中且可达，SO THAT 不依赖记忆路径完成英语内容维护。

#### Acceptance Criteria

1. WHILE 管理员已登录，the system SHALL 在「英语板块」菜单下展示单元管理、小节管理、单词管理、句库管理、语法管理、AI 内容生成入口。
2. WHEN 管理员点击任一英语菜单项，the system SHALL 跳转到与该功能对应的页面，且页面在左侧菜单中高亮为选中状态。
3. WHILE 管理员位于任一英语页面，the system SHALL 保证该页面路由与菜单 key 一致。
4. WHEN 管理员从单词本发起「学习」，the system SHALL 跳转到单词学习页并携带所选单词，而非跳转到单词管理页。
5. IF 管理员缺少某英语菜单项所需权限，the system SHALL 隐藏该菜单项并在直接访问时返回无权提示。

### R6 学员端按层级消费内容

**User Story:** AS 学生，I want 按小节浏览与学习单词、句子，SO THAT 学习范围与课堂进度对齐。

#### Acceptance Criteria

1. WHEN 学生进入某单元的单词学习，the system SHALL 按该单元的小节分组展示单词，未分节单词归入「未分节」分组。
2. WHEN 学生进入某单元的句子学习，the system SHALL 按该单元的小节分组展示句子，未分节句子归入「未分节」分组。
3. WHEN 某单元不存在小节或全部内容未分节，the system SHALL 以单元整体方式展示，不产生空分组。
4. WHILE 学生在英语学习中心选择版本、年级与学期，the system SHALL 复用现有选择值并在会话内保持一致。
5. IF 学员端语法学习入口启用，the system SHALL 按「版本 + 年级 + 学期 + 单元 + 小节」展示语法条目供学生学习（本项是否纳入首期见待确认）。

### R7 权限、初始化脚本与兼容

**User Story:** AS 系统维护者，I want 新增数据与权限在初始化脚本中同步，SO THAT 环境重建后功能完整可用。

#### Acceptance Criteria

1. WHERE 新增小节目录表与语法字段，the system SHALL 在 `init-h2.sql` 与 `init-mysql.sql` 中同步新增幂等 DDL。
2. WHERE 新增小节管理权限点，the system SHALL 在 `PermissionConsts` 登记并在管理员角色 authority 串追加。
3. WHEN 现有单词或句子数据缺少小节，the system SHALL 保留原记录并在展示时归入该单元的「未分节」分组。
4. WHEN 环境重建执行初始化脚本，the system SHALL 保证英语表结构包含版本、年级、学期、单元、小节字段且可重复执行。
5. WHERE 单词表学期字段，the system SHALL 在 `init-h2.sql` 的词表建表语句中直接声明，避免依赖后置 ALTER。

## Data Models

### 复用表

- **t_english_word**：单词（version / grade / term / unit / spell / phonetic / meaning / image_url / audio_url / example_sentence），本期新增 `section`。
- **t_english_sentence**：句库（version / grade / term / unit / en / zh / audio_url / sort），本期新增 `section`。
- **t_english_unit**：单元目录（version / grade / term / unit / sort）。
- **t_english_grammar**：语法库，本期新增 `version / term / unit / section / sort` 与审计列。
- **t_english_word_book / t_english_sentence_book / t_english_learning_log**：学习与复习数据，本期不改结构。

### 新增表

- **t_english_section**：小节目录（id / version / grade / term / unit / section / sort / 审计列），唯一键 `(version, grade, term, unit, section)`，供单词、句子、语法共用。

## References

[^1]: (File) `../student-english-hub/requirements.md` - 英语学习中心既有需求（层级为四级）。
[^2]: (File) `../student-english-hub/design.md` - 英语学习中心既有技术设计。
[^3]: (File) `server/rdbms/src/main/resources/scripts/init-h2.sql#L2224` - 英语模块建表段。
[^4]: (File) `server/rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishGrammar.java` - 语法实体现状（仅 grade，未继承 BaseModel）。
[^5]: (File) `wisestar-client/src/components/layout/MainLayout.jsx#L114` - 「英语板块」菜单现状。
[^6]: (File) `wisestar-client/src/App.jsx#L352` - 英语管理端路由现状。

---

## 已确认决策（2026-09-18）

1. **小节数据模型**（1A）：新增 `t_english_section` 目录表，单词、句子、语法增加 `section` 字段；可维护小节排序，允许存在空小节。
2. **语法板块范围**（2A）：新增管理端语法管理页（增删改查），语法库扩展版本/年级/学期/单元/小节维度；学员端首期不展示语法。
3. **学员端按小节分组**（3A）：学员端单词与句子按小节分组展示，未分节内容归入「未分节」分组。

**文档状态**：已确认
**下一步**：技术设计见 `design.md`，实施任务见 `tasklist.md`
