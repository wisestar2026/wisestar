# 学员端内容对接 需求文档

Feature Name: student-content-bridge
Updated: 2026-08-20

## Introduction

学员端（海洋智学）当前学习内容为纯前端 mock（学科/章节/知识点/题目均在 `useStudentStore` 内置）。本特性打通**后台管理端配置**与**学员端呈现**的闭环：后台录入/配置的学科、章节、小节、知识点、题库、题目，经学员端接口按订单权限过滤后真实呈现，替代 mock；练习判分与错题沿用既有真实落库链路（`/api/practice/submit`、`/api/practice/wrong-list`）。

## Glossary

- **学员端内容**：学员端展示的学习数据（学科/章节/小节/知识点/题目）。
- **后台配置**：后台管理端录入的知识管理数据（学科/章节/小节/知识点）与题目数据（题库 t_repo / 题目 t_template / 绑定关系）。
- **订单权限**：`t_student_permission` 中 `expire_at > NOW()` 的有效行（学科+年级+版本）。
- **练习设置**：小节 `practice` JSON（题量 questionCount / 难度 difficulty / 题型 types）。
- **绑定数据源**：小节-题库（t_section_repo）与知识点-题目（t_knowledge_point_question）。

## Requirements

### R1. 学员端学科与版本（按订单权限）

**User Story:** AS 学员，I want 查看我已被开通的学科与教材版本，SO THAT 仅学习被授权的科目。

#### Acceptance Criteria

1. WHEN 学员进入学员端，the system SHALL 按订单有效权限（学科+版本）返回可访问学科列表。
2. WHILE 学员切换学科，the system SHALL 仅展示该学科下有权限的教材版本。
3. IF 学员无任何有效权限，the system SHALL 提示「暂无可访问学科，请联系管理员开通」。

### R2. 研习内容（章节/小节/知识点）真实呈现

**User Story:** AS 学员，I want 在研习页看到后台配置的章节、小节与知识点，SO THAT 学习内容与后台维护一致。

#### Acceptance Criteria

1. WHEN 学员进入研习页并选择学科，the system SHALL 返回该学科下的章节列表（含小节数）。
2. WHEN 学员展开章节，the system SHALL 返回章节下的小节列表（含内容设置：学习目标/内容概述/讲解要点与练习设置）。
3. WHEN 学员进入小节，the system SHALL 返回小节下的知识点列表（含讲解要点与配图）。
4. IF 后台未配置任何内容，the system SHALL 显示空态提示「该章节暂无内容」而非 mock 数据。

### R3. 练习/试炼题目真实出题

**User Story:** AS 学员，I want 练习与试炼使用后台配置的题目，SO THAT 学习闭环真实有效。

#### Acceptance Criteria

1. WHEN 学员在小节发起练习，the system SHALL 按小节练习设置（题量/难度/题型）从小节绑定题库（t_section_repo → t_repo_template → t_template）出题。
2. WHEN 学员在知识点发起试炼，the system SHALL 从知识点绑定题目（t_knowledge_point_question → t_template）出题。
3. IF 绑定数据不足以满足题量，the system SHALL 返回实际可用题目并提示题量不足。
4. WHEN 学员交卷，the system SHALL 复用 `/api/practice/submit` 真实判分落库，错题进入错题本（`/api/practice/wrong-list`）。

### R4. 学员端接口权限与安全

**User Story:** AS 系统，I want 学员端内容接口仅对已登录学员开放且按订单权限过滤，SO THAT 未授权内容不可见。

#### Acceptance Criteria

1. WHEN 未登录用户访问学员端内容接口，the system SHALL 返回未认证错误（code 401）。
2. WHEN 学员请求学科内容，the system SHALL 仅返回订单权限内（expire_at > NOW()）的学科。
3. WHEN 后台账号访问学员端内容接口，the system SHALL 返回校验错误（非学员身份）。

### R5. 前端 mock 回退策略

**User Story:** AS 学员，I want 在接口异常时仍能使用学员端，SO THAT 网络问题不阻塞学习。

#### Acceptance Criteria

1. WHEN 学员端内容接口加载失败，the system SHALL 回退展示既有 mock 数据并提示「内容加载失败，当前为演示数据」。
2. WHILE 接口正常，the system SHALL 展示真实后台数据（mock 不参与渲染）。

## 边界约束（Non-Functional）

1. 学员端内容接口仅返回该学员权限范围内的数据。
2. 题目答案/解析仅在判分后向学员端暴露（防作弊）。
3. 学习积分/学习币/今日统计等数值仍为前端本地 mock（本次不接真实统计，留待后续）。

## Open Questions（待确认）

1. 练习题目范围：小节练习按「小节绑定题库」出题、知识点试炼按「知识点绑定题目」出题，是否两者都要？
2. 无数据时的呈现：后台未配置内容时显示空态提示（推荐），是否完全移除 mock 回退？
3. 首页学习统计（积分/学习币/今日任务）：本次是否接真实练习记录统计，还是仅打通内容展示？

## 已确认决策（2026-08-20）

1. **练习题目来源（两者都要）**：小节练习按小节练习设置从小节绑定题库（t_section_repo → t_repo → t_repo_template → t_template）出题；知识点试炼从知识点绑定题目（t_knowledge_point_question → t_template）出题。
2. **无数据呈现（空态提示）**：后台未配置内容时学员端显示空态提示（如「该章节暂无内容」），不展示 mock；接口异常时回退既有 mock 并提示「内容加载失败，当前为演示数据」。
3. **首页统计（仅内容打通）**：学习积分/学习币/今日任务等数值保持前端 mock，本次仅打通学科/章节/知识点/题目的真实内容呈现，真实统计留待后续迭代。
