# 学员端在线时长宝箱 需求文档

Feature Name: online-duration-chest
Updated: 2026-09-12

## Introduction

学员端首页新增「在线时长宝箱」玩法：以自然日累计在线时长为进度，设置 30 分钟、60 分钟、120 分钟三档宝箱；达到对应档位后学员可开启宝箱获得学习币，并以悬浮窗形式常驻学员端首页。本特性复用现有学习心跳（`t_study_session`）统计在线时长，复用统一奖励账本（`RewardService`）完成发放与幂等，确保刷新、切页、重复点击都不会重复发币。

## Glossary

- **在线时长**：学员当日通过学习心跳累积的有效在线时长（毫秒），取 `t_study_session` 当日全部会话 `duration_ms` 之和。
- **宝箱档位（tier）**：在线时长的三个阈值，分别为 30 分钟、60 分钟、120 分钟。
- **宝箱状态**：`locked`（未达成）、`claimable`（已达成未领取）、`claimed`（已领取）。
- **领取**：学员点击可领取宝箱，系统按档位发放学习币并记录为已领取。
- **悬浮窗**：学员端首页固定悬浮的宝箱面板，展示在线时长与三档宝箱状态。
- **自然日**：以服务器本地时区当日 00:00:00 至次日 00:00:00 为一天，宝箱进度与领取状态按自然日重置。

## Requirements

### R1. 在线时长统计

**User Story:** AS 学员，I want 系统按我的真实在线时长累计当日进度，SO THAT 宝箱进度与我在线时间一致。

#### Acceptance Criteria

1. WHILE 学员处于登录状态，the system SHALL 通过现有学习心跳持续累积该学员当日 `t_study_session.duration_ms`。
2. WHEN 学员请求在线时长宝箱数据，the system SHALL 返回当日在线时长（分钟），取该学员当日全部会话 `duration_ms` 之和并向下取整到分钟。
3. the system SHALL 按自然日切分在线时长，跨日后重新从 0 累计。

### R2. 三档宝箱与阈值

**User Story:** AS 学员，I want 在线时长达标后能开宝箱拿学习币，SO THAT 我的在线学习有即时奖励。

#### Acceptance Criteria

1. the system SHALL 提供 30 分钟、60 分钟、120 分钟三档在线时长宝箱。
2. WHEN 当日在线时长达到某档阈值，the system SHALL 将该档宝箱置为可领取。
3. the system SHALL 为每档宝箱配置固定的学习币奖励数额。

### R3. 宝箱状态与每日重置

**User Story:** AS 学员，I want 清楚看到每档宝箱是否已达成、是否已领取，SO THAT 我知道下一步该做什么。

#### Acceptance Criteria

1. the system SHALL 为每位学员的每一档宝箱维护三态之一：未达成、可领取、已领取。
2. WHEN 学员当日某档宝箱已领取，the system SHALL 将该档状态置为已领取并保持至当日结束。
3. WHEN 进入新的自然日，the system SHALL 将三档宝箱状态重置为未达成、当日在线时长从 0 起算。

### R4. 领取宝箱结算

**User Story:** AS 学员，I want 点击宝箱即到账学习币，SO THAT 领取动作简单可靠。

#### Acceptance Criteria

1. WHEN 学员请求领取某档宝箱，the system SHALL 校验该档当日在线时长已达标且尚未领取。
2. WHEN 校验通过，the system SHALL 通过统一奖励账本按该档数额发放学习币。
3. the system SHALL 以「学员 + 档位 + 自然日」作为领取幂等键，同一档位同一自然日重复领取不再发放。
4. IF 该档未达标或当日已领取，the system SHALL 拒绝本次领取并返回可读原因。
5. WHEN 学员领取成功，the system SHALL 返回本次到账学习币、累计学习币与该档最新状态。

### R5. 悬浮窗展示

**User Story:** AS 学员，I want 首页有一个常驻的宝箱悬浮窗，SO THAT 我随时知道在线时长进度并能开箱。

#### Acceptance Criteria

1. the system SHALL 在学员端首页以悬浮窗形式展示在线时长宝箱，展示当日在线时长与三档宝箱。
2. the system SHALL 用可区分视觉表达三档宝箱的未达成、可领取、已领取状态。
3. WHILE 存在可领取宝箱，the system SHALL 在悬浮窗上给出可领取提示。
4. WHEN 学员点击可领取宝箱，the system SHALL 展示本次到账学习币结果并刷新宝箱状态。
5. the system SHALL 支持悬浮窗收起与展开，收起后以悬浮入口保留可领取提示。

### R6. 数值归属与上限

**User Story:** AS 系统，I want 在线宝箱学习币进入统一账本并遵守既有规则，SO THAT 数值口径不分裂。

#### Acceptance Criteria

1. the system SHALL 将在线宝箱学习币记录到统一奖励账本 `t_user_learning_record`。
2. the system SHALL 使在线宝箱学习币计入学员总学习币展示。
3. WHILE 对应学科学期学习币已达单科上限，the system SHALL 按既有上限规则裁剪本次学习币发放。
4. the system SHALL 仅由后端计算与发放奖励，前端只展示结果。

### R7. 异常与兼容

**User Story:** AS 系统，I want 领取与查询在异常时保持稳定，SO THAT 不影响首页其他功能。

#### Acceptance Criteria

1. IF 学员未登录或非学员身份，the system SHALL 拒绝请求并返回未授权结果。
2. IF 并发提交同一档位领取，the system SHALL 保证仅一次实际发放。
3. IF 宝箱接口异常，the system SHALL 使首页其余模块正常展示。

## 边界约束（Non-Functional）

1. 在线时长以 `t_study_session` 为准，不新增独立的心跳链路；复用 `POST /api/student/study/heartbeat`。
2. 奖励发放复用 `RewardService.settle`，新增行为类型常量，不修改既有奖励规则与金额。
3. 领取写操作使用事务；幂等键防止重复发放。
4. 宝箱状态可在无独立表的前提下，由「当日在线时长 + 当日领取行为记录」推导；若需要显式持久化，另设状态表。
5. 悬浮窗不得遮挡首页主操作，且在纯净学习模式下按产品设置决定是否展示。

## Open Questions（待确认）

1. 在线宝箱学习币的学科归属（默认归入当前学习学科，继承单科 3000 上限）。
2. 悬浮窗默认展开还是收起成悬浮入口，是否在达到可领取时自动弹出提醒。

## 已确认决策（2026-09-12）

1. **三档学币数额**：30 分钟 +5 币、60 分钟 +10 币、120 分钟 +20 币；只发学习币，不发学海积分。
2. **在线时长口径**：采用心跳在线会话累计，取 `t_study_session` 当日全部会话 `duration_ms` 之和。
3. **与每日里程碑关系**：与现有「每日有效学习 30 分钟 +7 币 +4 分」并存；在线宝箱使用独立行为类型与幂等键，互不影响。
4. **学科归属（默认）**：在线宝箱学习币归入学员当前学习学科，继承单科 3000 上限；无学科上下文时不裁剪上限（实现时按现有 `RewardService` 规则处理）。
5. **悬浮窗（默认）**：默认收起为悬浮入口，存在可领取宝箱时自动展开提醒，支持手动收起/展开。
