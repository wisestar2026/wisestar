# 英语学习模块 需求文档

Feature Name: english-learning
Created: 2026-08-30
Status: 初稿待确认

## Introduction

英语学习模块包含**单词记忆**与**语法学习**两大核心功能，面向小学三年级起点学生，按教材版本（人教版）+ 年级 + 单元组织内容。单词记忆支持图片记忆/听音辨词/听写拼写/例句记忆/艾宾浩斯复习等多种方式，语法学习提供讲解 + 例句 + 练习完整闭环。

## Glossary

- **单词本**：用户学习的单词集合，记录熟练度与下次复习时间
- **艾宾浩斯遗忘曲线**：根据记忆遗忘规律安排复习时间（5 分钟/30 分钟/12 小时/1 天/2 天/4 天/7 天/15 天）
- **熟练度**：单词掌握程度（0-未学习，1-生疏，2-熟悉，3-熟练，4-精通）
- **语法点**：语法知识单元（如"一般现在时"、"名词复数"）

## Requirements

### R1. 单词库管理（后台）

**User Story:** AS 管理员，I want 按教材版本 + 年级 + 单元组织单词库，SO THAT 学生可以按进度学习

#### Acceptance Criteria

1. WHEN 管理员新增单词，the system SHALL 支持填写单词拼写/音标/释义/图片/音频/例句
2. WHEN 管理员批量导入单词，the system SHALL 支持 Excel 导入（按单元组织）
3. WHERE 单词归属，the system SHALL 支持选择教材版本/年级/单元
4. WHEN 单词信息更新，the system SHALL 同步更新所有用户的学习进度

### R2. 单词记忆（前端）

**User Story:** AS 学生，I want 通过多种方式记忆单词，SO THAT 提高记忆效率

#### Acceptance Criteria

1. WHEN 学生学习单词，the system SHALL 展示单词卡片（拼写 + 音标 + 释义 + 图片 + 例句）
2. WHEN 学生点击发音，the system SHALL 播放单词发音
3. WHEN 学生选择"听音辨词"，the system SHALL 播放发音并让学生选择正确单词
4. WHEN 学生选择"听写拼写"，the system SHALL 播放发音并让学生拼写单词
5. WHEN 学生完成学习，the system SHALL 记录熟练度并安排下次复习时间
6. WHILE 单词复习时间到期，the system SHALL 提醒学生复习

### R3. 艾宾浩斯复习（智能安排）

**User Story:** AS 学生，I want 系统根据遗忘曲线安排复习，SO THAT 科学记忆单词

#### Acceptance Criteria

1. IF 学生首次学习单词，the system SHALL 安排 5 分钟后首次复习
2. IF 学生复习正确，the system SHALL 按遗忘曲线延长下次复习时间（30 分钟/12 小时/1 天/2 天/4 天/7 天/15 天）
3. IF 学生复习错误，the system SHALL 重置为首次复习时间
4. WHILE 有待复习单词，the system SHALL 在学生学习时优先展示

### R4. 语法库管理（后台）

**User Story:** AS 管理员，I want 按年级组织语法库，SO THAT 学生可以系统学习语法

#### Acceptance Criteria

1. WHEN 管理员新增语法点，the system SHALL 支持填写标题/讲解内容/例句/练习题
2. WHEN 语法点按年级分类，the system SHALL 支持选择适用年级
3. WHEN 练习题设置，the system SHALL 支持选择题/填空题题型

### R5. 语法学习（前端）

**User Story:** AS 学生，I want 系统学习语法知识，SO THAT 掌握语法规则

#### Acceptance Criteria

1. WHEN 学生学习语法点，the system SHALL 展示讲解内容 + 例句
2. WHEN 学生学习完成，the system SHALL 提供练习题
3. IF 练习正确，the system SHALL 记录掌握情况
4. IF 练习错误，the system SHALL 显示正确答案与解析

### R6. 学习进度统计

**User Story:** AS 学生，I want 查看学习进度，SO THAT 了解学习效果

#### Acceptance Criteria

1. WHILE 学生学习，the system SHALL 记录学习时长/单词数/正确率
2. WHEN 学生查看进度，the system SHALL 展示已学单词数/熟练度分布/学习时长
3. WHERE 学习积分，the system SHALL 计入现有学海积分体系

## Data Models

### t_english_word（单词库）
- id / spell（拼写）/ phonetic（音标）/ meaning（释义）/ image_url / audio_url / example_sentence / version（版本）/ grade（年级）/ unit（单元）

### t_english_word_book（用户单词本）
- user_id / word_id / familiarity（熟练度 0-4）/ next_review_time / created_at

### t_english_grammar（语法库）
- id / title / content（讲解）/ examples / exercises / grade（年级）

### t_english_learning_log（学习记录）
- user_id / type（word/grammar）/ content_id / duration / correct_count / created_at

## References

[^1]: 艾宾浩斯遗忘曲线 - 记忆规律参考
[^2]: 人教版小学英语教材 - 内容组织参考

---

**文档状态**：初稿待确认
**下一步**：生成技术设计文档
