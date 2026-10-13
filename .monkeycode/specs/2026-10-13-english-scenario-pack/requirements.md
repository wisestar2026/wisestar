# 需求文档：英语单元情景练习预生成固化

Feature Name: 2026-10-13-english-scenario-pack
Updated: 2026-10-13

## Introduction

现状：学员端「情景对话练习」（单词情景）与「重点句式情景」在每次进入单元时都实时调用大模型生成，单次耗时约 40-120 秒，学员等待过久，且高峰期模型限流（429）会导致生成失败、内容不稳定。

目标：改为在内容准备阶段由管理端按单元批量预生成、人工预览确认后固化落库；学员端只读取已固化内容，秒开且稳定，不再触发实时 AI 生成。

本需求覆盖两类情景练习：单词情景对话（基于 `t_english_word`）与重点句式情景（基于 `t_english_sentence`）。

## Glossary

- **情景内容包（Scenario Pack）**：某 `版本+年级+册别+单元` 下，单词情景与句式情景的固化集合，是学员端的唯一数据来源。
- **单词情景组**：把单元单词按固定数量切分得到的一组单词；系统针对每组单独生成若干情景对话。
- **句式情景组**：把单元重点句式按固定数量切分得到的一组句式；系统针对每组单独生成若干情景。
- **合并展示**：学员端把同一单元各组生成的情景按顺序合并为一个列表呈现。
- **固化**：将生成结果写入数据库，并作为学员端情景练习的唯一数据来源。
- **单元单词集合**：在 `t_english_word` 中按 `版本+年级+册别+单元` 过滤得到的单词，按 `spell` 升序。
- **单元句式集合**：在 `t_english_sentence` 中按 `版本+年级+册别+单元` 过滤得到的句式，按 `sort` 升序。
- **管理系统**：wisestar 后台管理端。
- **学员端**：wisestar 学员学习中心。

## Requirements

### Requirement 1 管理端按单元生成情景内容包预览

**User Story:** AS 内容管理员, I want 按单元一键生成单词情景与句式情景的预览, so that 审核确认后可以固化给学员使用。

#### Acceptance Criteria

1. WHEN 内容管理员指定 `版本+年级+册别+单元` 并触发预览生成, THE 管理系统 SHALL 将该单元单词集合按每 8 个一组切分为单词情景组, 并对每组调用 AI 生成情景对话。
2. WHEN 内容管理员指定 `版本+年级+册别+单元` 并触发预览生成, THE 管理系统 SHALL 将该单元句式集合按每 6 个一组切分为句式情景组, 并对每组调用 AI 生成情景。
3. THE 管理系统 SHALL 在预览结果中返回单词情景组与句式情景组的分组信息、每组覆盖的单词/句式、以及情景总数, 且 SHALL 将该预览作为仅内存对象返回。
4. IF 指定单元既没有单词集合也没有句式集合, THEN THE 管理系统 SHALL 返回空的预览结果并在结果中标记该单元暂无可用内容。
5. IF AI 服务未启用, THEN THE 管理系统 SHALL 终止生成并返回可读的未启用提示。
6. IF AI Token 未配置, THEN THE 管理系统 SHALL 终止生成并返回可读的未配置提示。
7. IF AI 返回内容为空或无法解析为规定 JSON 结构, THEN THE 管理系统 SHALL 终止生成并返回可读的格式异常提示。

### Requirement 2 情景内容包固化保存

**User Story:** AS 内容管理员, I want 将确认后的预览保存固化, so that 学员端读取到稳定内容。

#### Acceptance Criteria

1. WHEN 内容管理员保存含有内容的预览, THE 管理系统 SHALL 以 `版本+年级+册别+单元` 为业务键写入一条情景内容包记录。
2. WHEN `版本+年级+册别+单元` 已存在有效情景内容包且内容管理员再次保存, THE 管理系统 SHALL 覆盖更新该记录, 并保持该业务键下只有一条有效记录。
3. IF 保存请求缺少可解析的内容, THEN THE 管理系统 SHALL 拒绝保存并返回可读提示。
4. THE 管理系统 SHALL 在保存结果中返回该内容包的标识与 `版本+年级+册别+单元`。

### Requirement 3 学员端读取固化内容（合并展示）

**User Story:** AS 学员, I want 打开单元情景练习立即看到内容, so that 无需等待 AI 生成。

#### Acceptance Criteria

1. WHEN 学员打开某单元的「情景对话练习」, THE 学员端 SHALL 以数据库固化内容作为该单元单词情景的唯一数据来源。
2. WHEN 学员打开某单元的「情景对话练习」, THE 管理系统 SHALL 按组顺序合并该单元全部单词情景组并返回情景列表。
3. WHEN 学员打开某单元的「重点句式情景」, THE 管理系统 SHALL 按组顺序合并该单元全部句式情景组并返回情景列表。
4. IF 该单元没有已固化的单词情景, THEN THE 管理系统 SHALL 返回空列表, 且 THE 学员端 SHALL 展示「该单元练习尚未生成，请联系老师」的提示并隐藏重新生成入口。
5. IF 该单元没有已固化的句式情景, THEN THE 管理系统 SHALL 返回空列表, 且 THE 学员端 SHALL 展示「该单元练习尚未生成，请联系老师」的提示并隐藏重新生成入口。
6. THE 管理系统 SHALL 保证学员端返回的每个情景所考查的单词属于其所属单词情景组的词集合。
7. THE 管理系统 SHALL 保证学员端返回的每个句式情景所考查的句式属于其所属句式情景组的句式集合。

### Requirement 4 情景内容包管理

**User Story:** AS 内容管理员, I want 查看、检索与删除已固化的情景内容包, so that 维护内容库。

#### Acceptance Criteria

1. WHEN 内容管理员按 `版本+年级+册别+单元` 查询内容包列表, THE 管理系统 SHALL 返回按创建时间倒序的分页结果。
2. WHEN 内容管理员查看某内容包详情, THE 管理系统 SHALL 返回该内容包的完整分组内容。
3. WHEN 内容管理员删除某内容包, THE 管理系统 SHALL 逻辑删除该记录, 且该单元学员端情景练习随之返回空列表。

### Requirement 5 整册批量生成固化

**User Story:** AS 内容管理员, I want 对整册所有单元批量生成并固化, so that 一次性准备整册内容。

#### Acceptance Criteria

1. WHEN 内容管理员指定 `版本+年级+册别` 并触发整册查询, THE 管理系统 SHALL 返回该册下所有存在单词集合或句式集合的单元列表。
2. WHEN 内容管理员对 `版本+年级+册别` 触发批量生成, THE 管理系统 SHALL 对该册每个单元依次生成并固化情景内容包。
3. THE 管理系统 SHALL 在批量过程中逐个单元反馈成功或失败结果。
4. IF 某个单元生成或保存失败, THEN THE 管理系统 SHALL 记录该单元失败并继续处理其余单元。

### Requirement 6 权限与访问控制

**User Story:** AS 系统管理员, I want 情景内容包的生成与管理受权限控制, so that 内容安全可控。

#### Acceptance Criteria

1. THE 管理端情景内容包接口 SHALL 要求 `english:word:ai` 功能权限。
2. THE 学员端情景练习接口 SHALL 要求已登录（`isAuthenticated`）。

### Requirement 7 ID 与审计一致性

**User Story:** AS 系统管理员, I want 情景内容包符合统一约定, so that 系统一致可维护。

#### Acceptance Criteria

1. THE 情景内容包记录 SHALL 使用 `id / create_at / create_by / update_at / update_by / is_deleted` 审计列。
2. THE 情景内容包实体 SHALL 继承统一基础模型 `BaseModel`。
