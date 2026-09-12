# Requirements Document — 预习例题配置与用途区分

## Introduction

本次迭代为学员端「知识点预习」增加独立的例题配置能力，使预习例题与「专项练习」「小节通关」在题源与组卷策略上可以区分。当前三者共用同一个题池（小节绑定题库 ∪ 知识点显式绑定 ∪ 知识点标签匹配），预习仅通过学员端请求参数取前 3 题，后台无法分别指定。本次引入「题库用途标记」将绑定题库区分为预习专用、练习专用与通用，并在小节练习配置中增加预习的题量与题型配置，同时保持历史数据的既有行为。

## Glossary

- **系统（System）**：wisestar 系统，含管理端（Web 后台）与学员端（Web 学员端）。
- **出题引擎（Question Engine）**：`StudentServiceImpl.studyQuestions` 承载的统一组卷能力，按范围、题量、题型、难度与顺序策略聚合题目。
- **预习例题（Preview Example）**：学员在「知识点预习」场景看到的题目，模式标识 `preview`，用于讲解后的即时检测。
- **专项练习（Drill Practice）**：学员按当节全部知识点逐个训练的场景，模式标识 `special`。
- **小节通关（Section Clear）**：学员按后台配置对整节组卷并以正确率判定通关的场景，模式标识 `trial`。
- **题库用途标记（Repo Usage）**：小节与题库绑定（`t_section_repo`）上的用途属性，取值 `preview` 预习专用、`practice` 练习专用、`both` 通用。
- **预习配置（Preview Config）**：小节练习配置（`t_section.practice`）中面向预习例题的字段，含题量与题型。
- **绑定题库（Bound Repo）**：小节通过 `t_section_repo` 关联的题库。
- **向后兼容（Backward Compatibility）**：历史数据缺少新字段时，系统采用缺省值使行为与改造前一致。

## Requirements

### Requirement 1 — 题库绑定用途标记

**User Story:** AS 教师，I want 为小节绑定的每个题库标记用途，so that 我可以分别指定预习与练习各自使用的题库。

#### Acceptance Criteria

1. THE 系统 SHALL 在小节与题库的绑定上提供用途属性，取值包含预习专用、练习专用与通用。
2. WHEN 教师保存小节题库绑定，系统 SHALL 持久化每个题库的用途属性。
3. WHERE 绑定的用途属性缺失，系统 SHALL 将该绑定视为通用。
4. WHEN 教师查询小节已绑定题库，系统 SHALL 返回每个绑定的用途属性。
5. WHEN 教师仅提交题库标识而未提交用途，系统 SHALL 将对应绑定保存为通用。

### Requirement 2 — 预习例题取题范围按用途区分

**User Story:** AS 学员，I want 预习只看到老师指定的例题，so that 预习内容与练习内容互不混淆。

#### Acceptance Criteria

1. WHEN 学员进入知识点预习并组卷，系统 SHALL 仅从用途为预习专用或通用的绑定题库取题。
2. WHEN 学员进入专项练习并组卷，系统 SHALL 仅从用途为练习专用或通用的绑定题库取题，并叠加知识点标签匹配与显式绑定。
3. WHEN 学员进入小节通关并组卷，系统 SHALL 仅从用途为练习专用或通用的绑定题库取题。
4. IF 指定用途下无可用绑定题库，系统 SHALL 退回全部绑定题库，避免出现空卷。
5. WHEN 出题引擎未收到用途参数，系统 SHALL 使用全部绑定题库。
6. WHEN 出题引擎按用途过滤题库，系统 SHALL 使知识点标签匹配的候选题目同样受该用途范围约束。

### Requirement 3 — 预习题量与题型后台可配

**User Story:** AS 教师，I want 在后台配置预习题量与题型，so that 预习检测的题量与形式符合教学安排。

#### Acceptance Criteria

1. THE 系统 SHALL 在小节练习配置中提供预习题量与预习题型字段。
2. WHEN 教师保存预习题量或预习题型，系统 SHALL 将其持久化到小节练习配置。
3. WHEN 学员组预习卷且未显式传入题量，系统 SHALL 采用该小节的预习配置题量。
4. WHEN 学员组预习卷且未显式传入题型，系统 SHALL 采用该小节的预习配置题型。
5. WHERE 预习配置缺失，系统 SHALL 采用缺省值：题量为 3、题型不限。
6. WHEN 教师更新预习题量或题型，系统 SHALL 使后续预习组卷立即采用新配置。

### Requirement 4 — 预习保留即时判分与解析

**User Story:** AS 学员，I want 预习例题作答后立即知道对错并查看解析，so that 我能即时理解知识点。

#### Acceptance Criteria

1. WHEN 系统向学员返回预习例题，系统 SHALL 同时返回标准答案与解析。
2. WHEN 学员作答预习例题，系统 SHALL 按既有判分规则在学员端即时判定对错。
3. WHEN 学员完成预习例题，系统 SHALL 保持既有预习记录与奖励语义不变。

### Requirement 5 — 历史数据向后兼容

**User Story:** AS 系统，I want 历史绑定与历史配置继续按原行为工作，so that 升级不影响现有教学数据。

#### Acceptance Criteria

1. WHEN 历史绑定缺少用途属性，系统 SHALL 将其按通用处理，预习、专项练习与小节通关的取题范围与改造前一致。
2. WHEN 小节练习配置缺少预习字段，系统 SHALL 按缺省题量 3、题型不限组预习卷。
3. WHEN 教师未使用用途标记，系统 SHALL 使现有绑定同时服务于预习与练习。
