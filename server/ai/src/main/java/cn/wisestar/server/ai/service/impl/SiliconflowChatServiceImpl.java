package cn.wisestar.server.ai.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import cn.wisestar.server.ai.domain.ChatRequest;
import cn.wisestar.server.ai.domain.ConversationRequest;
import cn.wisestar.server.ai.domain.ConversationResponse;
import cn.wisestar.server.ai.domain.ModelType;
import cn.wisestar.server.ai.domain.StreamResponseEvent;
import cn.wisestar.server.ai.domain.AiMessage;
import cn.wisestar.server.ai.domain.EventTypeEnum;
import cn.wisestar.server.ai.service.AiChatService;
import cn.wisestar.server.service.SystemService;
import cn.wisestar.server.domain.dto.SystemInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import reactor.util.retry.Retry;

/**
 * SiliconFlow 聊天服务实现类（SiliconflowChatServiceImpl）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，service/impl 包）。</p>
 * <p><b>类职责</b>：实现 {@link AiChatService} 接口，是当前系统唯一的 AI 服务提供商实现。
 * 支持通过 SiliconFlow 平台（硅基流动，https://api.siliconflow.cn）调用多种 AI 模型
 * （DeepSeek、Qwen、Llama 等），提供流式对话（SSE 风格）能力。
 * 关键设计：配置（是否启用、模型列表、API Token、提示词）从 {@link SystemService}
 * 读取，而不是从配置文件获取，管理员可在系统设置页在线修改 AI 配置。</p>
 * <p><b>被谁调用</b>：{@code ChatServiceImpl}（注入本实现类并委托调用）。</p>
 * <p><b>依赖的服务</b>：</p>
 * <ul>
 *   <li>{@link WebClient}——响应式 HTTP 客户端（见 AiConfiguration 配置，内存缓冲 16MB），
 *       用于请求 SiliconFlow 的 /v1/chat/completions 流式接口；</li>
 *   <li>{@link ObjectMapper}——Jackson JSON 解析器，解析流式响应增量；</li>
 *   <li>{@link SystemService}（shared 模块接口，rdbms 模块实现）——读取 AI 设置
 *       （SystemInfo.AiSetting：enabled/models/token/prompt）。</li>
 * </ul>
 *
 * <p><b>完整数据流</b>：</p>
 * <pre>
 *   ChatController#createChatStream（GET /api/ai/chat/stream，SSE）
 *     --&gt; ChatServiceImpl#createChatStream（空 consumer）
 *     --&gt; 本类 createChatStream(ChatRequest, conversationId, model, consumer)
 *     --&gt; 校验 AI 启用 / token / 模型
 *     --&gt; 组装请求体：system prompt + 历史消息（最多 10 条）+ 采样参数
 *     --&gt; WebClient POST https://api.siliconflow.cn/v1/chat/completions
 *         （Authorization: Bearer token，stream=true）
 *     --&gt; bodyToFlux(String.class) 逐行接收 SSE 数据块
 *     --&gt; 解析 delta：reasoning_content（DeepSeek 推理）/ content（正文）
 *     --&gt; 映射为 Flux&lt;StreamResponseEvent&gt;（in_progress / done / error）
 *     --&gt; 失败时 backoff 重试 3 次，最后追加 done 事件
 *     --&gt; SSE 推送给前端
 * </pre>
 *
 * @author zzr
 */
@Slf4j
@Service
public class SiliconflowChatServiceImpl implements AiChatService {

    /**
     * 响应式 HTTP 客户端（构造器注入，见 AiConfiguration 中声明的 webClient Bean），
     * 用于向 SiliconFlow 平台发送流式对话请求。
     */
    private final WebClient webClient;

    /**
     * Jackson JSON 解析器（Spring 容器自动注入），用于解析流式响应中的 JSON 增量块。
     */
    private final ObjectMapper objectMapper;

    /**
     * 系统服务（构造器注入，shared 模块接口），用于读取 AI 设置
     * （SystemInfo.AiSetting：enabled、models、token、prompt）。
     */
    private final SystemService systemService;

    /**
     * SiliconFlow API 基础 URL（静态常量）。
     * 实际请求地址为 SILICONFLOW_BASE_URL + "/v1/chat/completions"。
     */
    // SiliconFlow API基础URL
    private static final String SILICONFLOW_BASE_URL = "https://api.siliconflow.cn";

    /**
     * 构造器：注入 WebClient、ObjectMapper、SystemService 三个依赖。
     *
     * @param webClient     响应式 HTTP 客户端（用于请求 SiliconFlow 平台）
     * @param objectMapper  Jackson JSON 解析器
     * @param systemService 系统服务（读取 AI 配置）
     */
    public SiliconflowChatServiceImpl(WebClient webClient, ObjectMapper objectMapper, SystemService systemService) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.systemService = systemService;
    }

    /**
     * 获取 AI 配置（私有辅助方法）。
     *
     * <p><b>功能</b>：从 {@link SystemService#getSystemAiSetting()} 读取 AI 设置；
     * 若设置为 null（数据库中未初始化 AI 配置），则创建默认设置
     * （enabled=false），保证后续逻辑不出现 NPE。</p>
     *
     * <p><b>返回值</b>：{@link SystemInfo.AiSetting}（enabled/models/token/prompt 的载体）。</p>
     */
    private SystemInfo.AiSetting getAiSetting() {
        SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
        if (aiSetting == null) {
            log.warn("AI setting is null, creating default setting");
            aiSetting = new SystemInfo.AiSetting();
            aiSetting.setEnabled(false);
        }
        return aiSetting;
    }

    /**
     * 是否启用（实现 {@link AiChatService#isEnabled()}）。
     *
     * <p><b>功能</b>：判断 AI 功能是否启用——读取 AI 设置中的 enabled 字段
     * （null 视为未启用）。</p>
     *
     * <p><b>返回值</b>：true=AI 已启用，false=未启用。</p>
     *
     * <p><b>调用链</b>：ChatServiceImpl#getAllModelTypes / createConversation
     * → 本方法 → getAiSetting → systemService.getSystemAiSetting。</p>
     */
    @Override
    public boolean isEnabled() {
        SystemInfo.AiSetting aiSetting = getAiSetting();
        return aiSetting.getEnabled() != null && aiSetting.getEnabled();
    }

    /**
     * 获取支持的模型列表（实现 {@link AiChatService#getSupportedModels()}）。
     *
     * <p><b>功能</b>：将系统 AI 设置中配置的模型字符串列表（models）转换为
     * {@link ModelType} 列表；未配置模型时返回空列表。
     * 转换规则：displayName=模型名，value=模型名，description="AI模型: " + 模型名。</p>
     *
     * <p><b>返回值</b>：{@code List<ModelType>}（可能为空）。</p>
     *
     * <p><b>调用链</b>：ChatServiceImpl#getAllModelTypes → 本方法 → getAiSetting。</p>
     */
    @Override
    public List<ModelType> getSupportedModels() {
        SystemInfo.AiSetting aiSetting = getAiSetting();
        if (aiSetting.getModels() == null || aiSetting.getModels().isEmpty()) {
            return Collections.emptyList();
        }

        // 将字符串列表转换为ModelType列表
        return aiSetting.getModels().stream()
                .map(model -> new ModelType(model, model, "AI模型: " + model))
                .collect(Collectors.toList());
    }

    /**
     * 创建会话（实现 {@link AiChatService#createConversation(ConversationRequest, String)}）。
     *
     * <p><b>功能</b>：生成一个新的会话：</p>
     * <ol>
     *   <li>会话 id——UUID 随机串去掉横线（32 位十六进制）；</li>
     *   <li>创建时间——System.currentTimeMillis() 毫秒时间戳；</li>
     *   <li>元数据——modelType 固定为 "siliconflow"；modelId 按优先级取
     *       入参 model → 请求体 modelType → 第一个可用模型，均无则兜底
     *       "deepseek-chat"（修复：原实现忽略入参 model，模型选择失效）。</li>
     * </ol>
     *
     * <p><b>参数说明</b>：conversationRequest——{@link ConversationRequest}（title/modelType），
     * 可为 null；model——调用方显式指定的模型名（可空）。</p>
     *
     * <p><b>返回值结构</b>：{@link ConversationResponse}（id、createdAt、metaData）。</p>
     *
     * <p><b>调用链</b>：ChatServiceImpl#createConversation → 本方法。</p>
     */
    @Override
    public ConversationResponse createConversation(ConversationRequest conversationRequest, String model) {
        // 创建一个新的 ConversationResponse 对象
        ConversationResponse response = new ConversationResponse();
        response.setId(UUID.randomUUID().toString().replace("-", ""));
        response.setCreatedAt(System.currentTimeMillis());

        // 设置模型类型和ID
        Map<String, String> metaData = new HashMap<>();
        metaData.put("modelType", "siliconflow");

        // 确定模型：优先显式入参 model，其次请求体 modelType，再退化为第一个可用模型
        String modelId = null;
        if (model != null && !model.trim().isEmpty()) {
            modelId = model.trim();
        } else if (conversationRequest != null && conversationRequest.getModelType() != null) {
            modelId = conversationRequest.getModelType();
        } else {
            List<ModelType> models = getSupportedModels();
            if (!models.isEmpty()) {
                modelId = models.get(0).getValue();
            }
        }
        metaData.put("modelId", modelId != null ? modelId : "deepseek-chat");
        response.setMetaData(metaData);

        return response;
    }

    /**
     * 创建聊天流（实现 {@link AiChatService#createChatStream}）——本类核心方法。
     *
     * <p><b>功能</b>：调用 SiliconFlow 平台的 /v1/chat/completions 流式接口发起对话，
     * 将响应转换为 {@link Flux}&lt;StreamResponseEvent&gt; 事件流。处理要点：</p>
     * <ol>
     *   <li>前置校验：AI 未启用 → error 事件；token 未配置 → error 事件；
     *       无可用模型且未指定 model → error 事件；</li>
     *   <li>组装请求体：model（选中的模型）、stream=true、max_tokens=4096、
     *       temperature=0.7、top_p=0.7；</li>
     *   <li>组装消息列表：system prompt（getPrompt 获取）+ 历史消息
     *       （chatRequest.getAdditionalMessages()，最多保留最近 10 条）；</li>
     *   <li>发送请求：Authorization: Bearer token，5 分钟超时；</li>
     *   <li>解析流式响应（bodyToFlux(String) 按行接收）：
     *       <ul>
     *         <li>"[DONE]"——流结束：通过 consumer 回传完整回复（拼接所有 content），
     *             发射 done 事件；</li>
     *         <li>delta.reasoning_content（DeepSeek 等模型的推理内容）——发射
     *             in_progress 事件，内容放入 reasoningContent 字段；</li>
     *         <li>delta.content（正文增量）——累加到 contentList，发射 in_progress 事件；</li>
     *         <li>其他（空块等）——返回 null（被 filter 过滤）。</li>
     *       </ul></li>
     *   <li>错误处理：HTTP 错误 → error 事件（含状态码）；网络错误 → error 事件（含消息）；</li>
     *   <li>重试：{@link Retry#backoff} 退避重试最多 3 次；</li>
     *   <li>收尾：concatWith 追加一个 done 事件保证流一定以 done 结束。</li>
     * </ol>
     *
     * <p><b>参数说明</b>：</p>
     * <ul>
     *   <li>chatRequest——{@link ChatRequest}：model + additionalMessages（历史上下文）；</li>
     *   <li>conversationId——会话 id（仅用于回传 AiMessage 时标记会话）；</li>
     *   <li>model——模型名（为空时取第一个可用模型）；</li>
     *   <li>consumer——{@link Consumer}&lt;AiMessage&gt;：流完成（[DONE]）时回调
     *       完整 AI 回复（角色 assistant）。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@code Flux<StreamResponseEvent>}——in_progress（增量内容，
     * 或 reasoningContent）/ done（结束）/ error（错误信息）。</p>
     *
     * <p><b>异常</b>：调用平台失败不会抛出（全部转换为 error 事件），
     * 但超出 5 分钟超时或重试耗尽时按网络错误处理。</p>
     *
     * <p><b>调用链</b>：ChatController#createChatStream → ChatServiceImpl#createChatStream
     * → 本方法 → WebClient → SiliconFlow 平台。</p>
     */
    @Override
    public Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model,
            Consumer<AiMessage> consumer) {

        // 检查AI是否启用
        if (!isEnabled()) {
            return Flux.just(new StreamResponseEvent(EventTypeEnum.error, "AI功能未启用"));
        }

        SystemInfo.AiSetting aiSetting = getAiSetting();
        if (StringUtils.isEmpty(aiSetting.getToken())) {
            return Flux.just(new StreamResponseEvent(EventTypeEnum.error, "AI Token未配置"));
        }

        // 使用传入的model参数，如果为空则使用第一个可用模型
        String selectedModel = model;
        if (StringUtils.isEmpty(selectedModel)) {
            List<ModelType> models = getSupportedModels();
            if (!models.isEmpty()) {
                selectedModel = models.get(0).getValue();
            } else {
                return Flux.just(new StreamResponseEvent(EventTypeEnum.error, "没有可用的AI模型"));
            }
        }

        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", selectedModel);
        requestBody.put("stream", true);
        requestBody.put("max_tokens", 4096);
        requestBody.put("temperature", 0.7);
        requestBody.put("top_p", 0.7);

        // 构建消息列表
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统 prompt 消息
        AiMessage prompt = getPrompt("siliconflow", selectedModel);
        if (prompt != null && prompt.getContent() != null) {
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", prompt.getRole());
            systemMessage.put("content", prompt.getContent());
            messages.add(systemMessage);
        }

        // 添加历史消息（最多10条）
        List<ChatRequest.EnterMessage> historyMessages = chatRequest.getAdditionalMessages();
        if (historyMessages != null) {
            int startIndex = Math.max(0, historyMessages.size() - 10);
            for (int i = startIndex; i < historyMessages.size(); i++) {
                ChatRequest.EnterMessage msg = historyMessages.get(i);
                Map<String, String> historyMessage = new HashMap<>();
                historyMessage.put("role", msg.getRole() != null ? msg.getRole() : "user");
                historyMessage.put("content", msg.getContent());
                messages.add(historyMessage);
            }
        }

        requestBody.put("messages", messages);

        // 发送请求
        List<String> contentList = new ArrayList<>();
        return webClient.post()
                .uri(SILICONFLOW_BASE_URL + "/v1/chat/completions")
                .header("Authorization", "Bearer " + aiSetting.getToken())
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMinutes(5))
                .mapNotNull(original -> {
                    // 流式响应每条 SSE 都打 info 会刷爆日志，降为 debug 级别
                    log.debug("AI response: {}", original);
                    if ("[DONE]".equals(original)) {
                        // 将所有的 content 保存到 AiMessage
                        consumer.accept(new AiMessage(conversationId, "assistant", String.join("", contentList)));
                        return new StreamResponseEvent(EventTypeEnum.done, "");
                    }
                    try {
                        JsonNode jsonObject = objectMapper.readTree(original);
                        JsonNode delta = jsonObject.path("choices")
                                .get(0)
                                .path("delta");

                        // 处理DeepSeek的推理内容（如果存在）
                        String reasoningData = delta.path("reasoning_content").asText(null);
                        if (StringUtils.hasText(reasoningData)) {
                            return new StreamResponseEvent(EventTypeEnum.in_progress, "", reasoningData);
                        }

                        String content = delta.path("content").asText(null);
                        if (content != null) {
                            contentList.add(content);
                            return new StreamResponseEvent(EventTypeEnum.in_progress, content);
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("Failed to parse response: {}", original, e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .onErrorResume(e -> {
                    if (e instanceof WebClientResponseException) {
                        WebClientResponseException wre = (WebClientResponseException) e;
                        log.error("HTTP error: {} - {}", wre.getStatusCode(), wre.getResponseBodyAsString());
                        return Flux.just(
                                new StreamResponseEvent(EventTypeEnum.error, "HTTP error: " + wre.getStatusCode()));
                    } else {
                        log.error("Network error", e);
                        return Flux
                                .just(new StreamResponseEvent(EventTypeEnum.error, "Network error: " + e.getMessage()));
                    }
                })
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .concatWith(Flux.just(new StreamResponseEvent(EventTypeEnum.done, "")));
    }

    /**
     * 获取 Prompt（实现 {@link AiChatService#getPrompt(String, String)}）。
     *
     * <p><b>功能</b>：返回发送给 AI 的系统提示词（system prompt），作为消息列表
     * 的第一条。获取顺序：</p>
     * <ol>
     *   <li>优先使用系统 AI 设置中的 prompt（管理员在系统设置页配置）；</li>
     *   <li>为空时读取 classpath 下的默认提示词文件 prompt/siliconflow.md
     *       （UTF-8 编码，指导 AI 生成问卷等行为的角色设定）。</li>
     * </ol>
     *
     * <p><b>参数说明</b>：modelType（模型类型，当前恒为 "siliconflow"）、
     * modelId（选中的模型 id）——本实现未使用这两个参数区分提示词。</p>
     *
     * <p><b>返回值</b>：{@link AiMessage}——role="system"，content=提示词文本。</p>
     *
     * <p><b>异常</b>：@SneakyThrows 吞掉受检异常（读取资源文件的 IOException）；
     * 若默认提示词文件缺失且未配置自定义 prompt，将抛出 IOException 并转换为运行时异常。</p>
     */
    @Override
    @SneakyThrows
    public AiMessage getPrompt(String modelType, String modelId) {
        SystemInfo.AiSetting aiSetting = getAiSetting();

        // 创建一个新的消息对象
        AiMessage message = new AiMessage();
        message.setRole("system");

        // 优先使用系统配置的提示词，如果没有则使用默认提示词
        String prompt = aiSetting.getPrompt();
        if (StringUtils.isEmpty(prompt)) {
            ClassPathResource resource = new ClassPathResource("prompt/siliconflow.md");
            prompt = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        }
        message.setContent(prompt);
        return message;
    }

}
