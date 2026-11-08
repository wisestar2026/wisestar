# 英语学习系统升级（单词三阶 / 句式升级 / 复习与通关）需求文档

Feature Name: 2026-11-05-english-learning-upgrade
Created: 2026-11-05
Status: 待确认
来源：`英语学习系统升级需求文档 V2.0（评审稿）` + `AI 英语出题任务规范（外研版 3-9 年级）`

## Introduction

现有英语模块已具备单词本（`t_english_word_book`）、句子本（`t_english_sentence_book`）、语法本（`t_english_grammar_book`）、情景/句式内容包、单元融合卷与智能复习队列。单词学习是「一词一题看英文选中文」，单词练习是三选一选择题，句式练习按连词成句/听音组句/听力理解即时生成，复习队列按艾宾浩斯到期取词。

本次升级把学习链路改为**分阶段递进**：单词按「记忆 → 练习 → 运用」三阶、句式按「课本跟读 → 句子学习 → 句子练习 → 情景对话」四段，并为每阶段定义可判定的通关标准；补齐 4 个新题型（听英文选中文、词汇默写、句式词汇填空、句子默写）；把复习从「重做原题」改为「针对知识点生成派生题」；把薄弱点从「做错即记」改为「按知识点聚合多次答题，输出题型权重更高，连续多道不同角度做对才算攻克」；并对低年级加难度上限过滤，避免 hard 题目流入主线学习。

## Glossary

- **知识点（knowledge item）**：复习与薄弱点的最小单位。单词模块为 `word_id`；句式模块为 `sentence_id`；语法模块为 `grammar_point_id`。
- **小节（section）**：教材单元内的分组，取 `t_english_word.section` / `t_english_sentence.section`；单词通关的统计粒度。
- **单词三阶**：单词记忆（识别释义）→ 单词练习（三种选择题）→ 单词运用（三种产出题）。
- **句式四段**：课本跟读 → 句子学习（听音组句/连词成句/词汇填空）→ 句子练习（三种选择题+句子默写）→ 情景对话。
- **派生题（derived question）**：以知识点为单位，切换考察角度重新生成的题目；与「原题重做」相对。
- **考察角度（exam angle）**：同一知识点下的不同考法，如「词→义」「义→词」「看图识词」「搭配填空」。
- **掌握状态**：`熟练 / 一般 / 陌生` 三态，由知识点聚合统计派生，与单词本 `familiarity` 0~4 取值并存但语义不同。
- **模块内复习**：单元页右上角【复习】进入，只处理当前单元、针对知识点出派生题。
- **全局错题本**：全系统答错的原始题目实例，重做原题，永久保存。
- **通关（pass）**：某模块在小节/单元粒度上同时满足正确率与状态分布阈值。
- **Demand**：`EnglishDifficultyUtil.Demand`，作答方式枚举 `RECOGNITION / CUED / APPLICATION / FULL`。

## Requirements

### R1 单词学习的三阶链路

**User Story:** AS 学员，I want 单词按「记忆→练习→运用」逐步推进，SO THAT 从认词到用词形成闭环。

#### Acceptance Criteria

1. WHEN 学员进入某单元单词通关页，the system SHALL 按小节分组展示三阶入口（单词记忆 / 单词练习 / 单词运用），并分别展示该小节的已完成数与总题量。
2. WHEN 学员进入单词记忆，the system SHALL 逐词展示英文与音标，由学员自评或四选一识别中文释义，一词一题。
3. WHEN 学员进入单词练习，the system SHALL 提供「看中文选英文」「看英文选中文」「听英文选中文」三个子题型，每个子题型按小节统计正确率。
4. WHEN 学员进入单词运用，the system SHALL 提供「选词填空」「情景对话」「词汇默写」三个子功能。
5. the system SHALL 对同一单词在同一子题型下只生成固定数量的题目，避免重复刷题。
6. WHILE 小节内单词记忆未达通关标准，the system SHALL 仍允许学员手动进入单词练习，并在页面上给出「建议先完成单词记忆」的提示。
7. IF 单元可用单词少于 4 个，the system SHALL 禁用需要四选一干扰项的题型入口。

### R2 单词运用的三个子功能

**User Story:** AS 学员，I want 在句子和情景中真正使用单词，SO THAT 掌握词形与时态变化。

#### Acceptance Criteria

1. WHEN 学员作答选词填空，the system SHALL 为每个目标单词出一道题，题干给中文提示与备选词库，由学员手动输入答案。
2. the system SHALL 使选词填空考察单复数、时态、固定词组与词形变化，且不考察特殊句式结构。
3. WHEN 学员作答情景对话，the system SHALL 展示单段 6~8 句的对话，覆盖 4~5 个本单元单词，并提供备选单词库。
4. WHEN 学员作答词汇默写，the system SHALL 展示中文提示并允许播放英文音频，由学员默写英文单词。
5. the system SHALL 在单词运用阶段对答错的知识点写入模块内复习池，并计入薄弱点统计。

### R3 句式通关的四段链路

**User Story:** AS 学员，I want 句式学习按「跟读→学习→练习→情景」递进，SO THAT 从听懂到会用在真实交际里。

#### Acceptance Criteria

1. WHEN 学员进入句式通关页，the system SHALL 展示四段入口：课本跟读、句子学习、句子练习、情景对话。
2. WHEN 学员进入句子学习，the system SHALL 提供「听音组句」「连词成句」「词汇填空」三个题型。
3. WHEN 学员作答听音组句，the system SHALL 只播放音频、不提供中文提示，由学员按顺序点击词块拼出完整句子。
4. WHEN 学员作答连词成句，the system SHALL 提供中文提示、不播放音频，由学员按顺序点击词块拼句。
5. WHEN 学员作答词汇填空，the system SHALL 为每个空位提供独立的透明边框下划线输入框，考察句式核心语法结构。
6. WHEN 学员进入句子练习，the system SHALL 提供「看中文选英文」「看英文选中文」「听英文选中文」「句子默写」四个题型。
7. WHEN 学员作答句子默写，the system SHALL 展示中文译文并播放整句音频，按单词拆分多个下划线输入框接收答案。
8. WHEN 学员进入情景对话，the system SHALL 复用既有单元情景内容包的固化对话。
9. the system SHALL 保持课本跟读复用既有 `student/english/read-aloud` 的播放、录音与评分能力。

### R4 模块内复习：按知识点出派生题

**User Story:** AS 学员，I want 复习针对我的薄弱知识点出新题，SO THAT 不会靠背原题答案通关。

#### Acceptance Criteria

1. WHEN 学员点击单元页右上角【复习】，the system SHALL 进入限定当前单元的复习流程，不携带其他单元的知识点。
2. the system SHALL 由三类数据构成复习池：掌握状态为「一般/陌生」的知识点、本单元本次练习中答错的知识点、到达间隔复习时间的知识点。
3. WHEN 学员复习某知识点，the system SHALL 复用本模块题型为该知识点生成考察角度不同于原题的派生题。
4. the system SHALL 在复习角标上展示待复习知识点数量，数量随掌握状态变化实时刷新。
5. WHEN 单元完成或重置，the system SHALL 允许清空该单元的复习池，且不影响全局错题本数据。
6. the system SHALL 在复习页以「单元复习 / 我的错题本」两个 Tab 展示两类来源，明确区分派生题与原题。
7. the system SHALL 使全局错题本继续保存答错的原始题目实例，重做原题并按做对清除。

### R5 通关标准与解锁

**User Story:** AS 学员/老师，I want 每个阶段有明确达标线，SO THAT 知道什么时候可以进入下一阶段。

#### Acceptance Criteria

1. WHEN 统计单词记忆通关，the system SHALL 以小节为单位判定：答题正确率 ≥ 85%、标记为「熟练」的占比达到阈值、陌生单词数 = 0。
2. WHEN 统计单词练习通关，the system SHALL 对三个子题型分别判定正确率 ≥ 80%，并允许少量错误进入下一阶段。
3. WHEN 统计单词运用通关，the system SHALL 判定三个子功能正确率 ≥ 75%。
4. WHEN 三阶均达标，the system SHALL 将小节标记为「单词通关完成」。
5. WHEN 统计句式通关，the system SHALL 以单元句式集合为单位：句子学习的听音组句与连词成句 ≥ 85%、词汇填空 ≥ 75%；句子练习的三个选择题型 ≥ 80%、句子默写 ≥ 70%；情景对话 ≥ 75%；课本跟读沿用原有合格阈值。
6. WHEN 四段均达标，the system SHALL 将单元标记为「句式通关完成」。
7. WHILE 前一阶段未达标，the system SHALL 保留跳转入口并展示「建议先完成前面练习，当前存在较多薄弱点」提示。
8. IF 模块未通关，the system SHALL 持续展示复习角标。

### R6 薄弱点识别与攻克

**User Story:** AS 学员，I want 薄弱判定与攻克都有明确规则，SO THAT 状态变化可解释、可预期。

#### Acceptance Criteria

1. the system SHALL 按知识点聚合多次答题行为判定薄弱状态，判定依据为：同一知识点连续 2 道派生题做错；或该知识点累计正确率低于 60%。
2. the system SHALL 对输出类题型（填空、默写、连词成句、改写）的错误赋予高于选择题型的权重。
3. WHEN 选择题错 1 次，the system SHALL 只记录答题结果，不直接标记为薄弱。
4. WHEN 输出类题型错 1 次，the system SHALL 加重扣分并优先将知识点纳入薄弱池。
5. the system SHALL 在单词通关小节卡片展示「熟练 / 一般 / 陌生」三态计数。
6. WHEN 判定薄弱点被攻克，the system SHALL 要求该知识点在复习模式下连续 2 道不同派生题作答正确。
7. IF 该知识点属于输出类题型，the system SHALL 要求复习中至少 1 道输出题型答对，选择题答对不计入攻克判定的充分条件。
8. WHEN 攻克条件满足，the system SHALL 将其状态按「陌生 → 一般 → 熟练」推进，并从待复习计数中扣减。
9. IF 复习做对 1 题后紧接着又做错，the system SHALL 将连续正确计数清零并重新累计。

### R7 年级难度约束与派生题重算

**User Story:** AS 学员，I want 题目难度匹配我的年级，SO THAT 低年级不会遇到超纲题。

#### Acceptance Criteria

1. the system SHALL 在生成新题型时接入 `EnglishDifficultyUtil`，并按题型映射 Demand：听英文选中文与选择题型为 `RECOGNITION`；单词选词填空、情景对话填空、句式词汇填空为 `CUED`；单词词汇默写与句子默写为 `FULL`；语法改写为 `APPLICATION`。
2. the system SHALL 按年级限制可产出的难度标签：1~2 年级单词为 easy/medium、句式为 easy；3~4 年级单词与句式均为 easy/medium，hard 仅允许出现在单元复习与单元测试；5~6 年级开放 easy/medium/hard。
3. WHEN 派生题算出的难度标签超过本年级上限，the system SHALL 丢弃该题并重新生成。
4. the system SHALL 提供 `POST /api/template/recalibrate-english-difficulty` 与 `POST /api/english/sentence-pack/recalibrate` 两个重算入口，对历史题目重算难度标签。
5. the system SHALL 保留历史旧题的既有难度标签，仅对升级后新生成的派生题执行年级过滤。
6. the system SHALL 使派生题难度继承母题，并在入库前做二次校验，防止难度漂移。

### R8 语法通关与句式通关的系统隔离

**User Story:** AS 学员，I want 句式与语法考的是不同能力，SO THAT 低年级即使文本重复也不重复刷题。

#### Acceptance Criteria

1. the system SHALL 使句式通关绑定 `sentence_id`，语法通关绑定 `grammar_point_id`，不通过英文字符串判断归属。
2. WHEN 同一条英文句子同时属于单元交际句式与某语法点，the system SHALL 在界面上提示该句子同时属于两处练习。
3. WHEN 语法通关为某单元派生题目，the system SHALL 优先使用变体句子，降低直接复用原句的比例。
4. the system SHALL 按语法知识点粒度提供三阶段题型：语法记忆（识别题）、语法练习（语法单选 / 判断）、语法运用（变形填空 `CUED` / 改写 `APPLICATION`）。
5. WHEN 统计语法通关，the system SHALL 判定识别类正确率 ≥ 80%、输出类正确率 ≥ 70%。
6. WHEN 单个语法知识点达标，the system SHALL 标记该知识点通关并纳入熟练 / 一般 / 陌生三态与复习池。
7. the system SHALL 在语法页提供右上角复习入口，复习池按语法知识点生成派生题。

### R9 页面与交互

**User Story:** AS 学员，I want 进度与链路一眼可见，SO THAT 知道下一步该做什么。

#### Acceptance Criteria

1. the system SHALL 在单词通关与句式通关页左侧展示单元列表与各单元达成进度，右侧展示当前单元的分组与阶段链路。
2. the system SHALL 以圆环或进度条展示每个题型/子功能的已完成数与总题量。
3. the system SHALL 在输入类题型（选词填空、句式填空、词汇默写、句子默写）统一使用透明边框加浅色下划线的独立输入框。
4. the system SHALL 在页面右上角固定展示复习入口与待复习角标。
5. WHILE 前序阶段未完成，the system SHALL 在后续阶段入口旁展示引导提示。

### R10 出题内容约束

**User Story:** AS 教研，I want AI 生成的题目符合教材与词汇范围，SO THAT 题库质量可控。

#### Acceptance Criteria

1. the system SHALL 限定题干、选项与答案使用的词汇在本单元与本年级已学范围内。
2. the system SHALL 使 AI 原生母题在同一知识点、同一题型下最多生成 2 道。
3. the system SHALL 使派生题在同一母题下最多生成 3 道，且每道考察角度不同。
4. WHEN 派生题与母题的考察角度完全相同、仅替换名词或人名，the system SHALL 判定为「换汤不换药」并弃题。
5. the system SHALL 为每道题记录 `source_type`（collected / ai_generate / ai_derived）、`exam_angle`、`master_id`、`knowledge_id` 与 `difficulty` 标签。
