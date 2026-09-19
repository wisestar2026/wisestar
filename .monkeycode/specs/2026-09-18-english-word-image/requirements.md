# Requirements Document

## Introduction

本需求为英语板块补齐两件事：一是让老师能用免费词典接口批量提取单词的音标、释义、例句；二是让老师能为「词库中没有图片的单词」批量获取候选配图，逐一人工审核后确认入库，学员端用图片辅助记单词。

## Glossary

- **词典接口**：无需密钥的公开词典查询接口，输入英文单词，返回音标、中文释义、双语例句。
- **词库单词**：`t_english_word` 中的单词记录，含拼写、音标、释义、例句、图片地址等字段。
- **无图片单词**：`image_url` 为空或未设置的词库单词。
- **候选图**：系统为一个单词自动获取的若干张备选图片地址。
- **图片入库**：把审核通过的图片存储到系统文件服务，并把单词的 `image_url` 更新为存储后的访问地址。
- **老师**：具有英语单词编辑权限（`english:word:update`）的后台用户。

## Requirements

### Requirement 1

**User Story:** AS 英语老师, I want 批量从词典获取单词的音标、释义与例句, so that 无需逐个手工录入词典字段

#### Acceptance Criteria

1. WHEN 老师选中一个或多个单词并触发「词典补全」, THE 系统 SHALL 对每个单词调用词典接口并回填其音标、释义、例句。
2. WHEN 单词的音标、释义或例句已有非空内容, THE 系统 SHALL 保留该字段原值。
3. IF 词典接口对某单词无结果或调用失败, THEN THE 系统 SHALL 记录该单词的失败原因并继续处理其余单词。
4. WHEN 词典补全处理结束, THE 系统 SHALL 返回处理总数、成功数、失败数与失败原因列表。

### Requirement 2

**User Story:** AS 英语老师, I want 为没有图片的单词批量获取候选图片, so that 可以快速挑选合适配图

#### Acceptance Criteria

1. WHEN 老师设置筛选条件, THE 系统 SHALL 按教材版本、年级、册别、单元、小节与单词拼写筛选词库单词。
2. WHEN 老师启用「仅看无图片」, THE 系统 SHALL 只返回 `image_url` 为空或未设置的单词。
3. WHEN 老师选中若干单词并触发「获取候选图」, THE 系统 SHALL 为每个单词返回一个候选图片地址列表。
4. IF 某单词没有候选图片, THEN THE 系统 SHALL 返回空候选列表，供老师改用手动上传。

### Requirement 3

**User Story:** AS 英语老师, I want 审核候选图片并确认入库, so that 词库中的配图准确可信

#### Acceptance Criteria

1. WHEN 老师在某个单词下选定一张候选图并确认, THE 系统 SHALL 下载该图片、存储到系统文件服务，并把该单词的 `image_url` 更新为存储后的访问地址。
2. WHEN 老师为某个单词上传本地图片并确认, THE 系统 SHALL 存储该图片并把该单词的 `image_url` 更新为存储后的访问地址。
3. IF 候选图下载或存储失败, THEN THE 系统 SHALL 返回失败提示且不修改该单词的 `image_url`。
4. WHEN 某单词的图片确认入库, THE 系统 SHALL 在配图审核界面展示该单词已更新的图片。

### Requirement 4

**User Story:** AS 学员, I want 在单词卡片上看到单词配图, so that 可以借助图片记忆单词

#### Acceptance Criteria

1. WHEN 单词设置了 `image_url`, THE 学员端单词卡片 SHALL 展示该单词的配图。
2. WHEN 单词未设置 `image_url`, THE 学员端单词卡片 SHALL 正常展示拼写、音标与释义。
