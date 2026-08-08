package cn.wisestar.server.ai.domain;

import lombok.Data;

/**
 * 会话请求（ConversationRequest）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：创建 AI 对话会话时的请求体——会话标题与期望的模型类型。</p>
 * <p><b>被谁调用</b>：{@code ChatController#createConversation}（@RequestBody 绑定，
 * 可空）、{@code SiliconflowChatServiceImpl#createConversation}
 * （读取 modelType 确定模型）。</p>
 * <p><b>依赖</b>：Lombok @Data 自动生成 getter/setter。</p>
 *
 * <p><b>数据流</b>：前端 POST /api/ai/chat/create-conversation（JSON body）
 * → ChatController 反序列化为本对象 → ChatServiceImpl → SiliconflowChatServiceImpl
 * 读取 modelType 写入会话元数据（metaData.modelId）。</p>
 *
 * @author zzr
 */
@Data
public class ConversationRequest {
    /**
     * 会话标题。
     * <p>会话的展示名称（如"数学答疑"），当前实现仅作扩展字段，
     * SiliconflowChatServiceImpl 未使用此字段，仅透传/保留。</p>
     */
    private String title;

    /**
     * 模型类型。
     * <p>期望使用的 AI 模型标识（如 deepseek-chat）。创建会话时若指定，
     * 则写入会话元数据 metaData.modelId；未指定时底层取第一个可用模型，
     * 仍无则兜底 "deepseek-chat"。</p>
     */
    private String modelType;
}
