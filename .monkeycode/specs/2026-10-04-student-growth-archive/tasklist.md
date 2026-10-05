# 精准破弱模型（WPB）实施计划

Feature Name: student-growth-archive（精准破弱模型并入）
Model: 精准破弱（WPB）
Updated: 2026-10-05

> 约定：`*` 标记的子任务为测试/文档类可选任务，默认只执行核心开发任务。

- [x] 1. 检测落库与基线冻结（R1 / R2 / R11.6）
  - [x] 1.1 新增 `t_detect_record` 建表、实体与 Mapper
    - 表结构按 design.md 4.1；含 `baseline_key` 唯一列
    - 实体 `DetectRecord extends BaseModel`，`@TableName("t_detect_record")`
    - `DetectRecordMapper extends BaseMapper<DetectRecord>`
    - 同步 `init-h2.sql` 与 `init-mysql.sql`（幂等）
  - [x] 1.2 扩展检测 DTO
    - `DetectSubmitRequest` 增加 `subjectId / grade / term / chapterIds / questionCount / durationMs / semester / clientToken`
    - `DetectReportView` 增加 `detectType / baseline / recordId`
    - 新增 `DetectRecordView`（历史条目）
  - [x] 1.3 `DetectionServiceImpl.submit` 落库 + 基线判定（R1.1 / R1.2 / R1.3 / R1.4 / R2.1）
    - 首次（`baseline_key` 为空）标 PRE + `is_baseline=1`；否则 STAGE
    - `baseline_key = studentId:subjectId:semester` 仅 PRE 写入，唯一约束兜底并发
    - 落库失败 try/catch，仍返回诊断报告
  - [x] 1.4 检测历史查询接口（R2.2 / R2.3）
    - `DetectionService.history(studentId, subjectId, semester)`
    - `GET ${api.prefix}/student/detect/history`
  - [x]* 1.5 基线判定单元测试（R1 / R2 验收项）
    - `DetectionServiceImplTest`：首测 PRE+is_baseline+baseline_key、已有 PRE 转 STAGE、同 clientToken 复用既有记录

- [x] 2. 薄弱点事件流（R12）
  - [x] 2.1 新增 `t_weak_point_event` 建表、实体与 Mapper
    - 表结构按 design.md 4.5；`uk_weak_event` + `idx_weak_event`
    - 同步 `init-h2.sql` 与 `init-mysql.sql`
  - [x] 2.2 事件记录组件（第 12.2 节：批量、幂等、afterCommit、容错）
    - 事件对象 `WeakPointTransitionEvent`
    - `WeakPointEventRecorder`：`@TransactionalEventListener(AFTER_COMMIT)` + `REQUIRES_NEW` + try/catch
    - 逐条插入捕获 `DuplicateKeyException` 忽略
  - [x] 2.3 薄弱刷新链接入事件（R12.1 / R12.3）
    - `updateProgress`：active 新建 → discovered；conquered → active → reopened
    - `markConquered`：→ conquered 事件
    - 填充 kp/section/chapter 名称快照
  - [x] 2.4 薄弱事件时间线接口（R12.6）
    - `GET ${api.prefix}/student/weak/timeline`
  - [x]* 2.5 事件幂等单元测试（R12.5）
    - `GrowthArchiveServiceImplTest`：同 sourceType+sourceId 只写一次；语法类按 grammarId+日期累加

- [x] 3. 检查点 - 构建通过，检测落库、基线、事件可用
  - 确保 `mvn clean package -pl api -am -DskipTests` 通过；如有疑问请询问用户

- [x] 4. 基线薄弱点 seed 与薄弱标记（R1.6 / R11.1）
  - [x] 4.1 `EvaluationService` 增加 `seedWeakFromBaseline(userId, subjectId, Map<kpId,rate>)`
    - 仅首次基线调用；幂等 upsert `t_user_weak_knowledge`
    - 每个薄弱点写一条 `discovered` 事件（source=detect）
  - [x] 4.2 检测基线 seed 接入（口径 a）
    - `DetectionServiceImpl.submit` 判定基线成功后调用 seed；后续检测不调用
  - [x] 4.3 小节/章节视图薄弱标记（R11.1 / R11.4）
    - 一次查询 active 薄弱 kp → 内存聚合 `weak` / `weakCount`
  - [x]* 4.4 seed 幂等单元测试（R1.6）
    - `EvaluationServiceImplTest`：仅播种低于阈值且未登记的知识点；重复播种不新增记录/事件

- [x] 5. 薄弱点专攻加权（R11.2 / R11.3 / R11.5）
  - [x] 5.1 组卷加权 `StudentServiceImpl.pickByCoverage`（第 12.4 节）
    - 一次查询 active 薄弱 kp/section 集合，内存加权（薄弱 2 / 普通 1）
    - 权重常量可配；薄弱为空走原逻辑
  - [x] 5.2 章节达成率加权（R11.3）
    - `StudentServiceImpl.fillChapterProgress` 由算术平均改加权平均
  - [x] 5.3 跨单元薄弱专攻组卷接口（R11.5）
    - `POST ${api.prefix}/student/practice/weak`
  - [x]* 5.4 加权单元测试（R11.2 / R11.3）
    - `StudentServiceImplTest`：薄弱知识点配额高于普通；无线索时均分；保底覆盖＋归属回填

- [x] 6. 攻克奖励（R13）
  - [x] 6.1 新增 `ACTION_WEAK_SECTION_CONQUER`（40 币 + 0 积分）与规则文案
  - [x] 6.2 `RewardServiceImpl.settle` 幂等按 lifetime 行为集合（R13.4）
    - `LIFETIME_ACTIONS = {weak_conquer, weak_section_conquer}` 不加学期前缀
  - [x] 6.3 小节攻克判定与发放（R13.2）
    - 小节曾薄弱且当前无 active → 终身一次发 40 币
  - [x]* 6.4 终身一次/并发幂等测试（R13.1 / R13.2）
    - `RewardServiceImplTest`：攻克类幂等键不带学期前缀、普通行为带前缀、重复发放被拦截；
      并发穿透由 `t_user_learning_record` 唯一索引兜底（DB 层，不在 Mockito 覆盖范围）

- [x] 7. 检查点 - 构建通过，加权与奖励生效
  - 确保构建通过并冒烟；如有疑问请询问用户

- [x] 8. 对比呈现与成长报告（R14）
  - [x] 8.1 薄弱对比接口（基线 vs 当前，第 12.5 节）
  - [x] 8.2 成长档案规格落地（t_learning_growth / compare / report；见 design.md）
  - [x] 8.3 前端成长对比与薄弱时间线
  - [x]* 8.4 对比计算单元测试（R14.1）
    - `GrowthArchiveServiceImplTest`：新旧快照格式解析、按名称回查 kpId、攻克阈值与 delta、轨迹聚合

- [x] 9. 最终检查点 - 全量构建、种子幂等、冒烟、快照导出
  - 双库脚本幂等、预览冒烟、导出并提交快照；如有疑问请询问用户
  - 单元测试：`mvn -pl rdbms -am test`（21 项，覆盖 1.5 / 2.5 / 4.4 / 5.4 / 6.4 / 8.4）
