# Requirements Document

## Introduction

本次改造学员端错题本（路由 `/student/wrong`，当前由 `WrongBookPage` 承载、`WrongBookPanel` 渲染内容）。目标是把现有「一列错题 + 简单分组」的简陋页面升级为：顶部统计区（知识点错题饼图、按知识点错题数据、按错题类型错题数据）、中部页签（知识点错题 / 错误类型错题）、下部三栏（章节列表 / 小节列表 / 错题卡片列表）的布局；同时把「消灭错题 / 消灭知识点」的做题过程收敛到页内弹层，并新增「正确答案、章节、小节」等卡片信息。

## Glossary

- **错题本**：学员端汇总本人练习错题的页面（`/student/wrong`）。
- **错题卡片**：错题本中渲染单道错题的卡片元素。
- **原题**：已在错题本中记录的历史错题。
- **同题型新题**：与原题 `questionType` 相同、可用于巩固的新题。
- **消灭错题**：针对单道原题的一次页内巩固做题，包含原题与同题型新题。
- **消灭知识点**：针对某知识点下全部原题的一次页内巩固做题，并追加同题型新题。
- **错题组**：「消灭错题」或「消灭知识点」触发的一次做题集合。
- **知识点错题页签**：以章节、小节、错题卡片三栏组织错题的页签。
- **错误类型错题页签**：以题型分组组织错题卡片的页签。
- **问题类型**：题目题型，取值如 Radio（单选）、Checkbox（多选）、Judge（判断）、FillBlank（单项填空）、MultipleBlank（多项填空）、Text（多行文本）。

## Requirements

### Requirement 1

**User Story:** AS 学员, I want 一个按章节、小节、知识点、题型组织的错题本页面, so that 我能快速定位自己的薄弱错题并集中消灭。

#### Acceptance Criteria

1. WHEN 学员进入错题本页面, the system SHALL 在页面顶部展示知识点错题饼图、按知识点错题数据、按错题类型错题数据三个区域。
2. WHEN 学员进入错题本页面, the system SHALL 展示「知识点错题」「错误类型错题」两个页签。
3. WHILE 选中「知识点错题」页签, the system SHALL 以章节列表、小节列表、错题卡片列表三栏布局展示错题。
4. WHILE 选中「错误类型错题」页签, the system SHALL 按题型分组展示错题卡片列表。
5. WHEN 学员选择某章节, the system SHALL 在小节列表展示该章节下存在错题的小节。
6. WHEN 学员选择某小节, the system SHALL 在错题卡片列表展示该小节下的全部错题。
7. IF 学员的错题总数为零, the system SHALL 展示空态提示且不展示三栏区域。

### Requirement 2

**User Story:** AS 学员, I want 每道错题以字段完整的卡片呈现, so that 我能在一屏内看到错误全貌。

#### Acceptance Criteria

1. The system SHALL 为每道错题渲染一张错题卡片。
2. The system SHALL 在错题卡片展示知识点、错误次数、题目、我的答案（最近一次）、正确答案。
3. IF 错题未关联知识点, the system SHALL 在卡片上以「未归知识点」标识。
4. IF 错题缺少最近一次答案, the system SHALL 在卡片上以占位符展示「我的答案」。

### Requirement 3

**User Story:** AS 学员, I want 点击「消灭错题」后在当前页完成同题型巩固, so that 我不用跳转页面即可消灭错题。

#### Acceptance Criteria

1. WHEN 学员点击错题卡片的「消灭错题」, the system SHALL 在页内弹层展示一个错题组，错题组包含原题且总题数不超过 3，题目均为原题的题型。
2. WHILE 学员在弹层内作答, the system SHALL 逐题判分并展示对错与正确答案。
3. WHEN 错题组内全部题目答对, the system SHALL 将原题移出错题本。
4. IF 错题组内存在答错题目, the system SHALL 保留原题在错题本。
5. The system SHALL 在消灭过程中停留在错题本页面且不跳转到学习页面。

### Requirement 4

**User Story:** AS 学员, I want 一键消灭某个知识点下的全部错题, so that 我能成组攻克同一知识点的错误。

#### Acceptance Criteria

1. The system SHALL 在「知识点错题」页签为每个知识点分组提供「消灭知识点」按钮。
2. WHEN 学员点击「消灭知识点」, the system SHALL 在页内弹层展示该知识点下全部原题，并为每道原题追加 2 道同题型新题。
3. WHEN 弹层内全部题目答对, the system SHALL 将该知识点下全部原题移出错题本。
4. IF 弹层内存在答错题目, the system SHALL 保留该知识点下全部原题在错题本。
5. The system SHALL 在消灭过程中停留在错题本页面且不跳转到学习页面。

### Requirement 5

**User Story:** AS 学员, I want 系统为我生成同题型的新题, so that 我能通过同类题巩固而不仅是重复原题。

#### Acceptance Criteria

1. WHEN 需要为原题生成同题型新题, the system SHALL 优先从原题所属知识点的候选题中选取同题型题目。
2. The system SHALL 在生成同题型新题时排除当前错题组内已出现的题目。
3. IF 知识点候选不足, the system SHALL 依次从原题所属小节、所属题库的候选题中补充。
4. IF 候选仍不足, the system SHALL 按实际可用数量生成新题且不阻断做题流程。

### Requirement 6

**User Story:** AS 学员, I want 错题数据包含正确答案与章节小节信息, so that 3 栏布局与卡片答案展示有数据支撑。

#### Acceptance Criteria

1. The system SHALL 在错题列表返回每道错题的正确答案文本、章节标识与名称、小节标识与名称。
2. The system SHALL 仅返回当前学员本人的错题。
3. IF 错题缺少正确答案, the system SHALL 在卡片上以占位符展示「正确答案」。

### Requirement 7

**User Story:** AS 学员, I want 从知识点详情页查看错题时界面与独立错题本一致, so that 学习体验统一。

#### Acceptance Criteria

1. WHILE 学员从知识点详情页的错题页签查看错题, the system SHALL 展示与独立错题本一致的错题卡片与消灭交互。
