package cn.wisestar.server.ai.service;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import cn.wisestar.server.ai.domain.ChatRequest;
import cn.wisestar.server.ai.domain.ConversationRequest;
import cn.wisestar.server.ai.domain.ConversationResponse;
import cn.wisestar.server.ai.domain.StreamResponseEvent;
import cn.wisestar.server.ai.domain.AiMessage;
import cn.wisestar.server.ai.domain.ModelType;
import reactor.core.publisher.Flux;

/**
 * AI 聊天服务接口（AiChatService）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，service 包）。</p>
 * <p><b>接口职责</b>：定义"具体 AI 服务提供商"的统一抽象能力，用于屏蔽不同 AI 平台
 * （SiliconFlow、OpenAI 等）的差异。当前系统只有 SiliconFlow 一个实现
 * （{@code SiliconflowChatServiceImpl}），未来接入其他平台时新增实现类即可。</p>
 * <p><b>被谁调用</b>：{@code ChatServiceImpl}（通过具体实现类 SiliconflowChatServiceImpl
 * 间接使用）；部分接口由上层 {@link ChatService} 门面透出。</p>
 * <p><b>依赖的服务</b>：无直接依赖；实现类依赖 {@code SystemService}（获取 AI 配置：
 * 是否启用、模型列表、token、prompt）。</p>
 *
 * <p><b>接口方法说明</b>：</p>
 * <ul>
 *   <li>isEnabled——判断 AI 功能是否启用；</li>
 *   <li>createConversation——创建会话（生成 id 与元数据）；</li>
 *   <li>createChatStream——发起流式对话（核心）；</li>
 *   <li>getPrompt（default）——获取系统提示词，默认返回 null；</li>
 *   <li>getSupportedModels（default）——支持的模型列表，默认返回空列表。</li>
 * </ul>
 *
 * @author zzr
 */
public interface AiChatService {

    /**
     * 是否启用。
     *
     * <p><b>功能</b>：判断当前 AI 功能是否已启用（读取系统配置
     * SystemInfo.AiSetting.enabled）。上层在创建会话/发起对话前先调用此方法做开关判断。</p>
     *
     * <p><b>返回值</b>：true=AI 已启用，false=未启用。</p>
     *
     * <p><b>调用链</b>：ChatServiceImpl → 本方法 → SiliconflowChatServiceImpl#isEnabled
     * → systemService.getSystemAiSetting().enabled。</p>
     *
     * @return 是否启用
     */
    boolean isEnabled();

    /**
     * 创建会话。
     *
     * <p><b>功能</b>：创建新会话——生成会话 id（UUID 去横线）、创建时间戳，
     * 并从请求/入参/可用模型列表中确定模型 id，组装元数据（modelType、modelId）。</p>
     *
     * <p><b>参数说明</b>：</p>
     * <ul>
     *   <li>conversationRequest——{@link ConversationRequest}（title、modelType），可为 null；</li>
     *   <li>model——调用方显式指定的模型名（如前端 URL 参数），优先级高于
     *       conversationRequest.modelType（修复：原实现忽略该入参，模型选择失效）。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@link ConversationResponse}（id、createdAt、metaData）。</p>
     *
     * @param conversationRequest 会话请求
     * @param model               显式指定的模型（可空；非空时优先于请求体 modelType）
     * @return 会话响应
     */
    ConversationResponse createConversation(ConversationRequest conversationRequest, String model);

    /**
     * 创建聊天流。
     *
     * <p><b>功能</b>：调用 AI 平台发起流式对话，返回响应式事件流；流结束前通过
     * consumer 回调把完整 AI 回复（AiMessage）交给调用方处理（当前实现为日志输出，
     * 预留了消息持久化扩展点）。</p>
     *
     * <p><b>参数说明</b>：</p>
     * <ul>
     *   <li>chatRequest——{@link ChatRequest}：模型 + 附加消息（历史上下文，最多保留 10 条）；</li>
     *   <li>conversationId——会话 id；</li>
     *   <li>model——模型名（为空时取第一个可用模型）；</li>
     *   <li>consumer——{@link Consumer}&lt;AiMessage&gt;，流完成时回调完整回复消息。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@code Flux<StreamResponseEvent>}——in_progress（增量）/
     * done（结束）/ error（错误）。</p>
     *
     * @param chatRequest    聊天请求
     * @param conversationId 会话ID
     * @return 聊天响应流
     */
    Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model,
            Consumer<AiMessage> consumer);

    /**
     * 获取 Prompt。
     *
     * <p><b>功能</b>：获取当前 AI 服务提供商的系统提示词（system prompt）。
     * 优先使用系统配置的提示词，为空时读取 classpath 下的默认提示词资源
     * （如 prompt/siliconflow.md）。</p>
     *
     * <p><b>参数说明</b>：modelType（模型类型）、modelId（模型 id）。</p>
     *
     * <p><b>返回值</b>：{@link AiMessage}（角色 system + 提示词内容）；如果当前实现
     * 不需要 Prompt，返回 null（默认实现如此）。</p>
     *
     * @param modelType 模型类型
     * @param modelId   模型ID
     * @return Prompt 消息，如果当前实现不需要 Prompt，则返回 null
     */
    default AiMessage getPrompt(String modelType, String modelId) {
        return null;
    }

    /**
     * 获取当前服务支持的模型列表。
     *
     * <p><b>功能</b>：返回当前 AI 服务提供商支持的模型列表（从系统配置
     * SystemInfo.AiSetting.models 转换而来）。</p>
     *
     * <p><b>返回值</b>：{@code List<ModelType>}；默认实现返回空列表，
     * 具体实现类需覆盖此方法。</p>
     *
     * @return 支持的模型ID列表
     */
    default List<ModelType> getSupportedModels() {
        // 默认情况下，返回一个空列表，具体实现类需要覆盖此方法
        return Collections.emptyList();
    }
}
