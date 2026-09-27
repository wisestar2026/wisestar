# Requirements Document

## Introduction

本需求为学员端英语单词增加「选择题练习」这一新的记忆方式，并为外研版名词、形容词补齐配图。学员可从英语学习中心的单元卡片进入「单词练习」，在同一单元范围内完成三种选择题：看图选单词、看中文选单词、看单词选中文；作答结果计入单词熟练度并影响复习队列。同时，单词学习卡片改为「配图与释义一起揭示」，并在缺图时展示占位图，保证名词、形容词始终有图可看。

## Glossary

- **单词练习页**：学员端新增的选择题练习页面，按单元出题，路由 `/student/english/practice`。
- **选择题型**：看图选单词（image-to-word）、看中文选单词（meaning-to-word）、看单词选中文（word-to-meaning）三种。
- **干扰项**：与正确答案同属一个单元的错误选项。
- **目标配图单词**：释义词性前缀含 `n.` 或 `adj.` 且 `image_url` 为空的外研版单词。
- **候选图**：词典接口（有道 `pic_dict.pic`）为某单词返回的备选图片地址。
- **占位图**：单词无 `image_url` 时统一展示的内置图片，避免空白。
- **老师**：具有英语单词编辑权限（`english:word:update`）的后台用户。
- **熟练度记录**：`POST /api/english/word/record` 写入的单词作答，影响单词熟练度与复习到期时间。

## Requirements

### Requirement 1

**User Story:** AS 学员, I want 从单元卡片进入单词练习, so that 能在本单元范围内做选择题巩固记忆

#### Acceptance Criteria

1. WHEN 学员在英语学习中心查看单元卡片, THE 系统 SHALL 在该卡片的「单词学习」「句子学习」之外新增「单词练习」入口。
2. WHEN 学员点击某单元的「单词练习」入口, THE 系统 SHALL 使用该单元的单词作为题目来源。
3. IF 该单元单词数量不足 4 个, THEN THE 系统 SHALL 在该入口给出题目不足提示并允许返回学习中心。
4. WHEN 学员进入单词练习页, THE 系统 SHALL 展示本轮进度（当前题号与总题数）。

### Requirement 2

**User Story:** AS 学员, I want 通过图片、中文、单词三种线索回忆单词, so that 能从不同方向加深记忆

#### Acceptance Criteria

1. WHEN 单词练习页展示「看图选单词」题, THE 系统 SHALL 展示该单词的配图与 4 个英文单词选项。
2. WHEN 单词练习页展示「看中文选单词」题, THE 系统 SHALL 展示该单词的中文释义与 4 个英文单词选项。
3. WHEN 单词练习页展示「看单词选中文」题, THE 系统 SHALL 展示该英文单词与 4 个中文释义选项。
4. WHILE 生成某题选项, THE 系统 SHALL 提供 1 个正确选项与 3 个同单元干扰项，且 4 个选项互不相同。
5. IF 某单词缺少 `image_url`, THEN THE 系统 SHALL 对该作者仅出「看中文选单词」或「看单词选中文」题。
6. WHEN 学员选择某个选项, THE 系统 SHALL 立即标记该选项正误并展示正确答案。
7. WHEN 学员完成一道题, THE 系统 SHALL 允许学员进入下一题。
8. WHEN 学员完成本轮全部题目, THE 系统 SHALL 展示本轮答对数与总题数。

### Requirement 3

**User Story:** AS 学员, I want 练习作答被计入熟练度与复习队列, so that 练习与智能复习形成闭环

#### Acceptance Criteria

1. WHEN 学员答对一道题, THE 系统 SHALL 通过熟练度记录把该单词记为正确。
2. WHEN 学员答错一道题, THE 系统 SHALL 通过熟练度记录把该单词记为错误。
3. WHEN 一轮单词练习结束, THE 系统 SHALL 记录一次练习会话（会话类型为 `word-quiz`、会话时长与答对数）。
4. WHILE 学员在一轮练习中答错某单词, THE 系统 SHALL 在本轮末尾再次出该作者的题目，并在答对前持续重出；同一单词单轮重出次数上限为 3 次。

### Requirement 4

**User Story:** AS 老师, I want 为外研版名词、形容词批量补齐配图, so that 名词、形容词始终有图可看

#### Acceptance Criteria

1. WHEN 老师触发按条件批量补图, THE 系统 SHALL 选取指定 版本、年级、册别、单元 范围内 `image_url` 为空且释义词性前缀含 `n.` 或 `adj.` 的单词作为目标配图单词。
2. WHEN 某目标配图单词存在候选图, THE 系统 SHALL 自动取第一张候选图下载并写入该单词的 `image_url`。
3. IF 某目标配图单词无候选图或图片下载失败, THEN THE 系统 SHALL 保留该单词原有 `image_url` 并继续处理其余单词。
4. WHEN 批量补图处理结束, THE 系统 SHALL 返回处理总数、成功数、失败数与失败原因列表。
5. WHEN 老师复核某单词配图, THE 系统 SHALL 允许老师用其他候选图替换或上传本地图片。

### Requirement 5

**User Story:** AS 学员, I want 在单词学习卡片上让配图与释义一起出现, so that 图片能辅助我理解中文含义

#### Acceptance Criteria

1. WHILE 学员尚未揭示单词释义, THE 系统 SHALL 保持该单词的配图与中文释义处于隐藏状态，仅展示拼写、音标与朗读入口。
2. WHEN 学员揭示单词释义, THE 系统 SHALL 同时展示该单词的配图与中文释义。
3. IF 该单词缺少 `image_url`, THEN THE 系统 SHALL 在揭示释义时展示占位图。
4. IF 单词配图加载失败, THEN THE 系统 SHALL 以占位图替换该配图。

### Requirement 6

**User Story:** AS 学员, I want 缺图单词也有统一的图片占位, so that 页面布局保持一致

#### Acceptance Criteria

1. WHEN 任一展示单词配图的位置遇到缺少 `image_url` 的单词, THE 系统 SHALL 展示统一占位图。
2. THE 系统 SHALL 使用同一张内置占位图覆盖单词学习页与单词练习页。
