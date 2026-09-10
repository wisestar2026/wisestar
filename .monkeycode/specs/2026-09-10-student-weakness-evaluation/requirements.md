# 学员薄弱点·学习结果评价体系 需求文档

Feature Name: student-weakness-evaluation
Updated: 2026-09-10

## Introduction

学员端的错题本已基于真实落库数据（`t_practice_detail` 逐题明细聚合）运行，但「知识点掌握度 `mastery`」「薄弱板块」「五级评级」「学情总览」仍来自前端 `useStudentStore` 的 mock，且与错题本数据互不相通：掌握度口径为前端写死、薄弱点无研判规则、达标章节口径与正确率口径不一致。本特性以 `t_practice_detail` 为唯一数据源，落地 `t_user_knowledge_progress`（知识点掌握度）与 `t_user_weak_knowledge`（薄弱点研判）两表，定义可解释的掌握度算法、五级评级、薄弱点自动研判与攻克消除规则，并接入交卷链路与学员端页面，使「错题本—掌握度—薄弱点—学情总览」同源一致。奖励发放（薄弱点攻克 +25币/+12分）由配套特性 `student-points-coin-ledger` 结算。

## Glossary

- **掌握度（mastery）**：0–100 的整数，表示学员对某知识点的近期掌握水平，由练习明细正确率加权计算。
- **五级评级（level）**：由掌握度映射的等级标签：精通（≥85）/ 熟练（≥70）/ 夯实（≥55）/ 待巩固（≥40）/ 待攻克（<40）。
- **薄弱知识点**：掌握度低于阈值，或存在未订正错题的活跃知识点。
- **攻克（conquer）**：薄弱知识点经专项练习达标后消除薄弱标记；`status` 由 `active` 变为 `conquered`。
- **未订正错题**：`t_practice_detail.is_correct=0` 且 `corrected=0` 的题目记录。
- **达标章节（章节 progress）**：章节内知识点学习进度达到阈值；沿用研习页现有 progress 口径。
- **同源一致**：掌握度、薄弱点、错题本、学情总览均由 `t_practice_detail`/`t_practice_record` 派生，不引入第二套口径。
- **评价更新（recordPractice）**：一次交卷落库后，按题目所属知识点刷新掌握度与薄弱状态的内部过程。

## Requirements

### R1. 知识点掌握度真实计算

**User Story:** AS 学员，I want 掌握度来自我的真实练习表现，SO THAT 评价反映实际水平。

#### Acceptance Criteria

1. WHEN 一次练习/试炼交卷落库成功，the system SHALL 按题目所属知识点刷新对应 `t_user_knowledge_progress` 的掌握度、练习次数与最近正确率。
2. the system SHALL 以该知识点最近若干次练习明细的正确率为基础，按「越近权重越高」计算掌握度并取整到 0–100。
3. the system SHALL 累计该知识点的练习会话次数 `times`、总题数、正确题数，并记录 `last_correct_rate`、`last_practice_at`。
4. IF 一次交卷涉及多个知识点，the system SHALL 分别更新每个知识点，单题无知识点的明细 SHALL 跳过而不报错。
5. the system SHALL 使 `t_user_knowledge_progress` 按 `user_id + subject_id + version_id + knowledge_point_id` 唯一。

### R2. 五级评级

**User Story:** AS 学员，I want 掌握度有清晰的等级标签，SO THAT 我能快速判断学习状态。

#### Acceptance Criteria

1. the system SHALL 按掌握度映射五级评级：精通 ≥85、熟练 ≥70、夯实 ≥55、待巩固 ≥40、待攻克 <40。
2. WHEN 掌握度更新，the system SHALL 同步更新该知识点的等级标签供接口返回。
3. the system SHALL 保持等级阈值与前端 `LEVELS` 定义一致。

### R3. 薄弱点自动研判

**User Story:** AS 学员，I want 系统自动识别我的薄弱知识点，SO THAT 我能靶向补弱。

#### Acceptance Criteria

1. WHEN 一次交卷刷新某知识点掌握度，IF 掌握度低于 55，the system SHALL 将该知识点置为薄弱（`t_user_weak_knowledge.status=active`）。
2. WHEN 某知识点存在未订正错题，the system SHALL 将该知识点置为薄弱。
3. the system SHALL 按 `user_id + subject_id + knowledge_point_id` 唯一维护薄弱记录，并记录 `first_weak_at`。
4. WHILE 知识点已攻克，IF 后续练习使掌握度再次低于 55 或产生未订正错题，the system SHALL 将其重新置为 `active`。
5. the system SHALL 仅对学员有效订单权限内的学科知识点进行研判。

### R4. 薄弱点清单与攻克

**User Story:** AS 学员，I want 查看薄弱清单并提交达标练习消除薄弱标记，SO THAT 补弱有闭环。

#### Acceptance Criteria

1. WHEN 学员请求 `GET /api/student/knowledge/weak-list`，the system SHALL 返回全部 `status=active` 的薄弱知识点（含名称、学科、攻克奖励）。
2. WHEN 学员请求 `POST /api/student/weak/conquer`，IF 提交正确率达到达标阈值（≥80%），the system SHALL 将该知识点置为 `conquered`、`conquer_times+1`、记录 `conquered_at`，并返回 `weakCleared=true`。
3. WHEN 薄弱点由 `active` 首次变为 `conquered`，the system SHALL 结算薄弱点复测达标奖励（币 +25 / 积分 +12），同一知识点的攻克奖励仅发放一次。
4. IF 提交正确率未达阈值，the system SHALL 保持 `active` 并返回 `weakCleared=false`。
5. the system SHALL 在攻克后从短板清单中移除该知识点。

### R5. 错题重做与自动消除

**User Story:** AS 学员，I want 重做错题正确后自动移除并更新掌握度，SO THAT 错题本与掌握度联动。

#### Acceptance Criteria

1. WHEN 学员请求 `POST /api/student/wrong/redo` 且作答正确，the system SHALL 标记该题对当前学员已订正，并在错题本聚合中移除。
2. WHEN 错题订正导致该知识点不再存在未订正错题且掌握度不低于 55，the system SHALL 将该知识点移出薄弱。
3. IF 重做作答错误，the system SHALL 保留错题标记且不移除。
4. WHEN 学员请求 `GET /api/student/wrong-list`，the system SHALL 仅返回未订正错题。
5. the system SHALL 将错题订正行为交由配套奖励体系结算（币 +8 / 积分 +4）。

### R6. 学情总览同源一致

**User Story:** AS 学员，I want 学情总览与掌握度、错题本一致，SO THAT 数据可信。

#### Acceptance Criteria

1. WHEN 学员请求 `GET /api/student/stats`，the system SHALL 返回累计知识点数 `kps`、达标章节数 `chapters`、薄弱知识点数 `weak` 与总积分 `points`。
2. the system SHALL 使 `weak` 与薄弱清单条数一致、`kps` 与掌握度表记录一致、`chapters` 与研习页章节 progress 口径一致。
3. the system SHALL 使 `points` 取自积分账本（配套特性）。

### R7. 评价查询接口

**User Story:** AS 学员端，I want 通过接口获取进度、知识点详情与薄弱清单，SO THAT 页面渲染真实评价。

#### Acceptance Criteria

1. WHEN 学员请求 `GET /api/student/study/progress?subjectId=&versionId=`，the system SHALL 返回章节及其知识点的 `mastery / level / weak`，且按学科+版本独立。
2. WHEN 学员请求 `GET /api/student/knowledge/detail?kpId=`，the system SHALL 返回该知识点 `mastery / level / weak` 及是否已预习。
3. IF 知识点尚无练习数据，the system SHALL 返回掌握度 0、等级「待攻克」，不返回 mock。
4. the system SHALL 统一 `@PreAuthorize("isAuthenticated()")` 并校验学员身份与订单权限。

### R8. 前端评价真实化

**User Story:** AS 学员，I want 研习页与知识点页展示真实掌握度、评级与薄弱标记，SO THAT mock 不再误导。

#### Acceptance Criteria

1. WHEN 研习页/知识点页加载，the system SHALL 使用接口返回的 `mastery/level/weak` 渲染，不再引用 mock `chapters[].kps`。
2. WHEN 掌握度或薄弱状态变化，the system SHALL 在下次进入页面时刷新展示。
3. IF 评价接口异常，the system SHALL 回退展示上次缓存并提示加载失败，不静默使用 mock。

### R9. 内容变化下的评价稳定性与荣誉用途

**User Story:** AS 运营人员/学员，I want 掌握度与薄弱点不因教学内容调整而失真，且积分只作荣誉评价，SO THAT 学习评价长期可信。

#### Acceptance Criteria

1. WHEN 章节/小节/知识点新增、修改、删除或移动，the system SHALL 保持学员已有的掌握度与薄弱状态记录不被级联修改或删除。
2. the system SHALL 以练习明细与评价值账本为事实来源，评价查询不因当前教学内容结构变化而重算历史结论。
3. the system SHALL 使学海积分仅用于荣誉评价（头衔、证书、学情评价展示），学习币仅用于商品兑换，二者不交叉。
4. WHILE 教学内容结构变化，the system SHALL 保持掌握度算法、评级阈值与薄弱/攻克判定规则不变。
5. IF 某知识点被移除，the system SHALL 在相关清单中按逻辑删除语义停用该条目，历史评价记录保留。

## 边界约束（Non-Functional）

1. `t_practice_detail` 为掌握度与错题本的唯一数据源，评价体系不引入第二套判分口径。
2. 评价更新在交卷事务完成后执行，评价异常不阻断交卷判分结果落库。
3. `t_user_knowledge_progress`、`t_user_weak_knowledge` 遵循项目 `BaseModel` 约定。
4. 掌握度算法必须可解释、可复现，权重与窗口参数集中配置。
5. 薄弱点研判与攻克需校验知识点落在学员有效订单权限内。

## Open Questions（待确认）

1. 掌握度窗口取「最近 5 次练习」及权重（5,4,3,2,1）是否为最终口径？是否需要改为时间窗（如 30 天）？
2. 薄弱阈值取掌握度 <55（对应「待巩固/待攻克」）是否符合产品预期？是否使用 <60？
3. 攻克达标阈值取正确率 ≥80%、掌握度 ≥70 是否合适？
4. 错题订正标记落在 `t_practice_detail`（新增 `corrected/corrected_at`）是否可接受，还是新建订正记录表？

## 已确认决策（2026-09-10）

1. **数据源（基于 t_practice_detail）**：掌握度与薄弱点以学员真实练习/错题明细聚合，与错题本同源；不接入 `AnalysisApi`（`t_answer_detail`）避免双口径。
2. **两表落地**：`t_user_knowledge_progress`（知识点掌握度）+ `t_user_weak_knowledge`（薄弱点研判）。
3. **五级评级**：沿用前端现有 `LEVELS` 阈值（85/70/55/40）。
4. **攻克奖励**：薄弱点首次攻克结算 +25 币/+12 分，由积分账本特性执行，本特性只负责状态流转。
5. **范围**：AI 讲解接入、任务体系（`t_user_task`）、证书体系不在本特性范围内。

## 已确认决策（2026-09-10 第二轮）

1. **错题现状无问题**：错题本（`t_practice_detail` 聚合）与错因归因保持现有实现，不作为本轮修改点。
2. **积分荣誉用途**：学海积分仅用于荣誉评价体系；本特性产出的掌握度/薄弱点作为荣誉评价与学情展示的数据来源。
3. **体系稳定**：章节/小节/知识点可变，掌握度算法、评级阈值与薄弱/攻克规则保持稳定，历史评价记录不因内容调整而改变。
4. **引导主动学习**：评价结果用于引导学员练习与攻克薄弱点，具体奖励由积分账本特性结算。
