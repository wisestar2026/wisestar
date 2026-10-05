# 学员成长档案（学前检测 + 学习轨迹 + 成长报告）需求文档

Feature Name: student-growth-archive
Updated: 2026-10-04

## Introduction

现有「知识点检测」（`DetectionServiceImpl`）为诊断性质，提交后仅在内存聚合出报告返回前端，不落库、不发放奖励、不写学习进度；现有「学员学习档案」（`StudentArchiveServiceImpl` + `t_student_archive` / `t_student_archive_record`）按「学员+学期」建档，承载目标规划表、承诺书、上课记录与学期报告，`profileSnapshot` 当前取自练习产生的薄弱点，新生尚未练习时为空。本特性在不改变检测诊断语义与既有掌握度口径的前提下，把学员某学科本学期的**第一次全面检测**定位为「学前检测」，定格薄弱基线；将每一次学习行为留痕为**学习轨迹事件**；以「基线 → 当前」的成长对比生成**面向家长、人性化、可打印**的成长报告，供老师在后台设定学期目标规划。

## Glossary

- **全面检测**：学员在检测页选定学科、年级、册别、单元与题量后作答并判分的诊断活动，复用 `DetectionService`。
- **学前检测（PRE）**：学员在某学科某学期的第一次全面检测，用于定格本学期成长基线。
- **阶段检测（STAGE）**：同学科本学期除第一次以外的全面检测，用于阶段对比。
- **成长基线（baseline）**：学前检测产出的单元正确率与薄弱知识点集合，作为本学期成长对比的参照。
- **学习轨迹事件（growth event）**：一次学习行为对应的一条留痕记录，含学科、类型、时间、单元、知识点与量化指标。
- **成长报告**：以成长基线、成长对比、学习轨迹与目标规划为输入生成的家长可读报告，可打印交付。
- **掌握度（mastery）**：0–100 整数，表示对某知识点的近期掌握水平，沿用既有算法与阈值。
- **档案学习记录**：`t_student_archive_record` 中的一条，按「档案+日期」组织，承载当次学习叙事。
- **精准破弱模型（WPB）**：以薄弱知识点为核心的学情闭环模型，见 `weak-point-breakthrough-model.md`。
- **薄弱点事件（weak point event）**：某知识点被识别为薄弱、被攻克或被复现的一次留痕，含类型、时间、掌握度与来源。
- **薄弱标记（weak mark）**：薄弱知识点向上聚合到所属小节/章节后的可见标识，用于专攻加权与界面提示。
- **薄弱点专攻（targeted drill）**：对标记为薄弱的小节/知识点提高组卷抽取权重，并在章节达成率中提高其权重。

## Requirements

### R1. 学前检测的定位与学期基线定格

**User Story:** AS 学员/学管师，I want 本学期第一次检测被自动识别为学前检测并定格基线，SO THAT 后续成长有可对比的起点。

#### Acceptance Criteria

1. WHEN 学员在某学科本学期首次提交全面检测且判分完成，the system SHALL 将本次检测记录类型标记为学前检测（PRE）。
2. WHEN 学员在同一学科本学期再次提交全面检测，the system SHALL 将本次检测记录类型标记为阶段检测（STAGE）。
3. WHEN 学前检测判分完成，the system SHALL 将该次检测的单元正确率与薄弱知识点集合定格为该学员该学科本学期的成长基线。
4. the system SHALL 按「学员 + 学科 + 学期」维护唯一成长基线，同一学期内重复请求 SHALL 返回既有基线。
5. the system SHALL 保持检测的诊断语义：学前检测与阶段检测均不发放学海积分与学习币，且 SHALL 保持既有掌握度算法不变。
6. WHEN 学前检测（`is_baseline=1`）判分完成，the system SHALL 将本次检测的薄弱知识点一次性写入当前薄弱系统（`t_user_weak_knowledge`），并为每个薄弱点追加一条 `discovered` 事件；此后同一学员该学科再次检测 SHALL 仅作为普通练习数据，不重写当前薄弱系统（口径 a）。

### R2. 检测记录持久化与历史查询

**User Story:** AS 学员/老师，I want 每次检测都被持久化并可查询，SO THAT 检测构成可追溯的成长节点。

#### Acceptance Criteria

1. WHEN 一次全面检测判分完成，the system SHALL 持久化一条检测记录，包含学员、学科、学期、检测类型、所选单元、题量、正确题数、正确率、薄弱知识点集合与逐题结果。
2. WHEN 学员请求本人检测历史，the system SHALL 按检测时间倒序返回该学员该学科本学期的全部检测记录，含检测类型、正确率与薄弱点摘要。
3. WHEN 管理端请求某学员检测历史且请求者拥有 `student:archive` 权限，the system SHALL 返回对应检测记录。
4. IF 检测请求中的某题目不存在，the system SHALL 忽略该题并继续处理其余题目。

### R3. 学习轨迹事件流水

**User Story:** AS 学员/家长，I want 每一次学习都被留痕，SO THAT 学习成长轨迹完整可查。

#### Acceptance Criteria

1. WHEN 学员完成一次已落库的学习行为（在线练习、单元检测、英语语法练习、上课记录），the system SHALL 追加一条学习轨迹事件。
2. the system SHALL 使学习轨迹事件包含学员、学科、事件类型、发生时间、单元、知识点、题量、正确数、正确率、时长、积分与学习币。
3. the system SHALL 以「业务来源 + 业务ID」保证同一学习行为只产生一条事件。
4. WHEN 学员请求本人轨迹时间轴，the system SHALL 按发生时间倒序返回指定学科与指定时间范围内的轨迹事件。
5. the system SHALL 使轨迹事件写入独立于交卷主事务，IF 事件写入失败，the system SHALL 记录失败日志，并 SHALL 使轨迹可由来源业务数据（练习记录、检测记录、上课记录）重建。

### R4. 档案学习记录的自动留痕

**User Story:** AS 老师，I want 系统自动生成学习记录，SO THAT 我只需补充点评而无需逐条手工填写。

#### Acceptance Criteria

1. WHEN 学习轨迹事件产生，the system SHALL 按「档案 + 日期」自动生成或更新一条档案学习记录。
2. the system SHALL 使档案学习记录包含系统生成的家长可读叙事摘要、覆盖知识点与当次进步量。
3. WHILE 老师未编辑该记录，the system SHALL 保持记录为草稿状态；WHEN 老师定稿，the system SHALL 将记录状态标记为已定稿。
4. IF AI 文本能力不可用，the system SHALL 使用增强规则模板生成摘要，该摘要 SHALL 包含具体知识点名称与量化进步。

### R5. 成长对比

**User Story:** AS 家长/老师，I want 看到每个知识点的成长变化，SO THAT 能直观判断学习效果。

#### Acceptance Criteria

1. WHEN 生成成长报告，the system SHALL 以成长基线为参照，逐知识点计算「基线掌握度 → 当前掌握度」的变化量。
2. the system SHALL 为每个基线薄弱知识点标记报告生成时的状态：已攻克、持续巩固或仍薄弱。
3. WHEN 某基线薄弱知识点的当前掌握度达到 70 且最近一次练习正确率达到 80%，the system SHALL 在成长对比中标记为已攻克。
4. the system SHALL 计算并展示整体进步指标：平均掌握度变化、薄弱点数量变化、累计学习时长与累计练习量。

### R6. 成长报告的生成与人性化

**User Story:** AS 家长，I want 报告用可读的语言描述孩子的成长，SO THAT 我能理解并据此配合辅导。

#### Acceptance Criteria

1. WHEN 老师或学员触发成长报告生成，the system SHALL 汇总成长基线、成长对比、学习轨迹与目标规划形成报告输入。
2. the system SHALL 优先调用 AI 生成面向家长的叙事报告；IF AI 不可用或调用失败，the system SHALL 降级为规则模板报告。
3. the system SHALL 使成长报告包含基线学情、本学期进步、仍需巩固、下阶段建议与教师寄语五个部分。
4. the system SHALL 区分报告草稿与定稿状态，WHILE 报告为草稿，the system SHALL 允许重新生成。
5. the system SHALL 记录报告的生成时间与生成模型。

### R7. 报告打印与交付

**User Story:** AS 学管师，I want 能将成长报告打印交付家长，SO THAT 交付物正式且美观。

#### Acceptance Criteria

1. WHEN 用户点击打印，the system SHALL 以浏览器打印样式输出成长报告，包含学员姓名、学期、学科与生成日期。
2. the system SHALL 使打印样式按区块排版，且 SHALL 隐藏交互按钮与页面导航。
3. WHILE 报告为草稿状态，the system SHALL 提示用户定稿后再交付家长。

### R8. 老师目标规划联动

**User Story:** AS 老师，I want 依据学前基线与成长对比制定学期目标，SO THAT 目标规划有数据依据。

#### Acceptance Criteria

1. WHEN 老师进入某学员档案详情，the system SHALL 展示该学员本学期成长基线的薄弱知识点与当前成长对比。
2. WHEN 老师保存目标规划，the system SHALL 按学期保存目标规划与承诺书。
3. the system SHALL 允许老师在生成成长报告时使用已保存的目标规划作为输入。
4. IF 该学员本学期尚无成长基线，the system SHALL 提示老师先完成学前检测。

### R9. 权限与可见性

**User Story:** AS 系统，I want 不同角色按权限访问成长档案，SO THAT 数据安全可控。

#### Acceptance Criteria

1. the system SHALL 要求学员本人查看本人检测历史、学习轨迹与成长报告时通过 `isAuthenticated()` 校验。
2. the system SHALL 要求管理端查看检测历史、学习轨迹与成长报告时通过 `student:archive` 校验。
3. the system SHALL 要求管理端编辑目标规划与定稿成长报告时通过 `student:archive:edit` 校验。

### R10. 数据稳定性与兼容

**User Story:** AS 运营人员，I want 成长档案不因教学内容调整而失真，SO THAT 历史评价长期可信。

#### Acceptance Criteria

1. WHEN 教学内容（章节、小节、知识点）发生新增、修改、删除或移动，the system SHALL 保持既有成长基线、轨迹事件与报告内容不变。
2. the system SHALL 复用现有答案判分语义与掌握度口径，不引入第二套判分规则。
3. the system SHALL 使新增持久化对象符合项目 `BaseModel` 审计列约定，并遵循统一响应 `{code, data, message}` 与分页约定。

### R11. 薄弱标记与薄弱点专攻加权

**User Story:** AS 学员，I want 系统把注意力引导到我的薄弱处，SO THAT 我练得更有针对性、更快攻克薄弱点。

#### Acceptance Criteria

1. WHEN 某知识点存在 active 薄弱状态，the system SHALL 依据知识点所属关系，在其所属小节与章节上给出薄弱标记与薄弱点数量。
2. WHEN 为学员组卷，the system SHALL 对标记为薄弱的小节/知识点提高题目抽取权重；普通知识点权重为 1，薄弱知识点权重 SHALL 大于 1（默认 2，可配置）。
3. WHEN 计算章节达成率，the system SHALL 采用加权平均：薄弱小节权重为 2、普通小节权重为 1（权重可配置）。
4. WHILE 某小节仍有 active 薄弱知识点，the system SHALL 在小节与章节视图中保持薄弱标记；WHEN 该小节薄弱知识点全部攻克，the system SHALL 移除标记。
5. the system SHALL 提供跨单元薄弱点专攻组卷入口，允许学员对当前 active 薄弱点集中练习。

### R12. 薄弱点变化事件流

**User Story:** AS 学员/老师/家长，I want 看到薄弱点被发现、被攻克、又复现的过程，SO THAT 我能感知薄弱点在逐个减少。

#### Acceptance Criteria

1. WHEN 某知识点首次被判定为薄弱，the system SHALL 追加一条 `discovered` 事件。
2. WHEN 某薄弱知识点被攻克，the system SHALL 追加一条 `conquered` 事件。
3. WHEN 已攻克的知识点再次被判定为薄弱，the system SHALL 追加一条 `reopened` 事件（状态复现，但不重复发放攻克奖励）。
4. the system SHALL 使事件包含学员、学科、知识点、小节、章节、事件类型、发生时掌握度、来源与发生时间。
5. the system SHALL 以「学员 + 知识点 + 事件类型 + 业务来源ID」保证事件幂等。
6. WHEN 学员请求薄弱点变化时间线，the system SHALL 返回按发生时间倒序的事件列表。

### R13. 攻克奖励

**User Story:** AS 学员，I want 攻克薄弱点后得到奖励，SO THAT 攻克行为被正向强化。

#### Acceptance Criteria

1. WHEN 学员攻克一个薄弱知识点，the system SHALL 发放 25 学习币 + 12 学海积分，且 SHALL 保证同一知识点终身只发放一次。
2. WHEN 某小节的全部薄弱知识点（含该小节历史上曾薄弱的知识点）均被攻克且该小节无 active 薄弱知识点，the system SHALL 判定该小节攻克，并发放 40 学习币（0 积分），且 SHALL 保证同一小节终身只发放一次。
3. the system SHALL 不设置章节级攻克奖励。
4. the system SHALL 复用既有 `RewardService` 结算与发放通道，仅调整薄弱攻克类行为的幂等范围（由学期一次改为终身一次）。

### R14. 薄弱对比与呈效

**User Story:** AS 家长/老师，I want 对比入学基线与当前薄弱点，SO THAT 能直观看到薄弱点逐个减少。

#### Acceptance Criteria

1. WHEN 请求薄弱对比，the system SHALL 以基线检测记录为参照，逐知识点给出「基线 → 当前」状态：已攻克 / 持续巩固 / 仍薄弱。
2. the system SHALL 计算并展示薄弱点数量变化与薄弱点减少趋势。
3. the system SHALL 复用成长档案规格的成长对比与成长报告能力承载本模型的呈效，不另建第二套报告。
4. the system SHALL 保持成长报告可打印交付（复用浏览器打印样式）。

### R15. 本期不做

1. the system SHALL NOT 引入系统"提分"数值或自动提分评测。
2. the system SHALL NOT 提供章节级攻克奖励。
3. the system SHALL NOT 在本期引入独立家长登录门户与自动推送，画像可见性复用现有档案查看/打印。

## 边界约束（Non-Functional）

1. 学习轨迹事件表按「学员 + 发生时间」建索引，满足单学员轨迹查询。
2. 检测落库与轨迹事件写入保持幂等，重复请求不产生重复数据。
3. AI 调用设置连接与读取超时，超时或失败时降级规则模板，不阻断用户操作。
4. 精准破弱模型（WPB）的薄弱事件仅在状态跃迁（discovered / conquered / reopened）时产生并批量写入，写入发生在主事务提交之后，失败仅记录日志、不阻断交卷与订正。
5. 薄弱事件与攻克奖励以业务唯一键幂等，并发重复由数据库唯一约束兜底。
6. 薄弱点专攻加权在内存完成，不新增逐题数据库查询；加权权重可配置，高负载时可关闭并退回原组卷逻辑。
7. 事件冗余存储知识点/小节/章节名称快照，教学内容删除后展示可降级、不报错。
8. 成长基线以唯一键保证「学员 + 学科 + 学期」至多一条，检测落库失败不阻断诊断报告返回。

## 已确认决策（2026-10-04）

1. 采用方案二：学前检测锚点 + 学习轨迹事件流水 + 阶段成长对比报告。
2. 检测保持诊断性质，不发放奖励，不改写练习掌握度；成长基线独立于练习进度。
3. 复用现有知识点与掌握度口径，不引入第二套判分规则。
4. 学前检测触发方式：系统自动判定——学员某学科本学期首次提交全面检测即为学前检测，无需老师在后台额外发起学期检测任务。
5. 学习轨迹覆盖范围：先覆盖已落库的练习类学习行为——在线练习、单元检测、英语语法练习、上课记录；英语单词/句子学习与在线时长暂不纳入本期。
6. 成长报告交付形态与生成时机：复用浏览器打印样式，报告由老师或学员手动触发生成；本期不提供服务端 PDF 导出与阶段自动生成。

## 已确认决策（2026-10-05，精准破弱模型 WPB）

1. 采用方案一：本模型并入《学员成长档案》规格，见 `weak-point-breakthrough-model.md`。
2. 检测口径采用 a：首次全面检测为一次性冻结基线，其薄弱点一次性写入当前薄弱系统，此后不再重写；成长基线独立于练习进度，但首个基线可用于启动专攻。
3. 攻克奖励：知识点 25 学习币 + 12 积分（现值不变），小节 40 学习币 + 0 积分（新增），无章节奖励；幂等由学期一次调整为终身一次。
4. 薄弱点可复现：攻克后再次薄弱记录 `reopened` 事件并回到 active，但不重复发放奖励。
5. 专攻加权：薄弱知识点/小节提高组卷抽取权重；章节达成率改为加权平均（薄弱小节权重 2、普通 1）。
6. 不做：系统提分评测、章节级奖励、独立家长门户与自动推送。
7. 文档方案：并入本规格（方案一），不新建独立规格目录。
