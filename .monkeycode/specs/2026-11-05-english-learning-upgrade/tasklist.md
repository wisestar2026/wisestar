# 英语学习系统升级 实施计划

Feature: 2026-11-05-english-learning-upgrade
Created: 2026-11-05
基于: `requirements.md`、`design.md`

> 执行约定：每条完成后勾选；涉及数据库的改动必须同时改 `init-h2.sql` 与 `init-mysql.sql`；每阶段结束跑一次编译或构建。

## 阶段一：地基（数据与常量）

- [x] 1. 新增两张表与实体（R4/R6，design §Data Models）
   - `t_english_learning_state`：`user_id + module + knowledge_id` 唯一键；`state`、`consecutive_correct`、`last_angle`、`output_correct_count`、`weak_flag`、`weak_reason`
   - `t_english_exam_angle_use`：`user_id + module + knowledge_id + angle` 唯一键；`use_count`、`last_use_at`
   - 同步 `init-h2.sql` 与 `init-mysql.sql`，均用 `CREATE TABLE IF NOT EXISTS`
   - 新增 `EnglishLearningState`、`EnglishExamAngleUse` 实体（继承 `BaseModel`）与对应 Mapper

- [x] 2. 阈值常量收敛（R5/R6）
   - 在 `EnglishMastery` 增加单词三阶阈值：记忆 85%、练习 80%、运用 75%
   - 新增句式阈值常量：组句 85%、词汇填空 75%、选择题 80%、默写 70%、情景 75%
   - 新增语法阈值：识别 80%、输出 70%
   - 前端不得再硬编码同类数字

- [x] 3. 检查点 - 后端编译通过
   - `mvn clean package -pl api -am -DskipTests`

## 阶段二：规则服务（后端可单测）

- [x] 4. 实现 `EnglishExamAngleService`（R4/R6/R10）
   - 角度池：单词 `word-meaning / meaning-word / image-word / collocation`；句式 `listen-build / build / fill-gap / zh-en / en-zh / dictation`；语法 `rule-id / single-choice / judge / transform / rewrite`
   - `nextAngle` 在未使用角度中轮换，角度耗尽才允许重复
   - `isSameAngle` 用于「换汤不换药」判定

- [x] 5. 实现 `EnglishLearningRuleService`（R5/R6）
   - `judgeWeak`：连续 2 道派生题错、累计正确率 < 60%、输出题错误加权
   - `markBreakthrough`：连续 2 道不同角度正确 + 输出题至少 1 次正确；做错清零
   - `sectionProgress`：按小节汇总三阶完成数与通关状态
   - `unitPass`：按 design §Data Models 的阈值判定单词小节 / 句式单元通关
   - `recordLearning` 调用点接入状态更新，失败不阻断答题主流程

- [x] 6. 难度工具补年级过滤（R7）
   - `EnglishDifficultyUtil.Result#exceedsGradeLimit(String grade)`
   - 1~2 年级：单词 easy/medium，句式仅 easy；3~4 年级：easy/medium（hard 仅复习与单元测试）；5~6 年级：三档全开
   - 派生题生成处接入过滤，超限丢弃重生成；全部被过滤时回落到最低档并标记 `fallback`

- [x] 7. 单元测试（R5/R6/R7）
   - `EnglishLearningRuleServiceTest`：59%/60%/85% 三态边界、连续 2 题攻克、第 3 题答错清零、输出题单独要求
   - `EnglishDifficultyUtilTest`：三张年级上限表
   - `EnglishExamAngleServiceTest`：角度轮换与耗尽回落

- [x] 8. 检查点 - 后端测试通过
   - `mvn test -pl rdbms`

## 阶段三：学员端接口

- [x] 9. 扩展视图（R1/R5/R9）
   - `EnglishWordView` 增加 `masteryState`、`angleUsed`
   - `EnglishUnitProgressView` 增加 `wordPassRate`、`sentencePassRate`、`reviewDueWord`、`reviewDueSentence`
   - `EnglishUnitProgressView.wordPass` 改为走 `unitPass` 结果，口径与小节通关一致

- [x] 10. 接口改造（R1/R4/R5）
   - `GET /api/english/student/unit-progress`：返回小节级三阶进度（新增 `sections` 数组，每项含 `section / memoryDone / practiceDone / useDone / pass`）
   - `GET /api/english/student/review/candidates?unit=&module=`：返回待复习知识点与可用角度
   - `POST /api/english/student/learn-state`：记录作答结果（`knowledgeId / module / angle / correct / outputType`）
   - `POST /api/english/student/duel-angle`：可选，复习时取下一道不同角度派生题

- [x] 11. `recordLearning` 增加运用阶段来源（R2）
   - `source` 增加 `word-use`（选词填空、词汇默写）与 `word-scenario`（情景对话）
   - 首答统计口径保持「仅练习/巩固首答计入 `quiz_total`」，运用阶段单独统计，不污染掌握率

- [x] 12. 检查点 - 接口自测
   - 三年级与六年级各取一个学员，验证 `sections` 三阶数值、`reviewDueWord` 与复习候选单元隔离

## 阶段四：前端单词三阶

- [x] 13. 单词通关 hub 改造（R1/R9）
   - `EnglishWordLearnPage` 小节卡片由三个按钮改为三阶入口：单词记忆 / 单词练习（三题型）/ 单词运用（三功能）
   - 每个阶段展示圆环或进度条（已完成 / 总题量）
   - 未达前一阶段阈值时保留入口并给出「建议先完成前面练习」提示
   - 小节卡片展示「熟练 / 一般 / 陌生」三态计数

- [x] 14. 单词练习新增听英文选中文（R1）
   - `englishQuiz.js` 增加 `QUIZ_TYPE.LISTEN`，仅给中文选项、自动播放一次
   - 干扰项仍取同单元其他单词释义；不足 4 项时跳过该题

- [x] 15. 单词运用三功能（R2）
   - 选词填空：新增页面或复用 `EnglishScenarioPracticePage` 的输入交互，一词一题、给中文提示与词库
   - 情景对话：沿用既有 `EnglishScenarioPracticePage`
   - 词汇默写：新增独立入口，给中文、可播放音频、输入英文
   - 输入框统一透明边框加浅色下划线

- [x] 16. 检查点 - 前端构建通过
   - `npm run build`

## 阶段五：前端句式四段

- [x] 17. 句式通关 hub 改造（R3/R9）
   - `EnglishSentenceLearnPage` 改为四段入口：课本跟读 / 句子学习 / 句子练习 / 情景对话
   - 句子学习下含听音组句、连词成句、词汇填空
   - 句子练习下含看中文选英文、看英文选中文、听英文选中文、句子默写

- [x] 18. 补齐句式题型入口（R3）
   - `EnglishSentenceExercise` 的 `mode` 增加 `fill` 与 `dictation` 独立入口
   - 句子默写沿用逐词输入框实现，中文提示 + 整句音频
   - 词汇填空按空位拆多个输入框（当前为单框，需按 `blankCount` 拆分）

- [x] 19. 句式与语法隔离提示（R8）
   - 同一英文句子同时属于句式与语法时，页面提示「该句子同时属于单元交际句式和 XX 语法点练习」
   - 语法派生题优先使用变体句子，`grammar_point_id` 与 `sentence_id` 分开绑定

- [x] 20. 检查点 - 前端构建通过
   - `npm run build`

## 阶段六：复习与角标

- [x] 21. 复习页双 Tab（R4）
   - `EnglishReviewPage` 增加「单元复习 / 我的错题本」两个 Tab
   - 单元复习按单元筛选、按知识点出派生题；错题本保持重做原题
   - 复习角标数字取 `reviewCandidates` 返回数量

- [x] 22. 派生题取题与角度轮换（R4/R6/R10）
   - 复习时先取错题母题同知识点、再取未使用角度的派生变体
   - 不连续推送同一角度
   - 攻克判定在复习提交时调用 `markBreakthrough`

- [x] 23. 检查点 - 复习链路回归
   - 错题本「消灭错题」后复习池数量不变；单元完成清空复习池不影响错题本

## 阶段七：管理端与重算

- [x] 24. 重算接口（R7）
   - `POST /api/template/recalibrate-english-difficulty`：对题目模板批量重算，返回 `updated / skipped / failed`
   - `POST /api/english/sentence-pack/recalibrate`：对句子内容包重算
   - 缺标签的题目跳过并计数

- [x] 25. 管理端入口（R7/R10）
   - 题目与内容包管理页展示 `source_type`、`exam_angle`、`master_id`
   - 提供「按年级重算难度」按钮与结果提示

- [x] 26. 检查点 - 管理端回归
   - 抽查 10 道题重算前后难度标签；三年级学员主线不出现 hard

## 阶段八：收尾

- [x] 27. 文档同步
   - `docs/英语难度分级标准.md`：补年级上限表与 `exceedsGradeLimit`
   - `docs/开发维护日志.md`：新增本节记录
   - `docs/项目词典-后端-rdbms.md` / `-shared.md` / `-前端.md`：补新表、新服务、新字段
   - `docs/README.md`：如新增规范文档则登记

- [x] 28. 快照与交接
   - 停后端 → `server/db-export.sh` → 提交 `server/db-snapshot/wisestar.sql`
   - 重启预览并自检

## 风险与依赖

| 风险 | 影响 | 缓解 |
|---|---|---|
| 新表与既有 `*_book` 表口径不一致 | 掌握度与三态不同步 | 以 `learning_state` 为准做三态，`*_book` 继续供复习调度；两表在同一事务内更新 |
| 词汇填空需要按空位拆输入框 | 现有实现是单框，改动面较大 | 先按 `blankCount` 拆，无空位信息时降级为单框 |
| 低年级可用角度少，派生题重复 | 学员会觉得在刷同一题 | 角度耗尽后允许换语境（换名词/场景）而非换角度，并在管理端标记该知识点题目不足 |
| 复习角标与队列数量不一致 | 学员困惑 | 角标与 `reviewCandidates` 用同一取数方法，队列只做上限截断 |
