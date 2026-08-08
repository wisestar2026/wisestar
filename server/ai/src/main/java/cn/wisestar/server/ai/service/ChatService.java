package cn.wisestar.server.ai.service;

import cn.wisestar.server.ai.domain.ChatRequest;
import cn.wisestar.server.ai.domain.ConversationRequest;
import cn.wisestar.server.ai.domain.ConversationResponse;
import cn.wisestar.server.ai.domain.ModelType;
import cn.wisestar.server.ai.domain.StreamResponseEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 聊天服务接口（ChatService）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，service 包）。</p>
 * <p><b>接口职责</b>：定义 AI 聊天业务的顶层门面能力：模型列表查询、创建会话、
 * 创建流式聊天。供上层 Controller（{@code ChatController}）调用，
 * 由 {@code ChatServiceImpl} 实现，内部委托给具体 AI 服务提供商实现
 * （当前为 SiliconflowChatServiceImpl）。</p>
 * <p><b>被谁调用</b>：{@code ChatController}（/api/ai/chat/* 各接口）。</p>
 * <p><b>依赖的服务</b>：无直接依赖；实现类依赖 {@link AiChatService} 的具体实现
 * （SiliconflowChatServiceImpl）完成实际 AI 调用。</p>
 *
 * <p><b>数据流</b>：ChatController → ChatService（本接口）→ ChatServiceImpl
 * → AiChatService 实现（SiliconflowChatServiceImpl）→ SiliconFlow 平台 HTTP 接口。</p>
 *
 * @author zzr
 */
public interface ChatService {

    /**
     * 获取所有模型类型。
     *
     * <p><b>功能</b>：返回当前系统可用的 AI 模型列表。AI 未启用时返回空列表；
     * 启用时从系统配置（SystemInfo.AiSetting.models）转换为 {@link ModelType} 列表。</p>
     *
     * <p><b>返回值结构</b>：{@code List<ModelType>}（displayName/value/description）。</p>
     *
     * <p><b>调用链</b>：ChatController#getModels → 本方法 → SiliconflowChatServiceImpl#getSupportedModels。</p>
     *
     * @return 模型类型列表
     */
    List<ModelType> getAllModelTypes();

    /**
     * 创建会话。
     *
     * <p><b>功能</b>：创建一个新的对话会话——生成会话 id（UUID）、确定模型、
     * 组装元数据。AI 未启用时抛出 IllegalStateException。</p>
     *
     * <p><b>参数说明</b>：</p>
     * <ul>
     *   <li>conversationRequest——会话请求（title、modelType），可为 null；</li>
     *   <li>model——模型名兜底参数（请求参数），请求体未指定 modelType 时使用。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@link ConversationResponse}（id/createdAt/metaData）。</p>
     *
     * <p><b>调用链</b>：ChatController#createConversation → 本方法
     * → SiliconflowChatServiceImpl#createConversation。</p>
     *
     * @param conversationRequest 会话请求
     * @param model               模型
     * @return 会话响应
     */
    ConversationResponse createConversation(ConversationRequest conversationRequest, String model);

    /**
     * 创建聊天流。
     *
     * <p><b>功能</b>：发起流式 AI 对话，返回响应式事件流（Flux）。调用方（Controller）
     * 可将其直接序列化为 SSE 推送给前端。</p>
     *
     * <p><b>参数说明</b>：</p>
     * <ul>
     *   <li>chatRequest——{@link ChatRequest}：会话 id、模型、附加消息（历史上下文）；</li>
     *   <li>conversationId——会话 id（透传给底层）；</li>
     *   <li>model——模型名（透传给底层，为空时底层取第一个可用模型）。</li>
     * </ul>
     *
     * <p><b>返回值结构</b>：{@code Flux<StreamResponseEvent>}——事件类型
     * in_progress（增量内容）/ done（完成）/ error（错误，携带错误信息）。</p>
     *
     * <p><b>调用链</b>：ChatController#createChatStream → 本方法 → ChatServiceImpl
     * → SiliconflowChatServiceImpl#createChatStream → SiliconFlow 平台。</p>
     *
     * @param chatRequest    聊天请求
     * @param conversationId 会话ID
     * @param model          模型
     * @return 聊天响应流
     */
    Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model);
}
