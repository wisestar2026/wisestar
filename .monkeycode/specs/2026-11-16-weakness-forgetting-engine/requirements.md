# 知识点弱点攻克系统重构（记忆调度引擎）需求文档

Feature Name: 2026-11-16-weakness-forgetting-engine
Created: 2026-11-16
Status: 待确认
来源：用户「根据新的题目形式重新设计知识点弱点攻克系统，结合各种遗忘曲线的主流模型」+ 既有 WPB（精准破弱）实现 + 英语三阶四段升级

## Introduction

现有系统有两套互不相通的口径：学科知识点用「最近 5 次正确率加权」判薄弱（`t_user_knowledge_progress`，阈值 55），英语单词/句式/语法用固定艾宾浩斯档位排下次复习（5 分钟、30 分钟、12 小时、1 天、2 天、4 天，`EnglishReviewScheduler`）。新题型加入后（听音选词、词汇默写、句式填空、句子默写、连词成句、语法改写、情景运用、课本跟读），同一知识点在不同考察角度与作答方式下的难度差异很大，固定档位无法表达「这次是回忆还是产出」「隔了多久」「做题快慢」「错了几次」。

本次重构引入统一记忆调度引擎：以 FSRS-5 为主模型，SM-2 作为冷启动回退，Leitner 只用于界面分级展示；把「每次作答」折算成 1~4 评分，维护每个知识点的难度、稳定度、可提取度与到期时间；薄弱判定改为「可提取度 + 证据累积 + 输出题加重」的状态机，攻克改为「到期复习、换角度、输出题达标、间隔抬升」的联合条件。英语按 word / sentence / grammar item 级接入，语文数学等按知识点级接入，共用一套调度内核与一张调度表。

## Glossary

- **记忆调度项（review item）**：调度最小单位。英语为单词/句子/语法点 ID；其他学科为知识点 ID。
- **FSRS-5**：Free Spaced Repetition Scheduler 第 5 版，用难度 D（1~10）、稳定度 S（天）、可提取度 R（0~1）描述记忆。
- **可提取度 R**：`R(t,S) = (1 + 19/81 · t/S)^(-0.5)`，t 为距上次复习天数。
- **评分（grade）**：1 Again / 2 Hard / 3 Good / 4 Easy，由对错、用时、提示与重做次数折算。
- **作答方式（demand）**：`recognition`（选择/判断/听选）、`cued`（带提示填空）、`application`（改写/组句）、`full`（默写/无提示产出）。
- **严重度（severity）**：0~3，薄弱看板排序依据，融合可提取度缺口、失误次数、作答方式与薄弱标记。
- **冷启动**：调度项首次进入或历史不足 2 次时，用 SM-2 的 ease factor 与间隔序列产生初始稳定度。
- **攻克**：从薄弱池移出，需同时满足到期复习、换角度、输出题达标与稳定度抬升。
- **目标保持率**：复习时希望达到的可提取度，默认 0.9，可按学员配置调整。

## Requirements

### R1 统一记忆调度

**User Story:** AS 学员，I want 每个知识点都有独立的记忆状态与下次复习时间，SO THAT 复习节奏跟着我的遗忘速度走。

#### Acceptance Criteria

1. WHEN 学员对任一知识点完成一次作答，the system SHALL 按「用户 + 模块 + 知识点」写入或更新一条记忆调度记录，保存难度、稳定度、评分、到期时间、复习次数与失误次数。
2. the system SHALL 使用 FSRS-5 计算稳定度、难度与可提取度，默认目标保持率 0.9。
3. WHEN 调度项复习次数少于 2 次，the system SHALL 使用 SM-2 冷启动间隔（1 天、6 天、随后按 ease factor 递增），并把等价间隔写为稳定度。
4. WHEN 同一自然日内对同一调度项多次作答，the system SHALL 使用同日复习公式更新稳定度，并把到期时间顺延到下一自然日之后。
5. the system SHALL 把难度限定在 1~10、稳定度限定为不小于 0.1 天，并把失败后的稳定度限制为不高于复习前的稳定度。
6. the system SHALL 为每次作答写入一条复习日志，保存评分、作答方式、用时、复习前后稳定度与难度、复习前可提取度，供后续参数拟合与审计。

### R2 题型折算为评分

**User Story:** AS 学员，I want 选择和默写被区别对待，SO THAT 蒙对选择题不能替代真正的产出能力。

#### Acceptance Criteria

1. WHEN 学员答错，the system SHALL 给出评分 1（Again）。
2. WHEN 学员答对且使用提示或经过重做，the system SHALL 给出评分 2（Hard）。
3. WHEN 学员答对且用时低于该作答方式的快速阈值，the system SHALL 给出评分 4（Easy）。
4. WHEN 学员答对且用时高于该作答方式的慢速阈值，the system SHALL 给出评分 2（Hard）。
5. WHEN 学员答对且用时处于两个阈值之间，the system SHALL 给出评分 3（Good）。
6. WHEN 未提供用时，the system SHALL 给出评分 3（Good）。
7. the system SHALL 对 `cued`、`application`、`full` 作答的成功稳定度增益乘以 1.08，失败后的稳定度乘以 0.92，并在严重度计算中乘以 1.3。
8. the system SHALL 为 `recognition`、`cued`、`application`、`full` 分别使用 6/15/20/25 秒的中位用时与 0.55/0.55/0.55/0.55 的快速比例、2.0/1.7/1.75/1.8 的慢速倍数。

### R3 薄弱状态机

**User Story:** AS 学员，I want 薄弱判定稳定可信，SO THAT 不会错一道选择题就被长期标记。

#### Acceptance Criteria

1. WHEN 调度项证据少于 2 次，the system SHALL 只记录状态，不把调度项纳入薄弱池。
2. WHEN 可提取度低于 0.6 且证据不少于 2 次，the system SHALL 把调度项纳入薄弱池并记录薄弱原因。
3. WHEN 同一知识点连续答错达到 2 次，the system SHALL 把调度项纳入薄弱池。
4. WHEN 输出类作答方式答错一次，the system SHALL 把调度项纳入薄弱池并记录输出薄弱原因。
5. WHEN 调度项已在薄弱池且可提取度回升到 0.7 以上，the system SHALL 保持薄弱标记直到满足攻克条件。
6. WHEN 学员从薄弱池移出后再次答错，the system SHALL 重新纳入薄弱池并累计复发次数。
7. the system SHALL 把薄弱原因限定为 `low_retrievability`、`consecutive_wrong`、`output_error`、`lapse`。

### R4 攻克判定

**User Story:** AS 学员，I want 攻克意味着我真的记住了，SO THAT 奖励与状态可信。

#### Acceptance Criteria

1. WHEN 学员在到期状态下（复习前可提取度低于 0.85）答对，且本次考察角度与上次不同，the system SHALL 计入一次有效攻克证据。
2. WHEN 薄弱原因为输出薄弱，the system SHALL 要求至少一次 `cued`、`application` 或 `full` 作答达到评分 3 以上。
3. WHEN 有效攻克证据成立且复习后稳定度不低于 7 天，the system SHALL 把调度项移出薄弱池并记录攻克事件。
4. WHEN 复习后稳定度低于 7 天，the system SHALL 保留薄弱标记并缩短下次复习间隔。
5. the system SHALL 在攻克时保持既有奖励幂等口径（每知识点终身一次）。

### R5 复习计划与队列

**User Story:** AS 学员，I want 每天知道该复习什么、为什么，SO THAT 打开复习页就能开始。

#### Acceptance Criteria

1. WHEN 学员请求复习计划，the system SHALL 返回按模块统计的到期数、逾期数、薄弱数、新项数与预计耗时。
2. WHEN 学员请求复习队列，the system SHALL 按「严重度降序、到期时间升序、稳定度升序」排序，并返回每项的复习前可提取度、建议角度与建议作答方式。
3. WHEN 调度项未到期且不在薄弱池，the system SHALL 把该调度项放入「新项/巩固」配额，且新项数量不超过请求上限的 30%。
4. the system SHALL 在队列中保证同一知识点连续两项的角度不同。
5. WHEN 学员请求未来 30 天预测，the system SHALL 返回每日到期数与全体调度项的平均可提取度。

### R6 薄弱看板与攻克训练

**User Story:** AS 学员/老师，I want 看到最该攻克的点和预计遗忘时间，SO THAT 时间花在刀刃上。

#### Acceptance Criteria

1. WHEN 学员请求薄弱看板，the system SHALL 返回严重度、复习前可提取度、预计遗忘日期（R 降至 0.6 的日期）与建议动作。
2. the system SHALL 按严重度降序排列薄弱看板，并支持按学科与模块过滤。
3. WHEN 学员发起攻克训练，the system SHALL 选取严重度最高的 6~10 项，保证输出类作答方式占比不低于 40%，并返回每项的建议角度。
4. the system SHALL 在攻克训练中排除复习后稳定度已达 30 天且可提取度高于 0.9 的调度项。

### R7 与既有数据兼容

**User Story:** AS 系统，I want 平滑迁移旧数据，SO THAT 上线不丢失学习记录。

#### Acceptance Criteria

1. WHEN 调度服务首次遇到没有调度记录的单词/句子/语法点，the system SHALL 按熟练度映射初始稳定度（0~4 对应 0.4/1/3/7/16 天），并把下次复习时间映射为到期时间。
2. WHEN 调度服务首次遇到没有调度记录的知识点，the system SHALL 按掌握度映射初始稳定度 `0.5 + 29.5 × (mastery/100)^1.5` 与初始难度 `1 + 9 × (1 - mastery/100)`。
3. the system SHALL 在更新调度记录后，继续回写既有单词本/句子本/语法本的下次复习时间与熟练度，保证旧页面行为不变。
4. the system SHALL 提供幂等回填接口，把既有单词本、句子本、语法本与知识点进度批量写入调度表，重复执行不产生重复记录。
5. the system SHALL 只新增表和接口，不改动既有复习候选、错题本与通关接口的返回结构。

### R8 可解释与可观测

**User Story:** AS 教研/运维，I want 每次调度可追溯，SO THAT 能定位异常并迭代参数。

#### Acceptance Criteria

1. WHEN 学员查看薄弱看板，the system SHALL 返回薄弱原因与最近一次评分。
2. the system SHALL 记录复习日志的复习前可提取度、复习后稳定度与调度目标保持率。
3. WHEN 调度计算出现异常，the system SHALL 记录告警日志并保留既有调度记录，不阻断答题主流程。
