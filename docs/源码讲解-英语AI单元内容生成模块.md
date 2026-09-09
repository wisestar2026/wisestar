# 英语 AI 单元内容生成模块 -- 源码讲解与维护手册

> 面向后续维护者。读完本文应能回答：这个模块解决什么问题、一个请求从点按钮到写库经历了什么、出问题时去哪改、新环境怎么把它跑起来。
>
> 代码位置：后端 `server/api|rdbms|shared` 下 `english` 相关类；前端 `wisestar-client/src/pages/english/WordAiManagePage.jsx` 与 `src/pages/system/AiSettingPage.jsx`。
> 功能开发日志见 `docs/开发维护日志.md` 第 40 节。

---

## 一、这个模块解决什么问题

系统里「英语板块」有单词记忆 + 语法学习的闭环（学生端），但词库和语法库一直靠手工填。本模块让老师（或管理员）选定 教材版本 + 年级 + 单元（可选主题），由大模型一次性生成该单元的整套同步学习内容：

```
单词卡（拼写/音标/释义/例句+中译） + 语法讲解（含例句） + 配套单选题
```

并遵循「先生成给人看 → 满意才落库 → 落库后可一键同步进正式词库/语法库」的审校流程，避免 AI 脏数据直接进学生学习数据。

### 全链路数据流

```
老师点「生成整套内容」
  → POST /api/english/ai-pack/generate（版本/年级/单元/主题）
  → EnglishAiPackServiceImpl.callChat()
      → 读系统 AI 配置（t_sys_info.ai_setting，与 AI 问答同一套）
      → HTTPS 调 SiliconFlow /v1/chat/completions（非流式）
  → 解析返回 JSON → 规整为 { title, words[], grammar{} } 标准结构（后端做 key 别名容错）
  → 前端结构化预览（单词表格 / 语法讲解 / 练习卡片）
老师点「保存为内容包」
  → POST /api/english/ai-pack/save
  → 写 t_english_ai_pack（同 版本+年级+单元 已存在则覆盖更新）
老师点「同步到词库与语法库」
  → POST /api/english/ai-pack/sync
  → words[] → t_english_word（同 版本+年级+单元+拼写 存在则更新，否则新增）
  → grammar → t_english_grammar（同 年级+标题 存在则覆盖，否则新增）
```

---

## 二、新环境如何把它跑起来（管理员前置动作）

前提：后端已用 preview profile 启动（H2 文件库）。详见 `docs/开发维护日志.md` 第 3 节。

1. 启动后端：在 `server/api` 目录执行
   `java -jar target/wisestar-v1.9.0.jar --spring.profiles.active=preview`
   （一定要带 `preview` profile，否则会去连本机 MySQL 报 Connection refused）
2. 启动前端：`wisestar-client` 目录执行 `npm run dev`，建 `.env.local`（被 gitignore）写 `API_TARGET=http://localhost:1991`。
3. 用管理员（admin/123456）登录，进入「系统管理 → AI 服务设置」（路由 `/system/ai`，权限 `system:role:list`）：
   - 打开「启用 AI 服务」开关；
   - 「可用模型列表」填 SiliconFlow 平台的模型 ID（**列表第一个 = AI 生成默认模型**），如 `deepseek-chat`；
   - 填 API Token（你自己的 SiliconFlow key，保存后**永不回显**，留空保存表示不修改）。
   - 保存。
4. 进入「英语板块 → AI 内容生成」（路由 `/english/word-ai`），选版本/年级/单元即可点「生成整套内容」。
   - 若未配置/未启用 AI，generate 接口返回业务 code 400 +「AI 服务未启用：请先在系统设置中开启 AI 并完成平台配置」，不会静默失败。

---

## 三、涉及文件清单

| 文件 | 类型 | 角色 |
|------|------|------|
| `server/shared/.../dto/english/EnglishAiPackQuery.java` | 新增 | 列表查询 DTO（版本/年级/单元 + 分页） |
| `server/shared/.../dto/english/EnglishAiPackView.java` | 新增 | 内容包视图 DTO（content 为整段 JSON 字符串） |
| `server/shared/.../dto/english/PackSyncResult.java` | 新增 | 同步结果统计（新增/更新单词数、语法条数） |
| `server/shared/.../service/EnglishAiPackService.java` | 新增 | 服务接口（shared 层定义，rdbms 实现） |
| `server/rdbms/.../domain/model/EnglishAiPack.java` | 新增 | 实体 → 表 `t_english_ai_pack` |
| `server/rdbms/.../domain/model/EnglishGrammar.java` | 新增 | 实体 → 表 `t_english_grammar`（语法库此前无实体，一并补上） |
| `server/rdbms/.../mapper/EnglishAiPackMapper.java` | 新增 | MyBatis-Plus BaseMapper |
| `server/rdbms/.../mapper/EnglishGrammarMapper.java` | 新增 | 同上 |
| `server/rdbms/.../impl/EnglishAiPackServiceImpl.java` | 新增 | 核心实现（大模型调用 + JSON 规整 + 保存 + 同步） |
| `server/api/.../api/EnglishAiPackApi.java` | 新增 | REST 控制器 |
| `server/rdbms/src/main/resources/scripts/init-h2.sql` | 修改 | 追加建表 `t_english_ai_pack` |
| `server/rdbms/src/main/resources/scripts/init-mysql.sql` | 修改 | 同上（MySQL 版） |
| `server/rdbms/.../impl/SystemServiceImpl.java` | 修改 | 修复 `getSystemAiSetting` 空配置 500（见第六节） |
| `wisestar-client/src/pages/english/WordAiManagePage.jsx` | 重写 | AI 单元生成页全流程（原页是「按条件批量补音频/图片」壳，已被取代） |
| `wisestar-client/src/pages/system/AiSettingPage.jsx` | 新增 | 系统管理 → AI 服务设置页 |
| `wisestar-client/src/api/system.js` | 修改 | 追加 `getAiSetting` / `saveAiSetting` |
| `wisestar-client/src/App.jsx` | 修改 | 新增路由 `/system/ai` |
| `wisestar-client/src/components/layout/MainLayout.jsx` | 修改 | 系统管理菜单追加「AI 服务设置」 |

---

## 四、表结构与内容 JSON 契约

### 4.1 表 `t_english_ai_pack`（新增）

列：`id, version, grade, unit, topic, title, content(text), word_count, created_at, create_at/create_by/update_at/update_by/is_deleted`（审计列与 BaseModel 一致，初始化脚本用 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 补齐）。

> 维护提醒：`content` 在 MySQL 用 `mediumtext`、H2 用 `text`。**两份脚本（init-h2.sql / init-mysql.sql）必须同步修改**；preview（H2）每次启动都会重跑 `init-h2.sql`（`spring.sql.init.mode: always`），所以加列/加表改 h2 脚本即可自动生效，MySQL 脚本只对全新库生效。

### 4.2 表 `t_english_word`（既有，写入方）

同步规则（幂等关键）：`version + grade + unit + spell` 四元组已存在 → 只更新 phonetic/meaning/example_sentence；不存在 → 新增。图片/音频字段不动（由旧的逐词 AI 内容补全流程负责，非本模块职责）。

### 4.3 表 `t_english_grammar`（既有，写入方）

同步规则：`grade + title` 已存在 → 覆盖 content/examples/exercises；不存在 → 新增。examples/exercises 列存 JSON 字符串。

### 4.4 content 的标准 JSON 结构（generate 返回值与存库值）

```json
{
  "title": "单元学习内容标题",
  "words": [
    {
      "spell": "elephant",
      "phonetic": "/ˈelɪfənt/",
      "meaning": "n. 大象",
      "example": "The elephant is big.",
      "exampleZh": "这头大象很大。"
    }
  ],
  "grammar": {
    "title": "语法点标题",
    "content": "中文讲解正文",
    "examples": [ { "en": "英文例句", "zh": "中文翻译" } ],
    "exercises": [
      {
        "q": "题干",
        "options": ["A. xxx", "B. xxx", "C. xxx", "D. xxx"],
        "answer": "B",
        "explanation": "解析"
      }
    ]
  }
}
```

大模型可能把 words 写成 `unitWords/wordList`、把 grammar 写成 `grammarPoint`、大小写混用等，后端 `normalizeContent` 已做别名与大小写容错，落库永远是这个标准结构。**前端预览直接读这套结构，不要再自行发明字段名。**

---

## 五、后端逐层拆解

### 5.1 Controller `EnglishAiPackApi.java`

| 方法 | 接口 | 说明 |
|------|------|------|
| list | `GET /api/english/ai-pack/list?version=&grade=&unit=&current=&pageSize=` | 分页列表，**content 置空**减小传输 |
| detail | `GET /api/english/ai-pack/detail?id=` | 详情，带完整 content |
| generate | `POST /api/english/ai-pack/generate?version=&grade=&unit=&topic=` | 调大模型生成（不落库） |
| save | `POST /api/english/ai-pack/save`（body=EnglishAiPackView） | 保存/覆盖内容包 |
| sync | `POST /api/english/ai-pack/sync?id=` | 同步进词库+语法库 |
| delete | `POST /api/english/ai-pack/delete?id=` | 删除内容包（逻辑删除） |

权限统一用 `english:word:ai`（与菜单「AI 内容生成」的可见权限一致，避免有菜单权限却 403）。

### 5.2 核心实现 `EnglishAiPackServiceImpl.java`

构造依赖：`EnglishAiPackMapper` + `EnglishWordMapper` + `EnglishGrammarMapper` + `SystemService`。自建 `RestTemplate`（连接超时 15s、读超时 180s，因为一次生成可能几十秒）与 `ObjectMapper`。

关键方法（按数据流）：

- **`generateUnit(version, grade, unit, topic)`**：校验非空 → `callChat` → `parseRawJson` → `normalizeContent` → 返回带 content 的 view。
- **`callChat(...)`**：从 `systemService.getSystemAiSetting()` 拿配置。未启用或 token 空 → 抛 `javax.validation.ValidationException`（全局异常处理器转成 `code 400` 友好提示）。模型取 `models` 列表第一个，空则兜底 `deepseek-chat`。请求 `https://api.siliconflow.cn/v1/chat/completions`，`Authorization: Bearer <token>`，`max_tokens=4096`、`temperature=0.5`、`stream=false`。系统提示词 `SYSTEM_PROMPT` 用中文约束输出必须是**严格 JSON 且只含 words/grammar/title**。
  - **顺带衔接词库**：`buildUserPrompt` 会先查该 版本+年级+单元 已有哪些词，把它们塞进提示词要求模型"务必包含并补全"，保证 AI 生成的词表不会丢掉老师手工导入的词。
- **`parseRawJson(raw)`**：容忍模型在回复外包 ` ```json ... ``` ` 代码块、前后带解释文字：先剥代码块、再取第一个 `{` 到最后一个 `}`。解析失败抛友好异常。
- **`normalizeContent(root, ...)`**：把模型输出规整成第四节标准结构。单词缺失拼写直接跳过；exercise 里 `answer` 只信选项字母；grammar 缺失时给默认标题 `年级 单元 语法重点`。
- **`savePack(view)`**：content 为空直接拒绝。查同 `version+grade+unit` 是否已有包，有则 updateById 覆盖，无则 insert（主键由 BaseModel 的雪花 ID 自动生成）。返回重新查库后的完整 view。
- **`syncToBank(id)`**：标注 `@Transactional(rollbackFor = Exception.class)`。解析 content：
  - words 数组 → 逐词按四元组 count 判断新增/更新（更新用 `mapper.update(entity, wrapper)` 条件更新，不覆盖 image/audio）；
  - grammar 对象 → 按 `grade+title` 判断新增/覆盖，examples/exercises 用 `objectMapper.writeValueAsString` 序列化后写入。
  - 返回 `PackSyncResult(wordsAdded, wordsUpdated, grammarSynced)`。

> 维护提醒：新增/更新单词、语法都走 MyBatis-Plus LambdaQueryWrapper，实体字段自动映射下划线列。若后续给单词加字段（如例句中文），需同步改 EnglishWord 实体、normalizeContent 与 sync 三处。

### 5.3 既有服务的小改动：`SystemServiceImpl.getSystemAiSetting()`

```java
return info != null && info.getAiSetting() != null
        ? info.getAiSetting()
        : new SystemInfo.AiSetting();
```

历史根因：数据库 t_sys_info 行存在但 ai_setting 列为 NULL（或整行不存在）时，旧代码返回 null；该方法是 @Cacheable（commonCache），而 commonCache 配置为"禁止缓存 null"，于是缓存拦截器直接抛 IllegalArgumentException → 500。这个 bug 让「系统 AI 设置」接口和 AI 问答从无数据环境起就一直不可用。修复后空配置返回空对象（enabled=false），各调用方拿到后自行给出"AI 未启用"的友好提示。

---

## 六、前端拆解

### 6.1 `WordAiManagePage.jsx`（英语板块 → AI 内容生成）

三个区域自上而下：

1. **生成条件卡**：版本/年级/单元三个 Select（常量 `VERSIONS/GRADES/UNITS`，与单词管理页一致）+ 可选主题 Input +「生成整套内容」。三个条件缺一按钮禁用。
2. **生成结果预览卡**（有 preview 才有）：内部组件 `PackPreview` 分三块渲染——核心词汇（antd Table：拼写/音标/释义/例句+中译）、语法讲解（含例句列表）、配套练习（每题为一张小卡片，显示答案 Tag 与解析）。卡片右上按钮：
   - 「保存为内容包」→ save 接口 → 把返回的完整 view 赋回 preview（此时带 id）；
   - 「同步到词库与语法库」（仅 preview.id 存在时显示）→ sync 接口 → 成功 toast 展示新增/更新/语法统计。
3. **历史内容包卡**：筛选（同一组版本/年级/单元）+ 表格（版本/年级/单元/标题/单词数/创建时间/操作）。行操作：查看（拉 detail 后在 Modal 里复用 PackPreview）、同步、删除（Popconfirm）。

约定：

- 所有请求走 `src/api/request.js`（axios 实例），拦截器会解 `{code, data}`，业务代码统一 `dataOf(res) = res?.data`；非 200 会被拦截器统一 `message.error` 并 reject。
- AI 生成单次耗时长，generate 请求显式传 `timeout: 240000`（覆盖实例默认 30s）。
- 复用组件 `PackPreview` 定义为顶层函数组件，避免内联导致详情弹窗重挂载丢失状态。
- 页面里不出现任何 emoji，图标统一用 antd icons。

### 6.2 `AiSettingPage.jsx`（系统管理 → AI 服务设置）

表单字段：启用开关（Switch）/ 模型列表（Select mode="tags"，可下拉选也可手输模型 ID，标签顺序即优先级）/ Token（Input.Password）/ 提示词（TextArea）。

- 加载：`GET /api/system/aiSetting`（token 已被后端脱敏恒为空，所以不参与回显）。
- 保存：`POST /api/system/update`，body 只包 `{ aiSetting: {...} }`——后端 `mergeSysInfo` 对 aiSetting 做**部分字段合并**：token 传空串会被忽略（不会清掉旧 token），要换才填。enabled/models/prompt 则全量覆盖。

---

## 七、维护检查清单（改动前先看这里）

1. **改表**：t_english_ai_pack 相关 DDL 记得**同时改 init-h2.sql 与 init-mysql.sql**；H2 靠重启自动执行，MySQL 需手动在既有库执行 ALTER。
2. **重启后端必须带 preview profile**：不带会连本地 MySQL 报 Connection refused（日志见 `server/api/logs/error/`）。
3. **AI 配置只在 t_sys_info（id=1）的 ai_setting JSON 列**，不在 application.yml。没有系统管理入口前别手改库——直接打开「系统管理 → AI 服务设置」操作即可。
4. **token 永不下发前端**（SystemApi 主动 setToken(null)）。页面里永远拿不到旧 token，属预期行为。
5. **generate 的读超时是 180s**；如果换了更慢的大模型还超时，优先调 `callChat` 里 `SimpleClientHttpRequestFactory.setReadTimeout`。
6. **幂等语义别破坏**：词按 `version+grade+unit+spell` 判同，语法按 `grade+title` 判同。想"重新生成覆盖"，直接再点生成→保存→同步即可，不会产生重复行。
7. 内容包删除只删包，**不回滚**已同步进词库/语法库的数据（设计如此：库数据是"已确认成果"）。
8. 菜单/权限：AI 生成页菜单要求 `english:word:ai`，接口全部同权；AI 设置页菜单与路由要求 `system:role:list`（沿用角色权限既有点位，未新建权限点）。
9. 已知遗留：早期冒烟测试在 t_english_grammar 留下一行「Be动词用法(冒烟)」（三年级），该表暂无管理接口，等语法管理页或该年级同标题再生成覆盖时处理。
