# 教研平台（习题列表页整体改造）需求文档

Feature Name: teaching-research-platform
Updated: 2026-09-08

## Introduction

管理端「习题管理 → 习题列表」（/exercise/list，原「知识树 × 练习总览」页）整体改造为**教研平台**：为老师提供统一教研工作台，按「学科 + 年级 → 章节 → 小节 → 知识点」的树状结构查看知识体系，直接点击章节 / 小节 / 知识点名称完成内容编辑与知识点简介维护；点击知识点后查看其直绑题目卡片（题干 / 答案 / 解析），点击卡片进入题目编辑弹窗校对，并支持从题库追加题目绑定到当前知识点。

- 原页面的「练习绑定 / 解绑 / 学员端预览」功能随本次整体替换移除；练习与题目的绑定维护仍保留在既有知识管理页（章节管理 / 小节管理 / 知识点管理页内嵌绑定入口），数据链路不受影响。
- 数据沿用 t_subject / t_chapter / t_section / t_knowledge_point / t_knowledge_point_question / t_template 与既有 API；题目编辑复用 `components/question/QuestionEditModal.jsx`。
- 本特性不改学员端链路与数据结构（知识点简介存储进既有 `t_knowledge_point.content` JSON 的新增可选键 `intro`，不加列）。

## Glossary

- **知识节点**：学科 → 章节（t_chapter）→ 小节（t_section）→ 知识点（t_knowledge_point）。
- **节点直绑题目**：知识点层经 t_knowledge_point_question 直绑的 t_template 题目。
- **知识点简介（intro）**：展示在知识点节点旁的简介文字；持久化于 t_knowledge_point.content JSON 的可选 `intro` 字段（与既有 `points` 讲解要点并存）。历史数据无 intro 时显示「暂无简介」。
- **QuestionEditModal**：既有全局通用题目新建/编辑弹窗（components/question/QuestionEditModal.jsx）。

## Requirements

### R1. 教研平台入口（习题列表页整体改造）

**User Story:** AS 老师，I want 从「习题管理 → 习题列表」进入教研平台，SO THAT 在单一页面完成知识内容与题目的教研。

#### Acceptance Criteria

1. WHEN 用户点击菜单「习题管理 → 习题列表」（原 key /exercise/list），系统 SHALL 渲染教研平台页。
2. 菜单项标签 SHALL 更名为「教研平台」，路由与权限守卫沿用 /exercise/list（repo:list + template:list）。
3. 原习题列表页的练习绑定 / 题目总览 / 学员端预览交互 SHALL 从本页移除（对应维护入口保留在知识管理页）。
4. WHEN 用户直接访问 /exercise/list，系统 SHALL 重定向或渲染教研平台页。

### R2. 学科与年级选择

**User Story:** AS 老师，I want 先选择教研学科与年级，SO THAT 只浏览该学科该年级的知识体系。

#### Acceptance Criteria

1. WHEN 页面加载，系统 SHALL 展示学科选择器（复用 listSubjects）。
2. WHEN 用户选中学科，系统 SHALL 加载并展示该学科章节的年级集合；用户选择年级后 SHALL 加载该学科 + 年级章节（复用 listChapters 按 grade 过滤）。
3. WHEN 学科或年级变化，系统 SHALL 清空并重建知识树至根层。

### R3. 章节 → 小节 → 知识点 树状呈现

**User Story:** AS 老师，I want 在左侧以树状结构逐层展开 章节 / 小节 / 知识点，SO THAT 总览整册结构。

#### Acceptance Criteria

1. WHEN 展开章节节点，系统 SHALL 懒加载并以树展示其下小节（复用 listSections）。
2. WHEN 展开小节节点，系统 SHALL 懒加载并以树展示其下知识点（复用 listKnowledgePoints）。
3. WHEN 知识点存在简介（content.intro），系统 SHALL 在节点旁展示简介；WHEN 简介为空，系统 SHALL 展示「暂无简介」占位。
4. 节点 SHALL 附带类型图标与层级缩进，树 SHALL 支持同时展开多个分支。

### R4. 节点名称点击即改（编辑章节 / 小节 / 知识点）

**User Story:** AS 老师，I want 直接点击节点名称完成编辑，SO THAT 免去切换管理页。

#### Acceptance Criteria

1. WHEN 用户点击章节节点名称，系统 SHALL 打开章节编辑弹窗（学科、年级、学期、名称、排序等既有字段），保存调用既有章节更新接口。
2. WHEN 用户点击小节节点名称，系统 SHALL 打开小节编辑弹窗并调用既有小节更新接口。
3. WHEN 用户点击知识点节点名称或简介区，系统 SHALL 打开知识点编辑弹窗，字段含：所属（只读回显）、名称、排序、年级、学期、简介（textarea，映射 content.intro）、讲解要点（可选维护，复用 points 数组）。
4. 知识点保存 SHALL 保留既有 content.points 与 imageUrl，仅增量更新简介或要点。
5. WHEN 保存成功，系统 SHALL 即时刷新该节点名称与简介，不重置整棵树。

### R5. 知识点 → 直绑题目卡片列表

**User Story:** AS 老师，I want 点击知识点后查看其直绑题目卡片，SO THAT 校对知识点相关题目。

#### Acceptance Criteria

1. WHEN 用户选中知识点节点，系统 SHALL 在右侧区加载该知识点直绑题目（listKnowledgePointQuestions）并以卡片列表展示。
2. 题目卡片 SHALL 呈现题干摘要、题型、难度、答案与解析；答案/解析区域 SHALL 默认折叠并支持「显示答案 / 隐藏答案」开关，便于校对时先看题再核答案。
3. WHEN 该知识点无直绑题目，系统 SHALL 展示空态与「从题库添加题目」按钮。
4. 卡片列表 SHALL 支持按题型与题干关键词筛选。

### R6. 卡片点击编辑题目

**User Story:** AS 老师，I want 点击题目卡片对题干 / 选项 / 答案 / 解析进行校对编辑，SO THAT 修正题目。

#### Acceptance Criteria

1. WHEN 用户点击题目卡片，系统 SHALL 以该题详情打开 QuestionEditModal（编辑态，复用既有保存组装逻辑）。
2. WHEN 用户保存，系统 SHALL 调用既有题目更新接口（updateTemplate）并刷新卡片内容与知识点绑定关系。
3. 题目编辑 SHALL 支持全部既有题型（单选 / 多选 / 判断 / 单项填空 / 多项填空 / 下拉 / 文本 / 评分等），交互与题目管理页一致。

### R7. 从题库添加题目到当前知识点

**User Story:** AS 老师，I want 将既有题库题目补充绑定到当前知识点，SO THAT 完善知识点题目覆盖。

#### Acceptance Criteria

1. WHEN 用户点击「添加题目」，系统 SHALL 弹出题目选择器（按题库 / 题型 / 关键词检索既有题目，来源 listTemplate 或 listRepo 下钻）。
2. WHEN 用户勾选并确认，系统 SHALL 以全量替换语义保存当前知识点的绑定集合（saveKnowledgePointQuestions：既有已绑 + 本次新增），并刷新卡片列表。
3. 新题目的本体创建仍由题目管理页承担，本平台不提供新建题目表单。

## 校验与约束（INCOSE 说明）

- 知识点简介仅作新增可选 JSON 键存储，任何既有读端（学员端讲解等）对未知键 SHALL 保持兼容。
- 删除 / 级联删除不在本特性范围内（知识管理页仍提供）。
- 题目绑定关系修改只影响 t_knowledge_point_question，不影响题目所属练习（t_template.repo_id）。
