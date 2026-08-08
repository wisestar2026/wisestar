package cn.wisestar.server.ai.service.impl;

import cn.wisestar.server.ai.domain.ChatRequest;
import cn.wisestar.server.ai.domain.ConversationRequest;
import cn.wisestar.server.ai.domain.ConversationResponse;
import cn.wisestar.server.ai.domain.ModelType;
import cn.wisestar.server.ai.domain.StreamResponseEvent;
import cn.wisestar.server.ai.domain.AiMessage;
import cn.wisestar.server.ai.service.AiChatService;
import cn.wisestar.server.ai.service.ChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 聊天服务实现类（ChatServiceImpl）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，service/impl 包）。</p>
 * <p><b>类职责</b>：实现 {@link ChatService} 门面接口，作为上层 Controller
 * 与具体 AI 服务提供商实现（当前为 {@link SiliconflowChatServiceImpl}）之间的适配层。
 * 负责：AI 启用开关的前置校验、模型列表透传、会话创建透传、流式对话透传
 * （并注入一个空的 consumer 回调，当前未做消息持久化，仅日志输出）。</p>
 * <p><b>被谁调用</b>：{@code ChatController}（/api/ai/chat/* 各接口）。</p>
 * <p><b>依赖的服务</b>：{@link SiliconflowChatServiceImpl}（@Autowired 字段注入，
 * 实际是 {@link AiChatService} 的 SiliconFlow 实现）。</p>
 *
 * <p><b>数据流</b>：ChatController → ChatServiceImpl（本类）→ SiliconflowChatServiceImpl
 * → SiliconFlow 平台。</p>
 *
 * @author zzr
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    /**
     * SiliconFlow 聊天服务实现（@Autowired 字段注入）。
     * 当前系统唯一的 AiChatService 实现，本类所有能力均委托给它。
     */
    @Autowired
    private SiliconflowChatServiceImpl siliconflowChatService;

    /**
     * 获取所有模型类型（实现 {@link ChatService#getAllModelTypes()}）。
     *
     * <p><b>功能</b>：返回可用 AI 模型列表。前置校验：AI 未启用时记录 warn 日志
     * 并返回空列表（而非抛异常，便于前端模型下拉为空时优雅降级）。</p>
     *
     * <p><b>返回值结构</b>：{@code List<ModelType>}（可能为空）。</p>
     *
     * <p><b>调用链</b>：ChatController#getModels → 本方法
     * → siliconflowChatService.isEnabled / getSupportedModels。</p>
     */
    @Override
    public List<ModelType> getAllModelTypes() {
        // 检查AI是否启用
        if (!siliconflowChatService.isEnabled()) {
            log.warn("AI功能未启用，返回空模型列表");
            return Collections.emptyList();
        }

        // 返回 SiliconFlow 支持的模型
        return siliconflowChatService.getSupportedModels();
    }

    /**
     * 创建会话（实现 {@link ChatService#createConversation(ConversationRequest, String)}）。
     *
     * <p><b>功能</b>：创建新会话。前置校验：AI 未启用时记录 warn 日志并抛出
     * IllegalStateException("AI功能未启用")（Controller 收到后转为 500/错误响应）。</p>
     *
     * <p><b>参数说明</b>：conversationRequest（会话请求，可为 null）、model（模型名兜底）。
     * 修复：model 参数已透传给 SiliconflowChatServiceImpl，底层按优先级
     * model → modelType → 首个可用模型确定会话模型。</p>
     *
     * <p><b>返回值结构</b>：{@link ConversationResponse}（id/createdAt/metaData）。</p>
     *
     * <p><b>异常</b>：AI 未启用时抛 IllegalStateException。</p>
     *
     * <p><b>调用链</b>：ChatController#createConversation → 本方法
     * → siliconflowChatService.isEnabled → siliconflowChatService.createConversation。</p>
     */
    @Override
    public ConversationResponse createConversation(ConversationRequest conversationRequest, String model) {
        // 检查AI是否启用
        if (!siliconflowChatService.isEnabled()) {
            log.warn("AI功能未启用，无法创建对话");
            throw new IllegalStateException("AI功能未启用");
        }

        // 使用 SiliconFlow 服务创建对话，透传 model 参数
        return siliconflowChatService.createConversation(conversationRequest, model);
    }

    /**
     * 创建聊天流（实现 {@link ChatService#createChatStream}）。
     *
     * <p><b>功能</b>：发起流式 AI 对话。此处构造一个"空"的 consumer 回调
     * （当前系统未实现 AI 消息持久化，consumer 仅做 debug 日志输出，
     * 保留后续接入消息保存逻辑的扩展点），然后委托给 SiliconFlow 实现。</p>
     *
     * <p><b>参数说明</b>：chatRequest（{@link ChatRequest}，含模型与历史上下文）、
     * conversationId（会话 id）、model（模型名）。</p>
     *
     * <p><b>返回值结构</b>：{@code Flux<StreamResponseEvent>}（in_progress/done/error）。</p>
     *
     * <p><b>调用链</b>：ChatController#createChatStream → 本方法
     * → siliconflowChatService.createChatStream（consumer 透传）。</p>
     */
    @Override
    public Flux<StreamResponseEvent> createChatStream(ChatRequest chatRequest, String conversationId, String model) {
        // 使用空的 consumer，因为当前系统没有实现消息持久化
        Consumer<AiMessage> emptyConsumer = message -> {
            // 这里可以添加消息保存逻辑，如果需要的话
            log.debug("Received AI message: {}", message.getContent());
        };

        return siliconflowChatService.createChatStream(chatRequest, conversationId, model, emptyConsumer);
    }
}
