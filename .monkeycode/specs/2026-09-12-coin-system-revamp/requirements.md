# 学习币体系重构 需求文档

Feature Name: coin-system-revamp
Updated: 2026-09-12

## Introduction

重构学员端学习币的产出与上限规则。目标有三点：一是把「按每次交卷发币」改为「每个学习内容每学期只发一次」，消除反复刷同一小节反复领币的问题；二是把单科单学期学习币上限从 3000 提升到 10000，让孩子看到数值持续增长；三是通过「知识点预习、消灭知识点（掌握度达精通）、专项练习、小节通关、章节达标、错题订正、每日签到」构成完整产出池，使完成约 90% 学习任务即可触及 10000 上限。老师手动发放的学习币继续独立记账并叠加展示，不占用 10000 上限。学海积分与头衔体系保持不变。

## Glossary

- **学习币（coins）**：按学科独立产出的兑换货币；单科单学期上限 10000；学期清零；只用于商城商品兑换。
- **学海积分（points）**：全学科终身累计的荣誉值；本次不改动其奖励值与头衔阈值。
- **自动学习币**：由学习行为结算产生的学习币（`t_subject_semester.coins`），受 10000 上限约束。
- **手动学习币**：老师通过 `POST /api/student/coin` 发放的学习币（`t_student_coin`），不占用 10000 上限。
- **内容幂等键（ref_id）**：一次奖励结算的业务关联标识；内容类奖励取「学期键 + 内容标识」，每日类奖励取自然日。
- **消灭知识点**：某知识点掌握度达到精通档（掌握度 ≥ 85），该知识点每学期首次达标时结算一次奖励。
- **学期键（semester）**：学习币的有效周期键，格式 `{学年}-{上/下}`（如 `2026-1`）。
- **学习任务**：预习、消灭知识点、专项练习、小节通关、章节达标五类内容型行为，构成 10000 学币的主要产出池。

## Requirements

### R1. 单科单学期学习币上限提升到 10000

**User Story:** AS 学员，I want 单科单学期可累计到 10000 学习币，SO THAT 我能看到数值持续增长。

#### Acceptance Criteria

1. the system SHALL 将单科学期学习币上限设置为 10000。
2. WHEN 一次自动结算使某学科学期学习币超过上限，the system SHALL 仅发放至 10000、置 `reached_limit=true`，并返回 `coinsCapped=true`。
3. WHILE 某学科达到上限，the system SHALL 继续发放该行为对应的学海积分，并将该次学习币计为 0。
4. the system SHALL 使老师手动发放的学习币存放在 `t_student_coin`，且不计入 10000 上限。
5. WHEN 学员查询本学期学习币，the system SHALL 返回 `limit=10000`，并将自动学习币与手动学习币分列展示。

### R2. 内容学习奖励改为每学期每内容一次

**User Story:** AS 学员，I want 完成同一学习内容只领取一次奖励，SO THAT 奖励来自推进学习而不是反复刷题。

#### Acceptance Criteria

1. WHEN 学员首次完成某知识点的预习，the system SHALL 按「币 +5」结算，且同一知识点在同一学期仅结算一次。
2. WHEN 学员首次完成某小节的专项练习，the system SHALL 按「币 +20」结算，且同一小节在同一学期仅结算一次。
3. WHEN 学员首次完成某小节的小节通关（行为类型 `trial`，即试炼检测），the system SHALL 按「币 +30」结算，且同一小节在同一学期仅结算一次。
4. WHEN 学员完成某知识点且掌握度达到精通（≥ 85），the system SHALL 按「币 +60」结算一次，且同一知识点在同一学期仅结算一次。
5. WHEN 学员在某学科达成第 N 个达标章节（N=1..6，6 及以上按 6 计），the system SHALL 按「300/350/400/450/500/600 币」结算该阶段奖励，且同一学期每个阶段仅结算一次。
6. WHEN 学员完成某薄弱知识点的攻克复测，the system SHALL 按「币 +25」结算一次，且同一知识点在同一学期仅结算一次。
7. the system SHALL 以「学期键 + 内容标识」作为内容类奖励的幂等键，使同一学期内重复提交不重复发放。
8. WHEN 学员完成某小节通关且正确率不低于 90%，the system SHALL 追加「币 +15」（行为类型 `trial_bonus`），且同一小节在同一学期仅结算一次。

### R3. 消灭知识点奖励

**User Story:** AS 学员，I want 每次将知识点练到精通都能获得学习币，SO THAT 我主动刷题并消灭知识点。

#### Acceptance Criteria

1. WHEN 学员的一次练习评价使某知识点掌握度达到 85 及以上，the system SHALL 按「币 +60」结算一次消灭知识点奖励。
2. the system SHALL 以「学期键 + 知识点标识」作为消灭知识点奖励的幂等键。
3. WHILE 某知识点在同一学期已结算过消灭知识点奖励，the system SHALL 跳过该知识点的重复结算。
4. the system SHALL 在结算响应中返回本次是否首次发放，供前端展示到账结果。

### R4. 错题订正按题结算

**User Story:** AS 学员，I want 每订正一道错题就获得一次学习币，SO THAT 我主动检查并订正错题。

#### Acceptance Criteria

1. WHEN 学员订正一道错题，the system SHALL 按「币 +5」结算，且同一道题在同一学期仅结算一次。
2. the system SHALL 以「学期键 + 题目标识」作为错题订正奖励的幂等键。
3. WHILE 某道题在同一学期已订正结算过，the system SHALL 跳过该题的重复结算。

### R5. 每日签到

**User Story:** AS 学员，I want 每天签到领取学习币，SO THAT 我养成每日学习的习惯。

#### Acceptance Criteria

1. WHEN 学员完成当日签到，the system SHALL 按「币 +10」结算一次，且同一自然日仅结算一次。
2. the system SHALL 以自然日（`yyyy-MM-dd`）作为签到奖励的幂等键。
3. WHILE 学员当日已签到，the system SHALL 返回 `firstTime=false` 且不重复发放学习币。
4. WHEN 学员请求签到状态，the system SHALL 返回当日是否已签到与签到可得学习币。
5. the system SHALL 不设置连续签到天数逻辑。

### R6. 任务完成奖励

**User Story:** AS 学员，I want 完成老师布置的任务获得学习币，SO THAT 我按时完成每日任务。

#### Acceptance Criteria

1. WHEN 学员完成一条当日任务，the system SHALL 按「币 +15」结算一次。
2. the system SHALL 以「学期键 + 任务标识」作为任务奖励的幂等键。
3. IF 学员请求领取尚未达成的任务奖励，the system SHALL 返回校验错误且不写账本。
4. WHILE 某任务在同一学期已结算过奖励，the system SHALL 返回 `firstTime=false` 且不重复发放。

### R7. 学海积分与头衔体系保持不变

**User Story:** AS 学员，I want 学海积分与头衔规则保持稳定，SO THAT 我的荣誉评价不受学习币调整影响。

#### Acceptance Criteria

1. the system SHALL 保持 `t_user_points` 的积分累加口径不变。
2. the system SHALL 保持头衔阈值 300/1600/5500/9000 与头衔名称不变。
3. the system SHALL 保持既有学习行为的学海积分发放值不变（预习 3 / 专项练习 6 / 试炼 10 / 试炼优秀 8 / 错题订正 4 / 薄弱攻克 12 / 每日里程碑 5·3·4 / 章节阶段 40..100）。
4. the system SHALL 将消灭知识点、每日签到、任务完成三个新增行为的学习币计入，学海积分记为 0。

### R8. 产出池与 90% 达成目标

**User Story:** AS 运营人员，I want 学习币产出池与学习任务量匹配，SO THAT 完成 90% 学习任务即可拿到 10000。

#### Acceptance Criteria

1. the system SHALL 使单科单学期内容型学习任务的满额产出不低于 11000 学习币。
2. WHERE 教学科目含 91 个知识点、62 个小节、6 个章节阶段，the system SHALL 使满额产出为 11615 学习币：预习 455、消灭知识点 5460、专项练习 1240、小节通关 1860、章节阶段 2600。
3. WHEN 学员完成 90% 的内容型学习任务，the system SHALL 使其可触及 10000 学习币上限。
4. the system SHALL 将每日签到、在线宝箱、每日里程碑、错题订正、薄弱点攻克作为上限内的额外产出，加快触顶。

### R9. 结算幂等与学期重置

**User Story:** AS 系统，I want 奖励按学期与内容幂等，SO THAT 跨学期重新学习可再次获得奖励且学期内不重复。

#### Acceptance Criteria

1. the system SHALL 以 `t_user_learning_record` 的行为记录作为幂等判定来源。
2. WHEN 学习币跨入新学期的学期键，the system SHALL 使该学科学习币从 0 起算，并使内容型奖励在新学期可再次结算。
3. the system SHALL 保持历史行为记录不随学期清零而删除。
4. WHILE 同一学员同一行为与同一幂等键已存在记录，the system SHALL 跳过重复发放。
5. the system SHALL 终止按 7 个自然日重复发放的防刷逻辑，改由内容幂等键控制。

### R10. 前端展示与签到入口

**User Story:** AS 学员，I want 在学习币页面看到 10000 上限并在首页完成签到，SO THAT 我清楚规则并能每日操作。

#### Acceptance Criteria

1. WHEN 学员进入荣誉商城页，the system SHALL 展示单科单学期上限 10000 与各科进度。
2. WHEN 学员进入学员端首页，the system SHALL 提供每日签到入口并展示当日签到状态。
3. WHEN 学员点击签到，the system SHALL 展示本次到账的学习币或已签到提示。
4. the system SHALL 在积分规则说明中展示调整后的学习币数值。

## 边界约束（Non-Functional）

1. 奖励计算与发放仅在后端服务层执行，前端不得参与金额判定。
2. 所有结算写操作使用事务；幂等键防止重复发放。
3. 内容类奖励的 `ref_id` 形如 `{semester}:{contentId}`，每日类奖励的 `ref_id` 为自然日；`ref_id` 长度不超过 64。
4. 结算涉及的学科、知识点必须落在学员有效订单权限（`t_student_permission`，`expire_at > NOW()`）内。
5. 并发下同一学员同一幂等键的结算必须串行化，避免并发穿透。

## 拟定奖励表（学习币，待用户确认）

| 行为类型 | action | 币 | 积分 | 幂等 ref_id | 频率 |
|----------|--------|----|----|------------|------|
| 知识点预习 | `preview` | 5 | 3 | `preview:{kpId 或 sectionId}` | 每学期每内容一次 |
| 专项练习 | `practice` | 20 | 6 | `practice:{targetId}` | 每学期每内容一次 |
| 小节通关（试炼） | `trial` | 30 | 10 | `trial:{targetId}` | 每学期每内容一次 |
| 通关优秀追加 | `trial_bonus` | 15 | 8 | `trial_bonus:{targetId}` | 每学期每内容一次 |
| 消灭知识点（精通） | `kp_master` | 60 | 0 | `master:{kpId}` | 每学期每知识点一次 |
| 薄弱点攻克 | `weak_conquer` | 25 | 12 | `weak:{kpId}` | 每学期每知识点一次 |
| 错题订正 | `wrong_correct` | 5 | 4 | `wrong:{questionId}` | 每学期每题一次 |
| 章节阶段达标 | `chapter_stage` | 300/350/400/450/500/600 | 40/50/60/75/90/100 | `chapter_stage:{subjectId}:{stage}` | 每学期每阶段一次 |
| 每日签到 | `daily_checkin` | 10 | 0 | `{yyyy-MM-dd}` | 每自然日一次 |
| 任务完成 | `task_done` | 15 | 0 | `task:{taskId}` | 每学期每任务一次 |
| 每日知识点里程碑 | `daily_kp` | 8 | 5 | `{yyyy-MM-dd}` | 每自然日一次 |
| 每日错题里程碑 | `daily_wrong` | 6 | 3 | `{yyyy-MM-dd}` | 每自然日一次 |
| 每日有效学习 | `daily_time` | 7 | 4 | `{yyyy-MM-dd}` | 每自然日一次 |
| 在线宝箱 | `online_chest_30/60/120` | 5/10/20 | 0 | `{yyyy-MM-dd}:{tier}` | 每自然日每档一次 |

> 表中「币」列为拟定值，最终以用户确认为准；「积分」列除新增三项（消灭知识点、签到、任务完成记 0）外均保持现状不变。

## 已确认决策（2026-09-12）

1. **仅改学币**：学海积分与头衔阈值保持现状，新增行为积分记为 0。
2. **签到规则**：每日固定 +10 币，不设置连续签到天数。
3. **消灭知识点口径**：知识点掌握度达到精通（≥ 85）即算消灭，每个知识点每学期结算一次，+60 币。
4. **上限口径**：单科单学期自动学习币上限 10000，老师手动发币不计入。
5. **一次性口径**：预习、专项练习、小节通关、消灭知识点、错题订正、章节阶段、任务完成均按学期与内容一次性结算。
6. **兑换规则**：商品兑换规则由各校区自行设定，本特性不修改商城商品与兑换流程。
