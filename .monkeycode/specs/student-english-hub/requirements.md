# 学生端英语学习中心 需求文档

Feature Name: student-english-hub
Created: 2026-09-15
Status: 初稿待确认

## Introduction

在现有学员端（海洋童趣风格）中新增「英语学习中心」，为学生提供按「教材版本 + 年级 + 册别 + 单元」组织的单词与句子学习闭环，包含单元导航、单词学习、句子学习、智能复习与测评入口。

内容以系统自有英语词库（`t_english_word`，PEP 四~六年级 740 词）、自有语法库（`t_english_grammar`）与自有 AI 内容包（`t_english_ai_pack`）为基础。产品形态参考同类英语学习产品（如参考截图）的**信息架构**，界面视觉、图标、配色、文案、音频与数据全部采用自有或已授权素材，不复制任何第三方品牌元素。

## Glossary

- **英语学习中心**：学员端英语模块主界面，聚合单元导航、单词学习、句子学习、智能复习与测评入口。
- **单元**：教材的一个教学单元，取自 `t_english_word.unit`（如 Unit 1）。
- **词表**：某单元下的单词集合。
- **句库**：某单元下的句子集合（英文 + 中文释义 + 音频，来源见 R6）。
- **熟练度**：单词/句子的掌握程度，取值 0（未学习）至 4（精通）。
- **复习队列**：按间隔重复算法排序的待复习单词与句子集合。
- **测评**：对单元单词或句子的即时判分练习，含题量与正确率统计。
- **学习会话**：学生一次连续完成某类学习/测评的过程，记录题量、正确率与时长。

## Requirements

### R1 英语学习中心主界面

**User Story:** AS 学生，I want 按教材进度进入单元并开始学习，SO THAT 高效使用英语内容。

#### Acceptance Criteria

1. WHEN 学生进入英语学习中心，the system SHALL 按所选教材版本、年级、册别展示单元列表。
2. WHEN 学生选择某一单元，the system SHALL 展示该单元的单词学习、句子学习、测评入口及各自完成进度。
3. WHEN 学生切换教材版本、年级或册别，the system SHALL 刷新单元列表与进度。
4. WHILE 该单元存在到期待复习内容，the system SHALL 在智能复习区展示待复习数量与「一键复习」入口。
5. IF 所选教材版本、年级或册别无内容，the system SHALL 展示空状态与可操作提示。
6. WHILE 学生查看主界面，the system SHALL 展示该学生真实的学习进度数据。

### R2 单词学习

**User Story:** AS 学生，I want 通过多种方式记忆单词，SO THAT 提高记忆效率。

#### Acceptance Criteria

1. WHEN 学生进入单词学习，the system SHALL 展示单词卡片，字段包含拼写、音标、释义与例句。
2. WHILE 单词存在图片或音频字段，the system SHALL 在单词卡片展示对应图片或音频播放入口。
3. WHEN 学生点击发音，the system SHALL 播放单词音频。
4. IF 单词缺少音频，the system SHALL 使用浏览器语音合成朗读单词。
5. WHEN 学生进入听音辨词，the system SHALL 播放发音并提供 4 个候选单词供选择。
6. WHEN 学生进入听写拼写，the system SHALL 播放发音并提供拼写输入框。
7. WHEN 学生提交一次作答，the system SHALL 记录正误并更新该单词熟练度与下次复习时间。
8. IF 学生作答错误，the system SHALL 展示正确答案并允许重试。

### R3 句子学习

**User Story:** AS 学生，I want 通过多种句子练习掌握句型，SO THAT 提升听与写的能力。

#### Acceptance Criteria

1. WHEN 学生进入句子学习，the system SHALL 按单元展示句子列表，字段包含英文、中文释义与发音入口。
2. WHEN 学生点击句子发音，the system SHALL 播放句子音频。
3. IF 句子缺少音频，the system SHALL 使用浏览器语音合成朗读句子。
4. WHEN 学生进入听音组句，the system SHALL 播放句子音频并要求学生按顺序选择词块组成句子。
5. WHEN 学生进入连词成句，the system SHALL 提供乱序词块并要求学生排列为正确语序。
6. WHEN 学生进入句子默写，the system SHALL 播放句子音频并要求学生输入完整句子。
7. WHEN 学生进入听力理解，the system SHALL 播放句子音频并要求学生完成对应理解题。
8. WHEN 学生完成一次句子作答，the system SHALL 记录正误并更新该句子熟练度与下次复习时间。
9. IF 学生句子作答错误，the system SHALL 展示正确答案并允许重试。

### R4 智能复习

**User Story:** AS 学生，I want 系统按复习算法安排待复习内容，SO THAT 科学巩固已学单词与句子。

#### Acceptance Criteria

1. WHILE 存在到期复习内容，the system SHALL 在复习队列中优先展示该内容。
2. WHEN 学生点击「一键复习」，the system SHALL 按复习算法顺序生成一次复习会话。
3. WHEN 学生完成一次复习作答，the system SHALL 按间隔重复规则更新该内容的下次复习时间。
4. IF 学生复习答错，the system SHALL 将该内容的下次复习时间安排在更近的时点。
5. IF 学生复习答对，the system SHALL 将该内容的下次复习时间安排在更远的时点。
6. WHILE 复习队列为空，the system SHALL 展示已完成状态提示。

### R5 学习进度与激励

**User Story:** AS 学生，I want 查看学习进度并获得奖励，SO THAT 保持学习动力。

#### Acceptance Criteria

1. WHEN 学生完成一次学习会话，the system SHALL 记录题量、正确率与学习时长。
2. WHEN 学生完成学习或测评，the system SHALL 按现有学币与积分规则发放奖励。
3. WHEN 学生查看进度，the system SHALL 展示单元完成度与各题型正确率。
4. WHILE 学生在英语学习中心学习，the system SHALL 计入现有在线时长统计与宝箱规则。

### R6 内容管理（管理端）

**User Story:** AS 管理员，I want 维护单元与句库内容，SO THAT 学生端可持续获得学习内容。

#### Acceptance Criteria

1. WHEN 管理员维护单元，the system SHALL 支持按教材版本、年级、册别新增与调整单元。
2. WHEN 管理员维护句库，the system SHALL 支持句子的新增、编辑与批量导入，字段包含英文、中文释义、音频与所属单元。
3. WHEN 管理员维护单词，the system SHALL 复用现有单词管理能力，包含例句字段。
4. IF 句库或词库缺少音频，the system SHALL 允许保存并在学生端回退到语音合成。
5. WHEN 管理员导入句子，the system SHALL 按「教材版本 + 年级 + 册别 + 单元 + 英文」去重更新。

### R7 知识产权与合规

**User Story:** AS 校方，I want 英语学习中心的界面与内容来源清晰，SO THAT 规避侵权风险。

#### Acceptance Criteria

1. WHERE 全部界面元素，the system SHALL 使用自有名称、原创图标、原创插画与原创配色。
2. WHERE 全部学习内容，the system SHALL 使用自有或已获得授权的词库、句库、图片与音频。
3. WHERE 功能命名，the system SHALL 使用通用教育术语（如听音组句、连词成句、听力理解、句子默写）。
4. WHERE 外部素材，the system SHALL 记录来源与授权信息。
5. WHEN 产品形态参考同类产品，the system SHALL 仅参考信息架构与通用教学法。

## Data Models

### 复用现有表

- **t_english_word**：单词（spell / phonetic / meaning / image_url / audio_url / example_sentence / version / grade / term / unit）。
- **t_english_grammar**：语法库。
- **t_english_ai_pack**：AI 单元内容包（含单词与语法例句）。
- **t_english_word_book**：学生单词本（user_id / word_id / familiarity / next_review_time）。
- **t_english_learning_log**：学习记录（user_id / type / content_id / duration / correct_count）。

### 新增表（已确认）

- **t_english_sentence**：独立句库（id / en / zh / audio_url / version / grade / term / unit / sort / create_time / update_time）。
- **t_english_sentence_book**：学生句子本（id / user_id / sentence_id / familiarity / correct_count / wrong_count / last_review_time / next_review_time）。
- **t_english_unit**：单元目录（id / version / grade / term / unit / sort / create_time），供单词与句子共用，用于单元列表展示与排序。

## References

[^1]: (Screenshot) - 参考产品「同步句子」页的信息架构（单元列表 + 题型卡片 + 智能复习区），仅作结构参考。
[^2]: (File) - `wisestar-client/src/pages/english/WordStudyPage.jsx` - 现有单词学习页。
[^3]: (File) - `server/rdbms/src/main/java/cn/wisestar/server/domain/model/EnglishWord.java` - 现有单词数据模型。
[^4]: (File) - `server/rdbms/src/main/java/cn/wisestar/server/impl/EnglishAiPackServiceImpl.java` - AI 内容包结构与例句来源。
[^5]: (File) - `wisestar-client/src/pages/student/student.css` - 学员端视觉规范（海洋童趣）。

---

**文档状态**：初稿待确认
**下一步**：与用户确认口径后生成技术设计文档
