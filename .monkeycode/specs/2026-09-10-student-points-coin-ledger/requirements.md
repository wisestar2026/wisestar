# 学员积分·学币计算体系 需求文档

Feature Name: student-points-coin-ledger
Updated: 2026-09-10

## Introduction

学员端「海洋智学」的数值体系当前为两套口径并存：后端 `GET /api/student/stats` 从 `t_practice_record` 派生数值（积分=练习得分合计、学币=按学科答对数、老师手动发币再加总），预习完成靠写一条满分练习记录来发奖；前端 `useStudentStore` 的奖励/头衔为 mock。本特性按需求文档 8.1/8.2/8.3 落地**双轨数值体系**：新增 `t_user_points`（学海积分/头衔，终身全学科）、`t_subject_semester`（学科学期学习币，单科上限 3000）、`t_user_learning_record`（学习行为记录/7 天防刷），并以统一奖励结算服务承接预习、练习、试炼、错题订正、薄弱点复测、每日里程碑、章节阶段等全部行为奖励。本特性只负责「数值账本与结算」，薄弱点研判见配套特性 `student-weakness-evaluation`。

数值体系的两条根本边界：**学习币只用于商品兑换，学海积分只用于荣誉评价**，二者不得交叉使用；奖励规则只依赖「学习行为类型」，不依赖章节/小节/知识点的具体配置，因此教学内容调整时积分与学习币体系保持稳定。同时，数值体系要主动引导学员「预习、练习、订正错题、攻克薄弱点」：个人中心-积分板块简单明了地呈现积分评价细节，学员端主页提供积分获取的引导与提示。

## Glossary

- **学海积分（points）**：全学科学习行为累计的终身成长值；不分科、不清零、无上限；**只用于荣誉评价体系**（头衔晋升、证书解锁、学情评价展示），不参与任何兑换。
- **学习币（coins）**：按学科独立产出的兑换货币；单科单学期上限 3000；学期清零；**只用于商城商品兑换**，多科合并扣款，不参与头衔/证书等荣誉判定。
- **学期（semester）**：学习币的有效周期键，格式 `{学年}-{上/下}`（如 `2026-1` 表示 2026 学年上学期）。
- **学习行为（action_type）**：触发一次奖励结算的行为类型，见 8.2 奖励体系。
- **奖励结算（settle）**：在事务内校验合法性 → 判定是否重复/超限 → 写行为记录 → 累加积分与学习币 → 重算头衔的完整过程。
- **7 天防刷**：同一学员对同一「知识点/目标」的同类学习行为，7 个自然日内仅首次结算奖励。
- **单科上限（reached_limit）**：某学科学期学习币达到 3000 后置位；此后该学科继续产积分但不产币。
- **头衔（title）**：由累计学海积分决定、只升不降的五级称号，属于荣誉评价体系。
- **手动发币**：老师通过 `POST /api/student/coin` 向学员发放的学习币（`t_student_coin`），计入学员总学习币展示。
- **个人中心-积分板块**：学员端个人中心内集中展示学海积分评价的板块，简单明了地呈现当前积分、头衔、下一头衔进度、积分获取规则与最近积分明细。
- **积分获取引导**：学员端主页向学员展示「今天可以做哪些事获得积分/学习币」以及完成进度的提示模块。

## Requirements

### R1. 双轨数值账本持久化

**User Story:** AS 学员，I want 拥有终身累计的学海积分和按学科分账的学习币，SO THAT 成长值与兑换货币各自独立、口径清晰。

#### Acceptance Criteria

1. the system SHALL 为每位学员维护唯一一条 `t_user_points`（`user_id / points / title_level / title_name`），`points` 为该学员全学科累计学海积分。
2. the system SHALL 按 `user_id + subject_id + semester` 维护 `t_subject_semester`（`coins / reached_limit`），同一组合仅一条有效记录。
3. WHEN 学员完成一次学习行为结算，the system SHALL 在同一事务内递增 `t_user_points.points` 与对应 `t_subject_semester.coins`。
4. the system SHALL 使学海积分不随学期变化、学习币按学期分账。

### R2. 统一奖励结算

**User Story:** AS 学员，I want 每次有效学习行为按定稿规则获得学习币与学海积分，SO THAT 激励可预期。

#### Acceptance Criteria

1. WHEN 学员触发预习完成，the system SHALL 按「币 +5 / 积分 +3」结算。
2. WHEN 学员完成专项练习，the system SHALL 按「币 +12 / 积分 +6」结算。
3. WHEN 学员完成试炼检测，the system SHALL 按「币 +20 / 积分 +10」结算；IF 该次试炼正确率 ≥90%，the system SHALL 追加「币 +15 / 积分 +8」。
4. WHEN 学员完成全套错题订正，the system SHALL 按「币 +8 / 积分 +4」结算。
5. WHEN 学员薄弱知识点复测达标，the system SHALL 按「币 +25 / 积分 +12」结算。
6. WHEN 学员达成每日里程碑（完成 2 个知识点练习 / 订正错题 ≥3 道 / 有效学习 30 分钟），the system SHALL 分别按「+8币+5分 / +6币+3分 / +7币+4分」结算，且每日各里程碑仅结算一次。
7. WHEN 学员在某学科累计达成第 N 个达标章节（N=1..6，6 及以上按 6 计），the system SHALL 按「80/100/120/150/180/200 币」与「40/50/60/75/90/100 积分」结算该阶段奖励，且每个阶段里程碑终身仅结算一次。
8. the system SHALL 以 `action_type + 业务关联ID（ref_id）` 作为一次结算事件的幂等键，同一事件重复提交不重复发放奖励。
9. the system SHALL 忽略未登录或非学员身份的结算请求。

### R3. 7 天防刷

**User Story:** AS 系统，I want 拦截短期内对同一目标的重复学习奖励，SO THAT 奖励不被刷取。

#### Acceptance Criteria

1. WHEN 学员对同一「知识点」重复触发同类学习行为，IF 距该行为最近一次成功结算不足 7 个自然日，the system SHALL 本次结算奖励为 0 并返回 `firstTime=false`。
2. WHILE 学员距上次同类行为已达 7 个自然日，the system SHALL 正常发放奖励。
3. the system SHALL 通过 `t_user_learning_record` 统一判定防刷，并对每日里程碑按自然日去重、对章节阶段里程碑按终身去重。
4. IF 学习目标为空（无法定位知识点/目标），the system SHALL 拒绝结算并返回校验错误。

### R4. 单科上限与学期清零

**User Story:** AS 系统，I want 控制单科学习币上限与学期清零，SO THAT 数值规则与产品定稿一致。

#### Acceptance Criteria

1. WHILE 某学科学期学习币未达 3000，the system SHALL 按结算结果累加该学科学习币。
2. IF 某次结算将使该学科学期学习币超过 3000，the system SHALL 仅发放至上限、置 `reached_limit=true`，并提示「本学科本学期学习币已达上限，可继续学习积累学海积分」。
3. WHILE 某学科 `reached_limit=true`，the system SHALL 继续发放该行为对应的学海积分、并将该次学习币发放量记为 0。
4. WHEN 学习币跨入新学期的学期键，the system SHALL 使该学科学习币从 0 起算（新学期新记录）。
5. the system SHALL 提供学期键计算能力，默认上学期为 9 月 1 日至次年 1 月 31 日、下学期为 2 月 1 日至 8 月 31 日。

### R5. 头衔自动晋升

**User Story:** AS 学员，I want 学海积分达到阈值时自动晋升头衔，SO THAT 长期成长有荣誉反馈。

#### Acceptance Criteria

1. the system SHALL 按学海积分阈值判定头衔：初探者 0、勤学者 ≥300、深耕者 ≥1600、善思者 ≥5500、领航者 ≥9000。
2. WHEN 一次结算使累计学海积分达到更高阈值，the system SHALL 升级 `title_level/title_name` 并在结算响应中返回 `titleUpgraded=true`。
3. WHILE 学海积分回落（如数据修正），the system SHALL 保持已达成头衔不降级。

### R6. 数值查询接口

**User Story:** AS 学员，I want 查询我的积分、学习币与今日数据，SO THAT 首页与档案展示真实数值。

#### Acceptance Criteria

1. WHEN 学员请求 `GET /api/student/stats`，the system SHALL 返回 `totalPoints`（来自 `t_user_points`）、`coinsBySubject`（来自 `t_subject_semester` + 手动发币）、`manualCoins`、`today`（当日积分/学习币/题量/时长）及累计题量正确率。
2. WHEN 学员请求 `GET /api/student/profile`，the system SHALL 返回姓名、头像、头衔、总积分、证书数。
3. WHEN 学员请求 `GET /api/student/coins`，the system SHALL 返回本学期各科学习币与 `limit=3000`。
4. the system SHALL 保持 `GET /api/student/stats` 现有响应字段结构，使前端不因字段重命名而中断。

### R7. 统一结算入口

**User Story:** AS 学员端，I want 一个统一的奖励结算接口，SO THAT 预习/练习/试炼/错题等行为复用同一套结算逻辑。

#### Acceptance Criteria

1. WHEN 学员请求 `POST /api/student/learning/complete`，the system SHALL 依据 `{kpId, actionType, durationMs}` 执行对应结算并返回 `{ok, coins, points, titleUpgraded, coinsCapped}`。
2. the system SHALL 仅在服务端计算与发放奖励，前端只展示结算结果。
3. IF `actionType` 非法或与当前上下文不匹配，the system SHALL 返回校验错误且不写任何账本。

### R8. 预习完成迁移到账本

**User Story:** AS 学员，I want 预习完成由账本统一记账并保留完成状态，SO THAT 不再以伪造练习记录发奖。

#### Acceptance Criteria

1. WHEN 学员请求 `POST /api/student/preview/complete`，the system SHALL 通过统一结算发放预习奖励，并写 `action_type=preview` 的 `t_user_learning_record` 作为完成与防刷依据。
2. WHEN 学员查询知识点详情/进度，the system SHALL 返回该知识点是否已预习完成（依据 `t_user_learning_record`）。
3. the system SHALL 在迁移后停止为预习写入 `t_practice_record` 的 `mode=preview` 记录。
4. IF 同一知识点 7 日内重复预习，the system SHALL 返回 `firstTime=false` 且不重复发放。

### R9. 老师手动发币并入总账

**User Story:** AS 学员，I want 老师手动发放的学习币仍计入我的总学习币，SO THAT 线下奖励不丢失。

#### Acceptance Criteria

1. WHEN 老师请求 `POST /api/student/coin` 发放学习币，the system SHALL 写入 `t_student_coin` 并保留原因与操作人。
2. WHEN 学员查询总学习币，the system SHALL 返回 `t_subject_semester` 学习币与 `t_student_coin` 手动发币之和。
3. the system SHALL 允许手动发币记录携带可选 `subject_id`，用于按学科归集。

### R10. 前端奖励与数值真实化

**User Story:** AS 学员，I want 前端展示真实的积分、学习币、头衔与奖励到账结果，SO THAT mock 数值不再误导。

#### Acceptance Criteria

1. WHEN 学员端首页/档案/研习/知识点页加载，the system SHALL 使用接口返回的真实积分、学习币、头衔与奖励结果渲染。
2. WHEN 一次学习行为结算成功，the system SHALL 即时展示实际到账的币与积分（含防刷 `firstTime=false`、上限 `coinsCapped` 的提示）。
3. IF 数值接口异常，the system SHALL 回退展示上次缓存值并提示加载失败。

### R11. 用途边界（学习币兑换、学海积分荣誉）

**User Story:** AS 学员，I want 学习币与学海积分各司其职，SO THAT 兑换与荣誉互不影响。

#### Acceptance Criteria

1. the system SHALL 仅允许学习币用于商城商品兑换扣款，并禁止学海积分参与任何兑换。
2. the system SHALL 仅允许学海积分用于头衔晋升、证书解锁与学情评价展示，并禁止学习币影响头衔或证书判定。
3. the system SHALL 在数值接口与前端展示中明确区分「学习币（可兑换）」与「学海积分（荣誉值）」的用途文案。
4. IF 请求尝试以学海积分兑换商品或以学习币晋升头衔，the system SHALL 拒绝并返回校验错误。

### R12. 个人中心-积分板块

**User Story:** AS 学员，I want 在个人中心简单明了地看到我的积分评价，SO THAT 我清楚自己的荣誉水平与提升方向。

#### Acceptance Criteria

1. WHEN 学员请求 `GET /api/student/points`，the system SHALL 返回当前学海积分、当前头衔、下一头衔名称与所需积分、距离下一头衔的差值。
2. the system SHALL 在同一响应中返回积分获取规则列表（行为名称、可获得积分、可获得学习币）。
3. the system SHALL 在同一响应中返回最近积分明细（行为名称、本次积分、发生时间），按时间倒序，默认最多 20 条。
4. WHEN 学员进入个人中心-积分板块，the system SHALL 以「当前积分 + 头衔进度条 + 规则说明 + 最近明细」的结构简单明了地呈现，不堆砌技术字段。
5. IF 学员暂无积分明细，the system SHALL 展示引导文案而非空白。

### R13. 学员端主页积分获取引导

**User Story:** AS 学员，I want 主页提示我如何获得积分与学习币，SO THAT 我主动开始学习。

#### Acceptance Criteria

1. WHEN 学员进入主页，the system SHALL 展示积分获取引导模块，列出当天可获得积分/学习币的行为及其完成状态与进度。
2. the system SHALL 使引导项覆盖「预习、专项练习、试炼、错题订正、薄弱点攻克、每日里程碑」中当天可执行的行为。
3. WHEN 学员完成某引导项，the system SHALL 更新该项为已完成并展示本次到账的积分与学习币。
4. WHEN 学员点击引导项，the system SHALL 跳转到对应学习入口。
5. IF 学员当天已无可获取积分的可执行行为，the system SHALL 展示鼓励文案。

### R14. 内容变化下的体系稳定性

**User Story:** AS 运营人员，I want 章节/小节/知识点调整后积分与学习币体系不变，SO THAT 内容维护不影响学员既有数值。

#### Acceptance Criteria

1. the system SHALL 使奖励金额只由学习行为类型决定，且与章节/小节/知识点的存在与配置无关。
2. WHEN 章节/小节/知识点新增、修改、删除或移动，the system SHALL 保持学员已累计的学海积分与学习币不变。
3. the system SHALL 以学习行为账本作为积分与学习币的唯一事实来源，数值查询不依赖对当前教学内容结构的重新扫描。
4. the system SHALL 将内容引用（学科/知识点等标识）作为不可变的记录保存，内容调整不级联修改或删除历史行为记录。
5. WHILE 教学内容结构变化，the system SHALL 保持奖励规则常量、防刷维度与头衔阈值不变。

## 边界约束（Non-Functional）

1. 奖励计算与发放仅在后端服务层执行，前端不得参与金额判定。
2. 所有结算写操作使用事务，保证账本一致性；幂等键防止重复发放。
3. `t_user_points`、`t_subject_semester`、`t_user_learning_record` 均遵循项目 `BaseModel`（`is_deleted` 逻辑删除、`create_at/by`、`update_at/by`）约定。
4. 结算涉及的学科、知识点必须落在学员有效订单权限（`t_student_permission`，`expire_at > NOW()`）内。
5. 并发下同一学员同一目标的结算必须串行化，避免 7 天防刷并发穿透与积分重复累加。

## Open Questions（待确认）

1. 学期键的默认口径（上学期 9/1–1/31、下学期 2/1–8/31）是否符合学校实际校历？是否需要可配置起止日期？
2. 章节阶段奖励「第 N 章」按学科内累计达标章节数计算，是否符合产品预期？
3. 每日里程碑在缺少 `t_user_task`（任务体系另行设计）时，是否由行为记录实时聚合触发？每日有效学习 30 分钟的时长口径是否取 `t_practice_record.duration_ms` 当日累计？

## 已确认决策（2026-09-10）

1. **账本模型（三表全实现）**：按需求文档落地 `t_user_points`（终身积分/头衔）+ `t_subject_semester`（分科学币/上限 3000/学期清零）+ `t_user_learning_record`（行为记录/7 天防刷），替代当前从 `t_practice_record` 派生的口径。
2. **奖励范围**：一次性覆盖 8.2 全部 9 类行为 + 章节阶段奖励，不再只实现预习。
3. **预习迁移**：预习完成改为写行为记录并发奖，停止写伪造练习记录。
4. **手动发币保留**：`t_student_coin` 保留为老师手动发币来源，并入总学习币展示。
5. **评级/头衔口径**：头衔阈值按 8.2 定稿；掌握度五级评级（精通/熟练/夯实/待巩固/待攻克）见配套特性。

## 已确认决策（2026-09-10 第二轮）

1. **用途分离**：学习币仅用于商品兑换；学海积分仅用于荣誉评价体系，二者不交叉。
2. **积分展示**：积分评价细节在个人中心-积分板块简单明了呈现（当前积分、头衔进度、规则、最近明细）。
3. **主页引导**：学员端主页提供积分获取引导/提示，指向预习、练习、错题、薄弱点等主动学习行为。
4. **体系稳定**：章节/小节/知识点可变，积分与学习币体系保持稳定；奖励规则只绑定行为类型，账本为唯一事实源，内容调整不影响既有数值。
5. **行为导向**：积分与学习币设计用于引导学员主动学习、练习、解决错题。
