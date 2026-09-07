# 习题列表（知识树×练习）需求文档

Feature Name: exercise-list-knowledge-tree
Updated: 2026-09-07

## Introduction

管理端菜单「习题管理 → 习题列表」（/exercise/list）当前为空白占位页且路由未注册。系统历史中同名功能「练习列表」（/repos → RepoListPage）在 2026-09-03 菜单重构（commit 37ff6af）时被移出侧边栏。本特性将「习题列表」建设为**按知识树组织的练习与题目总览页**：管理端沿 学科 → 章节 → 小节 → 知识点 下钻，查看每个节点绑定/归属的练习与题量，维护节点与练习的绑定关系，预览学员端在该节点将刷到的题目；练习详情页回显该练习绑定的章节/小节并支持反向下钻。

核心业务链：习题（t_template）经「集合」（加入练习，写 t_template.repo_id）成为练习（t_repo）的内容；练习经「绑定」挂到章节（t_chapter_repo）与小节（t_section_repo）；题目也可经「知识点-题目」（t_knowledge_point_question）直接归属知识点；学员端 study 接口据此映射呈现刷题。本特性不改动学员端链路，只建设管理端对上述关系的总览与维护视图。

## Glossary

- **习题 / 题目（Template）**：管理端题目管理中维护的单道题，存储于 t_template。一道题同一时刻只归属一个练习（t_template.repo_id 单值）。
- **练习（Repo）**：题目集合，存储于 t_repo，经 repo.bind / repo.unbind 从题目管理中批量划入/划出题目。练习侧不直接感知知识树。
- **知识节点**：学科 → 章节（t_chapter）→ 小节（t_section）→ 知识点（t_knowledge_point）四级。学科作为树的根层。
- **节点-练习绑定**：章节↔练习存 t_chapter_repo；小节↔练习存 t_section_repo。均为练习挂在节点上的多值关系。
- **节点-题目绑定**：知识点↔题目存 t_knowledge_point_question。是知识点层唯一的直接题目绑定。
- **学员端刷题语义**：小节刷题 = 该小节绑定的全部练习中的全部题目（Template.repo_id ∈ section_repo.repoIds）；知识点刷题 = t_knowledge_point_question 绑定的题目；订单权限在学员端过滤学科/年级。
- **绑定维护入口**：现有章节管理/小节管理/知识点管理页内嵌的绑定弹窗（saveChapterRepos / saveSectionRepos / saveKnowledgePointQuestions 全量替换）。

## Requirements

### R1. 习题列表入口与占位页替换

**User Story:** AS 管理员，I want 侧边栏「习题列表」点击后进入真实功能页，SO THAT 不再看到空白占位。

#### Acceptance Criteria

1. WHEN 具备 repo 相关权限的管理员点击「习题管理 → 习题列表」，系统 SHALL 渲染知识树习题总览页。
2. WHEN 管理员直接访问 /exercise/list，系统 SHALL 返回该页（路由注册，替换空白占位 ExerciseListPage）。
3. 菜单中已移除的「在线练习」「练习分配」「错题管理」等真实功能页，系统 SHALL 一并恢复为可经菜单到达的入口，保持既有功能不被占用占位。

### R2. 知识树下钻总览

**User Story:** AS 管理员，I want 沿 学科 → 章节 → 小节 → 知识点 逐级下钻，SO THAT 清晰看到每层节点的绑定练习与题量。

#### Acceptance Criteria

1. WHEN 页面加载，系统 SHALL 展示学科列表；选中学科后 SHALL 展示该学科下的章节（按年级归组）。
2. WHEN 管理员点开章节，系统 SHALL 展示该章节绑定练习列表（名称、题量、类型标签）及下一级小节列表；每级节点 SHALL 同时标注其直绑练习数与下属节点数。
3. WHEN 管理员点开小节，系统 SHALL 展示该小节绑定练习列表、直绑题量汇总及该小节下知识点列表。
4. WHEN 管理员点开知识点，系统 SHALL 展示经 t_knowledge_point_question 直绑该知识点的题目列表（题干摘要、题型、难度）及题量。
5. 章节与小节节点 SHALL 聚合显示「该节点直接绑定练习中的题目总量 + 知识点直绑题量」作为该节点题量，口径与学员端取题语义一致。

### R3. 节点绑定练习维护

**User Story:** AS 管理员，I want 在习题列表页直接调整章节/小节的绑定练习，SO THAT 不必往返知识管理页。

#### Acceptance Criteria

1. WHEN 管理员在章节/小节节点发起「绑定练习」，系统 SHALL 弹出练习选择框，候选为全量练习，当前已绑练习处于勾选态。
2. WHEN 管理员保存选择，系统 SHALL 全量替换该节点的绑定关系（复用 saveChapterRepos / saveSectionRepos），并刷新该节点题量与练习列表。
3. WHEN 管理员在知识点节点发起「绑定题目」，系统 SHALL 弹出题目选择框（复用知识点管理页既有交互的数据来源），保存后 SHALL 全量替换 t_knowledge_point_question。
4. 任一节点已绑练习行 SHALL 支持解绑与进入该练习组题页（/repos/:id）。

### R4. 学员端刷题内容预览

**User Story:** AS 管理员，I want 预览学员端在所选节点将刷到的题目，SO THAT 核验绑定配置正确。

#### Acceptance Criteria

1. WHEN 管理员对章节/小节/知识点节点发起「预览题目」，系统 SHALL 按 R2.5 的节点题量语义聚合题目并以题卡列表展示。
2. 预览 SHALL 提供「显示答案/解析」开关，默认关闭；开启后 SHALL 随题卡展示正确答案与解析。
3. 预览取题 SHALL 与学员端 study/questions 采用相同语义（练习内题目 + 知识点直绑题目），但 SHALL NOT 受管理员的学员订单权限过滤。

### R5. 练习详情回显知识绑定

**User Story:** AS 管理员，I want 在练习详情页看到该练习被挂到了哪些章节/小节，SO THAT 判断内容投放范围。

#### Acceptance Criteria

1. WHEN 管理员打开练习详情（/repos/:id），系统 SHALL 展示该练习绑定的章节与小节列表（含学科/年级上下文），无绑定则显示空态提示。
2. 绑定章节/小节条目 SHALL 支持点击，导航到习题列表页对应节点（带定位参数）。
3. 练习详情 SHALL 提供「去绑定」入口，跳转知识管理对应层级完成新增绑定，回显数据 SHALL 在页面刷新后一致。

### R6. 学员端映射不变（约束）

**User Story:** AS 管理员，I want 本特性只新增管理端总览与维护能力，SO THAT 不回归学员端既有刷题。

#### Acceptance Criteria

1. 本特性 SHALL NOT 修改学员端 study 取题与鉴权语义。
2. 本特性涉及的全部绑定写库 SHALL 复用既有接口与既有表结构，不新增关系表。

## Out of Scope

- 新建/编辑练习与新建/编辑题目：仍由「题目管理」「练习列表（/repos）」承担，习题列表页只提供跳转入口。
- 练习分配给学员（/repo-assign）与错题库管理（/wrong-questions）的页面改造：本次仅恢复菜单入口。
- 学员端呈现样式或入口调整。
