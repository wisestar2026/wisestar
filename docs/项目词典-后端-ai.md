# 项目词典 · 后端 ai（AI 能力层：流式对话 / SiliconFlow 接入）

> 本文件由源码清单自动生成后再人工校订，颗粒度到「每个类 + 每个公开方法」。
> 阅读方式：`归属` 段给出模块与包路径；`说明` 段取自类级 Javadoc；`方法` 段逐个列出公开/受保护方法（含 Javadoc 首句）。
>
> 归属规则：模块（Maven module）= 顶层归属；包（package）= 二级归属。
> 相关规则：`注解` 表示框架角色，`注入/字段` 表示直接依赖（协作方）。
> 本文件仅描述代码事实，未收录任何密钥；配置项中的 token 均为占位符。

---
### `ai/src/main/java/cn/wisestar/server/ai/config/AiConfiguration.java`
- 包: `cn.wisestar.server.ai.config`
- 类型: `class AiConfiguration`
- 注解: @Configuration
- **类说明**：
  AI 模块配置类（AiConfiguration）。
  **所属模块**：ai 模块（AI 对话能力模块，config 包）。

  **类职责**：为 AI 模块声明所需的 Spring Bean——主要是用于调用大模型 HTTP 接口的响应式 WebClient（Spring WebFlux 的 WebClient，配合 SiliconflowChatServiceImpl 的流式请求使用）。

  **被谁调用**：Spring 容器在装配 ai 模块时自动加载本配置类；其中声明的 webClient Bean 会被 `SiliconflowChatServiceImpl` 构造器注入使用。

  **依赖的服务**：依赖 Spring Boot 自动装配的 WebClient.Builder（由 spring-boot-starter-webflux 提供），并基于它定制 WebClient 实例。

  **数据流说明**：本类不参与 HTTP 业务请求处理，仅负责 Bean 装配： Spring 启动时执行 @Bean 方法 → 创建并注册 webClient Bean → 注入 SiliconflowChatServiceImpl → 供 AI 流式对话请求使用。
- 方法:
  - `public WebClient webClient(WebClient.Builder builder)`
    创建用于调用 AI 平台的响应式 WebClient Bean。
    **功能**：基于 Spring Boot 提供的 WebClient.Builder 构建 WebClient， 关键定制——将内存缓冲上限（maxInMemorySize）从默认值（256KB）调大到 16MB（16 * 1024 * 1024 字节），避免大模型流式响应的数据块因超过默认 内存缓冲而被 WebClient 丢弃/报错。

    **参数**：builder（WebClient.Builder，由 Spring Boot 自动配置注入， 已携带默认超时、编解码器等设置）。

    **返回值**：配置完成的 WebClient 实例（Spring 单例 Bean）。

    **调用方**：`SiliconflowChatServiceImpl#SiliconflowChatServiceImpl` 构造器注入此 Bean，用于发送 POST /v1/chat/completions 流式请求。

### `ai/src/main/java/cn/wisestar/server/ai/controller/ChatController.java`
- 包: `cn.wisestar.server.ai.controller`
- 类型: `class ChatController`
- 注解: @RestController, @RequestMapping
- **类说明**：
  AI 对话接口（ChatController）。
  **所属模块**：ai 模块（AI 对话能力模块，controller 包）。

  **类职责**：对外暴露 AI 对话相关的 HTTP 接口：模型列表查询、创建对话、 关闭对话（清理缓存）、以及核心的"流式聊天"接口（SSE 流）。

  **请求路径前缀**：类级路径为 `${api.prefix`/ai/chat}（api.prefix 通常为 /api）， 各方法再追加子路径（如 /api/ai/chat/models、/api/ai/chat/stream 等）。

  **被谁调用**：前端 AI 自习室/智能助手对话面板（通过 SSE 或普通请求调用）。

  **依赖的服务**：

  - ChatService——聊天业务门面接口（模型列表、创建对话、创建聊天流）；
  - ConversationCacheService——对话缓存服务（内存缓存消息历史， 默认保留 10 分钟，支持会话关闭清理）。

  **对话机制说明**：本 Controller 使用"无状态会话 + 内存缓存"方案： 创建对话时生成会话 id 并建立缓存；流式聊天接口（GET /stream）接收用户消息 后先写入缓存，再取缓存中的历史消息拼装请求体，最后以 SSE（text/event-stream） 形式流式返回 AI 响应。
- 注入/字段: ChatService chatService, ConversationCacheService conversationCacheService
- 方法:
  - `public List<ModelType> getModels()`
    获取所有可用模型类型列表。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/ai/chat/models（如 /api/ai/chat/models）。

    **功能**：返回当前系统可用的 AI 模型列表（显示名、模型值、描述）， 供前端对话面板的模型选择下拉框使用；若 AI 未启用则返回空列表。

    **请求参数**：无。

    **返回值结构**：`List`（模型类型列表，见 domain 包 ModelType： displayName/value/description）。

    **调用的下层 Service**：ChatService#getAllModelTypes() → `ChatServiceImpl#getAllModelTypes` → `SiliconflowChatServiceImpl#getSupportedModels`。

    **数据流**：GET 请求 → 本方法 → chatService.getAllModelTypes() → 判断 AI 是否启用 → 从 SystemInfo.AiSetting.models 配置转成 ModelType 列表 → JSON 返回。
  - `public ConversationResponse createConversation( @RequestBody(required = false) ConversationRequest conversationRequest, @RequestParam(required = false) String model)`
    创建对话。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/ai/chat/create-conversation （如 /api/ai/chat/create-conversation）。

    **功能**：创建一个新的 AI 对话会话：生成会话 id（UUID），确定使用的模型， 并在内存缓存中建立该会话的消息历史容器，返回会话信息给前端。

    **请求参数**：

    - conversationRequest（@RequestBody，可选）——ConversationRequest： 会话标题（title）、模型类型（modelType）；
    - model（@RequestParam，可选）——模型名，请求体未指定 modelType 时兜底。

    **返回值结构**：ConversationResponse——id（会话 id，UUID 去横线）、 createdAt（创建时间戳毫秒）、metaData（元数据 Map：modelType/modelId）。

    **异常**：AI 未启用时 ChatService.createConversation 抛出 IllegalStateException("AI功能未启用")。

    **调用的下层 Service**：ChatService#createConversation(ConversationRequest, String) → `SiliconflowChatServiceImpl#createConversation`； 随后调用 ConversationCacheService#createConversation(String) 建立缓存。

    **数据流**：POST 请求 → 本方法 → chatService.createConversation → 生成会话 id / 模型元数据 → conversationCacheService.createConversation(id) → 返回 ConversationResponse（JSON）。
  - `public void closeConversation(@RequestParam String conversationId)`
    关闭对话并清理缓存。
    **HTTP 方法 + 完整路径**：POST ${api.prefix}/ai/chat/close-conversation （如 /api/ai/chat/close-conversation?conversationId=xxx）。

    **功能**：按会话 id 关闭对话——将该会话的内存消息缓存从 ConcurrentHashMap 中移除，释放内存。前端在对话面板关闭/会话结束时调用。

    **请求参数**：conversationId（@RequestParam，会话 id）。

    **返回值结构**：无返回值（HTTP 200 空响应体）。

    **调用的下层 Service**：ConversationCacheService#closeConversation(String)。
  - `public Flux<StreamResponseEvent> createChatStream( @RequestParam(value = "content", required = false) String content, @RequestParam(value = "model", required = false) String model, @RequestParam(value = "conversation_id", required = false) String conversationId)`
    创建聊天流——使用 GET 请求和缓存的消息历史，SSE 流式返回。
    **HTTP 方法 + 完整路径**：GET ${api.prefix}/ai/chat/stream （如 /api/ai/chat/stream?content=你好&model=deepseek-chat&conversation_id=xxx）， 响应类型为 `text/event-stream`（Server-Sent Events）。

    **功能**：核心聊天接口。处理流程：

    - 用请求参数组装 ChatRequest（conversationId、model）；
    - 若有新用户消息（content 非空）且会话 id 存在，先写入对话缓存（角色 user）；
    - 从缓存读取该会话的历史消息列表作为上下文；若缓存为空但有 content， 则以当前 content 构造默认用户消息作为上下文；
    - 若最终上下文仍为空（无消息），兜底填入默认消息"请生成一个问卷"；
    - 调用 ChatService.createChatStream 发起流式对话，将 AI 响应事件 （in_progress/done/error）以 SSE 形式推送给前端；
    - doOnNext 钩子：累积 in_progress 增量内容，收到 done 事件时将完整 AI 回复回写会话缓存（角色 assistant），保证多轮对话上下文完整。

    **请求参数**（均为 @RequestParam，可选）：

    - content——用户输入的消息内容；
    - model——使用的 AI 模型名；
    - conversation_id——对话 id（与 create-conversation 返回的 id 对应）。

    **返回值结构**：`Flux`（Reactor 流，自动序列化为 SSE）—— 每条事件为 StreamResponseEvent：eventType（in_progress/done/error）、 content（增量文本）、reasoningContent（DeepSeek 等模型的推理内容）。

    **异常**：AI 未启用 / Token 未配置 / 无可用模型时，流内返回 error 类型事件（由下层 SiliconflowChatServiceImpl 生成），HTTP 连接正常完成。

    **调用的下层 Service**：ChatService#createChatStream(ChatRequest, String, String) → `SiliconflowChatServiceImpl#createChatStream`（内部调用 SiliconFlow 平台 /v1/chat/completions 流式接口）。

    **数据流**：GET SSE 请求 → 本方法（写缓存、取历史、组装 ChatRequest） → chatService.createChatStream → siliconflowChatService.createChatStream → WebClient 请求 SiliconFlow 平台 → 逐条解析 SSE 增量 → Flux → SSE 推送给前端。

### `ai/src/main/java/cn/wisestar/server/ai/domain/AiMessage.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class AiMessage`
- 注解: @Data, @NoArgsConstructor, @AllArgsConstructor
- **类说明**：
  AI 消息（AiMessage）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：通用 AI 消息载体——会话 id、消息角色、消息内容。 两个用途：

  - 系统提示词（getPrompt 返回值：role=system + prompt 文本）；
  - 流式对话结束时通过 consumer 回调完整 AI 回复 （role=assistant + 拼接后的完整正文）。

  **被谁调用**：`SiliconflowChatServiceImpl`（getPrompt 构造、流结束构造）、 `ChatServiceImpl`（consumer 接收 AiMessage 并打印日志）。

  **依赖**：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor—— 支持无参构造与全参构造（conversationId, role, content）。

### `ai/src/main/java/cn/wisestar/server/ai/domain/ChatRequest.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class ChatRequest`
- 注解: @Data
- **类说明**：
  聊天请求（ChatRequest）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：承载一次 AI 聊天请求的数据结构——会话 id、目标模型、 附加消息列表（即发送给 AI 的历史上下文消息序列）。

  **被谁调用**：`ChatController#createChatStream`（组装本对象）、 `SiliconflowChatServiceImpl#createChatStream`（读取 model 与 additionalMessages 组装 AI 平台请求体）。

  **依赖**：Lombok @Data 自动生成 getter/setter。

  **数据流**：Controller 组装 ChatRequest → ChatServiceImpl 透传 → SiliconflowChatServiceImpl 读取字段拼装 /v1/chat/completions 请求体。

### `ai/src/main/java/cn/wisestar/server/ai/domain/ConversationRequest.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class ConversationRequest`
- 注解: @Data
- **类说明**：
  会话请求（ConversationRequest）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：创建 AI 对话会话时的请求体——会话标题与期望的模型类型。

  **被谁调用**：`ChatController#createConversation`（@RequestBody 绑定， 可空）、`SiliconflowChatServiceImpl#createConversation` （读取 modelType 确定模型）。

  **依赖**：Lombok @Data 自动生成 getter/setter。

  **数据流**：前端 POST /api/ai/chat/create-conversation（JSON body） → ChatController 反序列化为本对象 → ChatServiceImpl → SiliconflowChatServiceImpl 读取 modelType 写入会话元数据（metaData.modelId）。

### `ai/src/main/java/cn/wisestar/server/ai/domain/ConversationResponse.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class ConversationResponse`
- 注解: @Data
- **类说明**：
  会话响应（ConversationResponse）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：创建 AI 对话会话成功后的返回结构——会话 id、创建时间戳、 元数据（模型类型/模型 id 等）。前端以会话 id 关联后续的流式聊天请求。

  **被谁调用**：`ChatController#createConversation` 直接作为 JSON 响应体返回； `SiliconflowChatServiceImpl#createConversation` 负责填充字段。

  **依赖**：Lombok @Data 自动生成 getter/setter。

  **数据流**：SiliconflowChatServiceImpl#createConversation 生成并填充 → ChatServiceImpl → ChatController 返回 JSON → 前端保存会话 id → 后续 GET /api/ai/chat/stream?conversation_id=xxx 使用。

### `ai/src/main/java/cn/wisestar/server/ai/domain/EventTypeEnum.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `enum EventTypeEnum`
- **类说明**：
  事件类型枚举（EventTypeEnum）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：定义流式聊天响应事件（StreamResponseEvent）的类型， 用于前端区分流式推送中每条数据的语义。

  **被谁调用**：`SiliconflowChatServiceImpl#createChatStream` （构造 StreamResponseEvent 时指定事件类型）；前端 SSE 消费端 （根据 eventType 渲染增量内容 / 结束提示 / 错误提示）。

  **取值说明**：枚举值采用小写下划线命名（in_progress/done/error）， 与 SSE 事件序列化后的字符串一致。

### `ai/src/main/java/cn/wisestar/server/ai/domain/ModelType.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class ModelType`
- 注解: @Data, @NoArgsConstructor, @AllArgsConstructor
- **类说明**：
  AI 模型类型（ModelType）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：AI 模型在系统内的描述结构——显示名、实际值、描述， 供前端模型选择下拉框展示与回传使用。

  **被谁调用**：`SiliconflowChatServiceImpl#getSupportedModels` （由系统配置的模型字符串列表转换生成）、`ChatController#getModels` （作为 GET /api/ai/chat/models 的 JSON 响应元素）。

  **依赖**：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor—— 支持无参与全参构造（displayName, value, description）。

  **数据流**：系统 AI 设置中配置的模型列表（String）→ getSupportedModels 转换为 List → ChatController 返回 JSON → 前端下拉框渲染； 用户选择后以 value 作为 model 参数回传给流式聊天接口。

### `ai/src/main/java/cn/wisestar/server/ai/domain/StreamResponseEvent.java`
- 包: `cn.wisestar.server.ai.domain`
- 类型: `class StreamResponseEvent`
- 注解: @Data, @NoArgsConstructor, @AllArgsConstructor
- **类说明**：
  流式响应事件（StreamResponseEvent）。
  **所属模块**：ai 模块（AI 对话能力模块，domain 包）。

  **类职责**：SSE 流式响应中的单条事件载体——事件类型、响应内容、 推理内容（针对 DeepSeek 等带思维链的模型）。前端按 eventType 分支处理 content（正文）与 reasoningContent（推理过程）。

  **被谁调用**：`SiliconflowChatServiceImpl#createChatStream` （每个流式数据块映射为一个事件）；`ChatController#createChatStream` 以 Flux 形式序列化为 text/event-stream 推送给前端。

  **依赖**：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor； 另提供简化双参构造器（eventType + content，reasoningContent 为 null）。

  **数据流**：SiliconFlow 平台 SSE 数据块 → 解析为增量文本 → 封装为 StreamResponseEvent → Flux → SSE 推送给前端 → 前端根据 eventType 渲染。
- 方法:
  - `public StreamResponseEvent(EventTypeEnum eventType, String content)`
    简化构造器：仅设置事件类型与响应内容。
    **功能**：快速构造不含推理内容的事件（reasoningContent 保持 null）， 用于 done/error 及普通正文增量事件。

### `ai/src/main/java/cn/wisestar/server/ai/service/AiChatService.java`
- 包: `cn.wisestar.server.ai.service`
- 类型: `interface AiChatService`
- **类说明**：
  AI 聊天服务接口（AiChatService）。
  **所属模块**：ai 模块（AI 对话能力模块，service 包）。

  **接口职责**：定义"具体 AI 服务提供商"的统一抽象能力，用于屏蔽不同 AI 平台 （SiliconFlow、OpenAI 等）的差异。当前系统只有 SiliconFlow 一个实现 （`SiliconflowChatServiceImpl`），未来接入其他平台时新增实现类即可。

  **被谁调用**：`ChatServiceImpl`（通过具体实现类 SiliconflowChatServiceImpl 间接使用）；部分接口由上层 ChatService 门面透出。

  **依赖的服务**：无直接依赖；实现类依赖 `SystemService`（获取 AI 配置： 是否启用、模型列表、token、prompt）。

  **接口方法说明**：

  - isEnabled——判断 AI 功能是否启用；
  - createConversation——创建会话（生成 id 与元数据）；
  - createChatStream——发起流式对话（核心）；
  - getPrompt（default）——获取系统提示词，默认返回 null；
  - getSupportedModels（default）——支持的模型列表，默认返回空列表。

### `ai/src/main/java/cn/wisestar/server/ai/service/ChatService.java`
- 包: `cn.wisestar.server.ai.service`
- 类型: `interface ChatService`
- **类说明**：
  聊天服务接口（ChatService）。
  **所属模块**：ai 模块（AI 对话能力模块，service 包）。

  **接口职责**：定义 AI 聊天业务的顶层门面能力：模型列表查询、创建会话、 创建流式聊天。供上层 Controller（`ChatController`）调用， 由 `ChatServiceImpl` 实现，内部委托给具体 AI 服务提供商实现 （当前为 SiliconflowChatServiceImpl）。

  **被谁调用**：`ChatController`（/api/ai/chat/* 各接口）。

  **依赖的服务**：无直接依赖；实现类依赖 AiChatService 的具体实现 （SiliconflowChatServiceImpl）完成实际 AI 调用。

  **数据流**：ChatController → ChatService（本接口）→ ChatServiceImpl → AiChatService 实现（SiliconflowChatServiceImpl）→ SiliconFlow 平台 HTTP 接口。

### `ai/src/main/java/cn/wisestar/server/ai/service/ConversationCacheService.java`
- 包: `cn.wisestar.server.ai.service`
- 类型: `class ConversationCacheService`
- 注解: @Service
- **类说明**：
  对话缓存服务（ConversationCacheService）。
  **所属模块**：ai 模块（AI 对话能力模块，service 包）。

  **类职责**：在 JVM 内存中缓存用户对话消息（按会话 id 组织），默认保留 10 分钟； 用于支撑"无状态会话 + 内存缓存"的 AI 对话方案——前端通过会话 id 关联历史消息， 无需后端持久化对话记录。提供：创建会话缓存、追加消息、读取历史消息、 关闭会话（清理缓存）、定时清理过期会话。

  **被谁调用**：`ChatController`（create-conversation 创建缓存、 stream 读写消息、close-conversation 清理缓存）。

  **依赖的服务**：无外部服务依赖（纯 JDK 并发工具）。

  **数据流**：

  ```
   POST /api/ai/chat/create-conversation --> ConversationCacheService#createConversation --> conversationMap.put(conversationId, new ConversationCache()) GET /api/ai/chat/stream?content=xxx&conversation_id=yyy --> addMessage（写入） + getMessages（读取历史） POST /api/ai/chat/close-conversation --> closeConversation（移除） 后台调度线程（每分钟） --> cleanExpiredConversations（清理超过 10 分钟未访问的会话）
  ```
- 方法:
  - `public ConversationCacheService()`
    构造器：启动过期清理定时任务。
    **功能**：创建单线程定时调度器，并以固定频率（初始延迟 1 分钟、 每 1 分钟一次）执行 #cleanExpiredConversations()， 自动清理超过 10 分钟未访问的会话缓存，防止内存泄漏。
  - `public void createConversation(String conversationId)`
    创建新对话缓存。
    **功能**：为指定会话 id 在缓存容器中放入一个新的空缓存对象 （消息列表为空、最后访问时间为当前时间）。若该会话已存在则覆盖旧缓存。

    **参数说明**：conversationId——会话 id（由 create-conversation 接口生成， UUID 去横线）。

    **调用链**：ChatController#createConversation → 本方法。
  - `public void addMessage(String conversationId, ChatRequest.EnterMessage message)`
    添加消息到对话。
    **功能**：将一条用户/AI 消息追加到指定会话的缓存消息列表末尾， 并刷新该会话的最后访问时间（用于过期判断）。会话不存在时静默忽略。

    **参数说明**：conversationId——会话 id；message——ChatRequest.EnterMessage （role + content，user 与 assistant 消息都会写入，保证多轮上下文完整）。

    **调用链**：ChatController#createChatStream（写入用户消息时）→ 本方法。
  - `public List<ChatRequest.EnterMessage> getMessages(String conversationId)`
    获取对话历史消息。
    **功能**：返回指定会话的消息历史列表（不可变视图，防止外部修改缓存数据）， 并刷新最后访问时间。会话不存在时返回空列表（而非 null）。

    **参数说明**：conversationId——会话 id。

    **返回值结构**：`List`（可能为空）。 注意：getMessages 返回的是底层缓存的副本+不可变包装，后续组装 AI 请求体时 会取最近 10 条作为上下文。

    **调用链**：ChatController#createChatStream（读取历史上下文）→ 本方法。
  - `public void closeConversation(String conversationId)`
    关闭并清理对话。
    **功能**：从缓存容器中移除指定会话的全部缓存数据（释放内存）。 会话 id 不存在时 remove 为无操作。

    **参数说明**：conversationId——会话 id。

    **调用链**：ChatController#closeConversation → 本方法。
  - `public void addMessage(ChatRequest.EnterMessage message)`
    添加一条消息到列表末尾。
  - `public List<ChatRequest.EnterMessage> getMessages()`
    获取消息列表（不可变视图）。
    先拷贝一份副本再包成 unmodifiableList，避免外部拿到引用后 直接修改缓存内部数据。
  - `public void updateLastAccessTime()`
    刷新最后访问时间为当前时间。
  - `public LocalDateTime getLastAccessTime()`
    获取最后访问时间。

### `ai/src/main/java/cn/wisestar/server/ai/service/impl/ChatServiceImpl.java`
- 包: `cn.wisestar.server.ai.service.impl`
- 类型: `class ChatServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  聊天服务实现类（ChatServiceImpl）。
  **所属模块**：ai 模块（AI 对话能力模块，service/impl 包）。

  **类职责**：实现 ChatService 门面接口，作为上层 Controller 与具体 AI 服务提供商实现（当前为 SiliconflowChatServiceImpl）之间的适配层。 负责：AI 启用开关的前置校验、模型列表透传、会话创建透传、流式对话透传 （并注入一个空的 consumer 回调，当前未做消息持久化，仅日志输出）。

  **被谁调用**：`ChatController`（/api/ai/chat/* 各接口）。

  **依赖的服务**：SiliconflowChatServiceImpl（@Autowired 字段注入， 实际是 AiChatService 的 SiliconFlow 实现）。

  **数据流**：ChatController → ChatServiceImpl（本类）→ SiliconflowChatServiceImpl → SiliconFlow 平台。
- 注入/字段: SiliconflowChatServiceImpl siliconflowChatService
- 方法:
  - `public List<ModelType> getAllModelTypes()`
    获取所有模型类型（实现 ChatService#getAllModelTypes()）。
    **功能**：返回可用 AI 模型列表。前置校验：AI 未启用时记录 warn 日志 并返回空列表（而非抛异常，便于前端模型下拉为空时优雅降级）。

    **返回值结构**：`List`（可能为空）。

    **调用链**：ChatController#getModels → 本方法 → siliconflowChatService.isEnabled / getSupportedModels。
  - `public ConversationResponse createConversation(ConversationRequest conversationRequest, String model)`
    创建会话（实现 ChatService#createConversation(ConversationRequest, String)）。
    **功能**：创建新会话。前置校验：AI 未启用时记录 warn 日志并抛出 IllegalStateException("AI功能未启用")（Controller 收到后转为 500/错误响应）。

    **参数说明**：conversationRequest（会话请求，可为 null）、model（模型名兜底）。 修复：model 参数已透传给 SiliconflowChatServiceImpl，底层按优先级 model → modelType → 首个可用模型确定会话模型。

    **返回值结构**：ConversationResponse（id/createdAt/metaData）。

    **异常**：AI 未启用时抛 IllegalStateException。

    **调用链**：ChatController#createConversation → 本方法 → siliconflowChatService.isEnabled → siliconflowChatService.createConversation。
  - `public Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model)`
    创建聊天流（实现 ChatService#createChatStream）。
    **功能**：发起流式 AI 对话。此处构造一个"空"的 consumer 回调 （当前系统未实现 AI 消息持久化，consumer 仅做 debug 日志输出， 保留后续接入消息保存逻辑的扩展点），然后委托给 SiliconFlow 实现。

    **参数说明**：chatRequest（ChatRequest，含模型与历史上下文）、 conversationId（会话 id）、model（模型名）。

    **返回值结构**：`Flux`（in_progress/done/error）。

    **调用链**：ChatController#createChatStream → 本方法 → siliconflowChatService.createChatStream（consumer 透传）。

### `ai/src/main/java/cn/wisestar/server/ai/service/impl/SiliconflowChatServiceImpl.java`
- 包: `cn.wisestar.server.ai.service.impl`
- 类型: `class SiliconflowChatServiceImpl`
- 注解: @Slf4j, @Service
- **类说明**：
  SiliconFlow 聊天服务实现类（SiliconflowChatServiceImpl）。
  **所属模块**：ai 模块（AI 对话能力模块，service/impl 包）。

  **类职责**：实现 AiChatService 接口，是当前系统唯一的 AI 服务提供商实现。 支持通过 SiliconFlow 平台（硅基流动，https://api.siliconflow.cn）调用多种 AI 模型 （DeepSeek、Qwen、Llama 等），提供流式对话（SSE 风格）能力。 关键设计：配置（是否启用、模型列表、API Token、提示词）从 SystemService 读取，而不是从配置文件获取，管理员可在系统设置页在线修改 AI 配置。

  **被谁调用**：`ChatServiceImpl`（注入本实现类并委托调用）。

  **依赖的服务**：

  - WebClient——响应式 HTTP 客户端（见 AiConfiguration 配置，内存缓冲 16MB）， 用于请求 SiliconFlow 的 /v1/chat/completions 流式接口；
  - ObjectMapper——Jackson JSON 解析器，解析流式响应增量；
  - SystemService（shared 模块接口，rdbms 模块实现）——读取 AI 设置 （SystemInfo.AiSetting：enabled/models/token/prompt）。

  **完整数据流**：

  ```
   ChatController#createChatStream（GET /api/ai/chat/stream，SSE） --> ChatServiceImpl#createChatStream（空 consumer） --> 本类 createChatStream(ChatRequest, conversationId, model, consumer) --> 校验 AI 启用 / token / 模型 --> 组装请求体：system prompt + 历史消息（最多 10 条）+ 采样参数 --> WebClient POST https://api.siliconflow.cn/v1/chat/completions （Authorization: Bearer token，stream=true） --> bodyToFlux(String.class) 逐行接收 SSE 数据块 --> 解析 delta：reasoning_content（DeepSeek 推理）/ content（正文） --> 映射为 Flux（in_progress / done / error） --> 失败时 backoff 重试 3 次，最后追加 done 事件 --> SSE 推送给前端
  ```
- 方法:
  - `public SiliconflowChatServiceImpl(WebClient webClient, ObjectMapper objectMapper, SystemService systemService)`
    构造器：注入 WebClient、ObjectMapper、SystemService 三个依赖。
  - `public boolean isEnabled()`
    是否启用（实现 AiChatService#isEnabled()）。
    **功能**：判断 AI 功能是否启用——读取 AI 设置中的 enabled 字段 （null 视为未启用）。

    **返回值**：true=AI 已启用，false=未启用。

    **调用链**：ChatServiceImpl#getAllModelTypes / createConversation → 本方法 → getAiSetting → systemService.getSystemAiSetting。
  - `public List<ModelType> getSupportedModels()`
    获取支持的模型列表（实现 AiChatService#getSupportedModels()）。
    **功能**：将系统 AI 设置中配置的模型字符串列表（models）转换为 ModelType 列表；未配置模型时返回空列表。 转换规则：displayName=模型名，value=模型名，description="AI模型: " + 模型名。

    **返回值**：`List`（可能为空）。

    **调用链**：ChatServiceImpl#getAllModelTypes → 本方法 → getAiSetting。
  - `public ConversationResponse createConversation(ConversationRequest conversationRequest, String model)`
    创建会话（实现 AiChatService#createConversation(ConversationRequest, String)）。
    **功能**：生成一个新的会话：

    - 会话 id——UUID 随机串去掉横线（32 位十六进制）；
    - 创建时间——System.currentTimeMillis() 毫秒时间戳；
    - 元数据——modelType 固定为 "siliconflow"；modelId 按优先级取 入参 model → 请求体 modelType → 第一个可用模型，均无则兜底 "deepseek-chat"（修复：原实现忽略入参 model，模型选择失效）。

    **参数说明**：conversationRequest——ConversationRequest（title/modelType）， 可为 null；model——调用方显式指定的模型名（可空）。

    **返回值结构**：ConversationResponse（id、createdAt、metaData）。

    **调用链**：ChatServiceImpl#createConversation → 本方法。
  - `public Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model, Consumer<AiMessage> consumer)`
    创建聊天流（实现 AiChatService#createChatStream）——本类核心方法。
    **功能**：调用 SiliconFlow 平台的 /v1/chat/completions 流式接口发起对话， 将响应转换为 Flux 事件流。处理要点：

    - 前置校验：AI 未启用 → error 事件；token 未配置 → error 事件； 无可用模型且未指定 model → error 事件；
    - 组装请求体：model（选中的模型）、stream=true、max_tokens=4096、 temperature=0.7、top_p=0.7；
    - 组装消息列表：system prompt（getPrompt 获取）+ 历史消息 （chatRequest.getAdditionalMessages()，最多保留最近 10 条）；
    - 发送请求：Authorization: Bearer token，5 分钟超时；
    - 解析流式响应（bodyToFlux(String) 按行接收）：

    - "[DONE]"——流结束：通过 consumer 回传完整回复（拼接所有 content）， 发射 done 事件；
    - delta.reasoning_content（DeepSeek 等模型的推理内容）——发射 in_progress 事件，内容放入 reasoningContent 字段；
    - delta.content（正文增量）——累加到 contentList，发射 in_progress 事件；
    - 其他（空块等）——返回 null（被 filter 过滤）。

    - 错误处理：HTTP 错误 → error 事件（含状态码）；网络错误 → error 事件（含消息）；
    - 重试：Retry#backoff 退避重试最多 3 次；
    - 收尾：concatWith 追加一个 done 事件保证流一定以 done 结束。

    **参数说明**：

    - chatRequest——ChatRequest：model + additionalMessages（历史上下文）；
    - conversationId——会话 id（仅用于回传 AiMessage 时标记会话）；
    - model——模型名（为空时取第一个可用模型）；
    - consumer——Consumer：流完成（[DONE]）时回调 完整 AI 回复（角色 assistant）。

    **返回值结构**：`Flux`——in_progress（增量内容， 或 reasoningContent）/ done（结束）/ error（错误信息）。

    **异常**：调用平台失败不会抛出（全部转换为 error 事件）， 但超出 5 分钟超时或重试耗尽时按网络错误处理。

    **调用链**：ChatController#createChatStream → ChatServiceImpl#createChatStream → 本方法 → WebClient → SiliconFlow 平台。
  - `public AiMessage getPrompt(String modelType, String modelId)`
    获取 Prompt（实现 AiChatService#getPrompt(String, String)）。
    **功能**：返回发送给 AI 的系统提示词（system prompt），作为消息列表 的第一条。获取顺序：

    - 优先使用系统 AI 设置中的 prompt（管理员在系统设置页配置）；
    - 为空时读取 classpath 下的默认提示词文件 prompt/siliconflow.md （UTF-8 编码，指导 AI 生成问卷等行为的角色设定）。

    **参数说明**：modelType（模型类型，当前恒为 "siliconflow"）、 modelId（选中的模型 id）——本实现未使用这两个参数区分提示词。

    **返回值**：AiMessage——role="system"，content=提示词文本。

    **异常**：@SneakyThrows 吞掉受检异常（读取资源文件的 IOException）； 若默认提示词文件缺失且未配置自定义 prompt，将抛出 IOException 并转换为运行时异常。
