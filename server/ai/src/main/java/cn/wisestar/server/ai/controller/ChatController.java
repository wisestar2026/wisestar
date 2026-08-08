package cn.wisestar.server.ai.controller;

import cn.wisestar.server.ai.domain.ChatRequest;
import cn.wisestar.server.ai.domain.ConversationRequest;
import cn.wisestar.server.ai.domain.ConversationResponse;
import cn.wisestar.server.ai.domain.ModelType;
import cn.wisestar.server.ai.domain.StreamResponseEvent;
import cn.wisestar.server.ai.service.ChatService;
import cn.wisestar.server.ai.service.ConversationCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话接口（ChatController）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，controller 包）。</p>
 * <p><b>类职责</b>：对外暴露 AI 对话相关的 HTTP 接口：模型列表查询、创建对话、
 * 关闭对话（清理缓存）、以及核心的"流式聊天"接口（SSE 流）。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/ai/chat}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/ai/chat/models、/api/ai/chat/stream 等）。</p>
 * <p><b>被谁调用</b>：前端 AI 自习室/智能助手对话面板（通过 SSE 或普通请求调用）。</p>
 * <p><b>依赖的服务</b>：</p>
 * <ul>
 *   <li>{@link ChatService}——聊天业务门面接口（模型列表、创建对话、创建聊天流）；</li>
 *   <li>{@link ConversationCacheService}——对话缓存服务（内存缓存消息历史，
 *       默认保留 10 分钟，支持会话关闭清理）。</li>
 * </ul>
 *
 * <p><b>对话机制说明</b>：本 Controller 使用"无状态会话 + 内存缓存"方案：
 * 创建对话时生成会话 id 并建立缓存；流式聊天接口（GET /stream）接收用户消息
 * 后先写入缓存，再取缓存中的历史消息拼装请求体，最后以 SSE（text/event-stream）
 * 形式流式返回 AI 响应。</p>
 */
@RestController
@RequestMapping("${api.prefix}/ai/chat")
public class ChatController {

    /**
     * 聊天业务服务（@Autowired 字段注入，实际注入 ChatServiceImpl）。
     */
    @Autowired
    private ChatService chatService;

    /**
     * 对话缓存服务（@Autowired 字段注入），用于创建/读取/清理会话消息缓存。
     */
    @Autowired
    private ConversationCacheService conversationCacheService;

    /**
     * 获取所有可用模型类型列表。
     *
     * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/ai/chat/models（如 /api/ai/chat/models）。</p>
     *
     * <p><b>功能</b>：返回当前系统可用的 AI 模型列表（显示名、模型值、描述），
     * 供前端对话面板的模型选择下拉框使用；若 AI 未启用则返回空列表。</p>
     *
     * <p><b>请求参数</b>：无。</p>
     *
     * <p><b>返回值结构</b>：{@code List<ModelType>}（模型类型列表，见 domain 包 ModelType：
     * displayName/value/description）。</p>
     *
     * <p><b>调用的下层 Service</b>：{@link ChatService#getAllModelTypes()}
     * → {@code ChatServiceImpl#getAllModelTypes} → {@code SiliconflowChatServiceImpl#getSupportedModels}。</p>
     *
     * <p><b>数据流</b>：GET 请求 → 本方法 → chatService.getAllModelTypes()
     * → 判断 AI 是否启用 → 从 SystemInfo.AiSetting.models 配置转成 ModelType 列表 → JSON 返回。</p>
     *
     * @return 模型类型列表
     */
    @GetMapping("/models")
    public List<ModelType> getModels() {
        return chatService.getAllModelTypes();
    }

    /**
     * 创建对话。
     *
     * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/ai/chat/create-conversation
     * （如 /api/ai/chat/create-conversation）。</p>
     *
     * <p><b>功能</b>：创建一个新的 AI 对话会话：生成会话 id（UUID），确定使用的模型，
     * 并在内存缓存中建立该会话的消息历史容器，返回会话信息给前端。</p>
     *
     * <p><b>请求参数</b>：</p>
     * <ul>
     *   <li>conversationRequest（@RequestBody，可选）——{@link ConversationRequest}：
     *       会话标题（title）、模型类型（modelType）；</li>
     *   <li>model（@RequestParam，可选）——模型名，请求体未指定 modelType 时兜底。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@link ConversationResponse}——id（会话 id，UUID 去横线）、
     * createdAt（创建时间戳毫秒）、metaData（元数据 Map：modelType/modelId）。</p>
     *
     * <p><b>异常</b>：AI 未启用时 ChatService.createConversation 抛出
     * IllegalStateException("AI功能未启用")。</p>
     *
     * <p><b>调用的下层 Service</b>：{@link ChatService#createConversation(ConversationRequest, String)}
     * → {@code SiliconflowChatServiceImpl#createConversation}；
     * 随后调用 {@link ConversationCacheService#createConversation(String)} 建立缓存。</p>
     *
     * <p><b>数据流</b>：POST 请求 → 本方法 → chatService.createConversation
     * → 生成会话 id / 模型元数据 → conversationCacheService.createConversation(id)
     * → 返回 ConversationResponse（JSON）。</p>
     *
     * @param conversationRequest 会话请求体（可选）
     * @param model               模型名（可选，请求参数）
     * @return 会话响应（含会话 id 与元数据）
     */
    @PostMapping("/create-conversation")
    public ConversationResponse createConversation(
            @RequestBody(required = false) ConversationRequest conversationRequest,
            @RequestParam(required = false) String model) {
        ConversationResponse response = chatService.createConversation(conversationRequest, model);

        // 创建对话缓存
        conversationCacheService.createConversation(response.getId());

        return response;
    }

    /**
     * 关闭对话并清理缓存。
     *
     * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/ai/chat/close-conversation
     * （如 /api/ai/chat/close-conversation?conversationId=xxx）。</p>
     *
     * <p><b>功能</b>：按会话 id 关闭对话——将该会话的内存消息缓存从
     * ConcurrentHashMap 中移除，释放内存。前端在对话面板关闭/会话结束时调用。</p>
     *
     * <p><b>请求参数</b>：conversationId（@RequestParam，会话 id）。</p>
     *
     * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
     *
     * <p><b>调用的下层 Service</b>：{@link ConversationCacheService#closeConversation(String)}。</p>
     *
     * @param conversationId 会话 id（请求参数）
     */
    @PostMapping("/close-conversation")
    public void closeConversation(@RequestParam String conversationId) {
        conversationCacheService.closeConversation(conversationId);
    }

    /**
     * 创建聊天流——使用 GET 请求和缓存的消息历史，SSE 流式返回。
     *
     * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/ai/chat/stream
     * （如 /api/ai/chat/stream?content=你好&amp;model=deepseek-chat&amp;conversation_id=xxx），
     * 响应类型为 {@code text/event-stream}（Server-Sent Events）。</p>
     *
     * <p><b>功能</b>：核心聊天接口。处理流程：</p>
     * <ol>
     *   <li>用请求参数组装 {@link ChatRequest}（conversationId、model）；</li>
     *   <li>若有新用户消息（content 非空）且会话 id 存在，先写入对话缓存（角色 user）；</li>
     *   <li>从缓存读取该会话的历史消息列表作为上下文；若缓存为空但有 content，
     *       则以当前 content 构造默认用户消息作为上下文；</li>
     *   <li>若最终上下文仍为空（无消息），兜底填入默认消息"请生成一个问卷"；</li>
     *   <li>调用 ChatService.createChatStream 发起流式对话，将 AI 响应事件
     *       （in_progress/done/error）以 SSE 形式推送给前端；</li>
     *   <li>doOnNext 钩子：累积 in_progress 增量内容，收到 done 事件时将完整
     *       AI 回复回写会话缓存（角色 assistant），保证多轮对话上下文完整。</li>
     * </ol>
     *
     * <p><b>请求参数</b>（均为 @RequestParam，可选）：</p>
     * <ul>
     *   <li>content——用户输入的消息内容；</li>
     *   <li>model——使用的 AI 模型名；</li>
     *   <li>conversation_id——对话 id（与 create-conversation 返回的 id 对应）。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@code Flux<StreamResponseEvent>}（Reactor 流，自动序列化为 SSE）——
     * 每条事件为 {@link StreamResponseEvent}：eventType（in_progress/done/error）、
     * content（增量文本）、reasoningContent（DeepSeek 等模型的推理内容）。</p>
     *
     * <p><b>异常</b>：AI 未启用 / Token 未配置 / 无可用模型时，流内返回
     * error 类型事件（由下层 SiliconflowChatServiceImpl 生成），HTTP 连接正常完成。</p>
     *
     * <p><b>调用的下层 Service</b>：{@link ChatService#createChatStream(ChatRequest, String, String)}
     * → {@code SiliconflowChatServiceImpl#createChatStream}（内部调用 SiliconFlow 平台
     * /v1/chat/completions 流式接口）。</p>
     *
     * <p><b>数据流</b>：GET SSE 请求 → 本方法（写缓存、取历史、组装 ChatRequest）
     * → chatService.createChatStream → siliconflowChatService.createChatStream
     * → WebClient 请求 SiliconFlow 平台 → 逐条解析 SSE 增量
     * → Flux&lt;StreamResponseEvent&gt; → SSE 推送给前端。</p>
     *
     * @param content        用户输入的消息内容（可选）
     * @param model          使用的 AI 模型（可选）
     * @param conversationId 对话 ID（可选）
     * @return 聊天响应流（SSE 事件流）
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<StreamResponseEvent> createChatStream(
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "model", required = false) String model,
            @RequestParam(value = "conversation_id", required = false) String conversationId) {

        // 创建ChatRequest对象
        ChatRequest chatRequest = new ChatRequest();
        chatRequest.setConversationId(conversationId);
        chatRequest.setModel(model);

        // 如果有新的用户消息，添加到缓存
        if (content != null && !content.trim().isEmpty() && conversationId != null) {
            ChatRequest.EnterMessage userMessage = new ChatRequest.EnterMessage();
            userMessage.setRole("user");
            userMessage.setContent(content.trim());
            conversationCacheService.addMessage(conversationId, userMessage);
        }

        // 从缓存获取历史消息
        List<ChatRequest.EnterMessage> cachedMessages = conversationId != null
                ? conversationCacheService.getMessages(conversationId)
                : new ArrayList<>();

        // 如果缓存为空且有内容，创建默认消息
        if (cachedMessages.isEmpty() && content != null && !content.trim().isEmpty()) {
            ChatRequest.EnterMessage defaultMessage = new ChatRequest.EnterMessage();
            defaultMessage.setRole("user");
            defaultMessage.setContent(content.trim());
            cachedMessages = new ArrayList<>();
            cachedMessages.add(defaultMessage);
        }

        // 设置消息历史
        chatRequest.setAdditionalMessages(cachedMessages);

        // 验证必要参数
        if (chatRequest.getAdditionalMessages() == null || chatRequest.getAdditionalMessages().isEmpty()) {
            List<ChatRequest.EnterMessage> messages = new ArrayList<>();
            ChatRequest.EnterMessage defaultMessage = new ChatRequest.EnterMessage();
            defaultMessage.setRole("user");
            defaultMessage.setContent("请生成一个问卷");
            messages.add(defaultMessage);
            chatRequest.setAdditionalMessages(messages);
        }

        // 累积本次 AI 回复的增量内容（in_progress 事件逐段追加，done 时回写缓存）
        StringBuilder assistantContent = new StringBuilder();

        return chatService.createChatStream(chatRequest, conversationId, model)
                .doOnNext(event -> {
                    // 累积 AI 增量内容，供 done 事件回写缓存
                    if ("in_progress".equals(event.getEventType().name()) && event.getContent() != null) {
                        assistantContent.append(event.getContent());
                    }
                    // 流结束：将完整 AI 回复回写会话缓存（角色 assistant），
                    // 使多轮对话能携带上一轮 AI 回复作为上下文（修复：原实现不回写，AI 回复丢失）
                    if ("done".equals(event.getEventType().name()) && conversationId != null) {
                        if (assistantContent.length() > 0) {
                            ChatRequest.EnterMessage assistantMessage = new ChatRequest.EnterMessage();
                            assistantMessage.setRole("assistant");
                            assistantMessage.setContent(assistantContent.toString());
                            conversationCacheService.addMessage(conversationId, assistantMessage);
                        }
                    }
                });
    }
}
