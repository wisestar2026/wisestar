package cn.wisestar.server.ai.domain;

import lombok.Data;
import java.util.List;

/**
 * 聊天请求（ChatRequest）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：承载一次 AI 聊天请求的数据结构——会话 id、目标模型、
 * 附加消息列表（即发送给 AI 的历史上下文消息序列）。</p>
 * <p><b>被谁调用</b>：{@code ChatController#createChatStream}（组装本对象）、
 * {@code SiliconflowChatServiceImpl#createChatStream}（读取 model 与 additionalMessages
 * 组装 AI 平台请求体）。</p>
 * <p><b>依赖</b>：Lombok @Data 自动生成 getter/setter。</p>
 *
 * <p><b>数据流</b>：Controller 组装 ChatRequest → ChatServiceImpl 透传
 * → SiliconflowChatServiceImpl 读取字段拼装 /v1/chat/completions 请求体。</p>
 *
 * @author zzr
 */
@Data
public class ChatRequest {
    /**
     * 会话ID。
     * <p>标识所属对话会话（与 ConversationCacheService 的缓存 key 对应），
     * 由 create-conversation 接口生成，格式为 UUID 去横线（32 位十六进制）。</p>
     */
    private String conversationId;

    /**
     * 模型名称。
     * <p>本次对话使用的 AI 模型标识（如 deepseek-chat、Qwen 等，值取决于
     * 系统 AI 设置中配置的模型列表）；为空时由底层实现取第一个可用模型。</p>
     */
    private String model;

    /**
     * 附加消息列表（包含历史会话记录）。
     * <p>发送给 AI 的消息序列（含历史上下文），元素为 {@link EnterMessage}
     * （role + content）。底层实现会截取最近 10 条发送给 AI 平台。</p>
     */
    private List<EnterMessage> additionalMessages;

    /**
     * 消息内部类（EnterMessage）：一条对话消息。
     *
     * <p><b>类职责</b>：表示单条聊天消息，包含消息角色与消息内容。
     * role 通常为 "user"（用户）或 "assistant"（AI）；content 为消息文本。</p>
     *
     * <p><b>被谁调用</b>：ChatController 构造用户消息、ConversationCacheService
     * 缓存消息、SiliconflowChatServiceImpl 拼装平台请求消息。</p>
     */
    @Data
    public static class EnterMessage {
        /**
         * 消息角色。
         * <p>取值："user"（用户消息）/ "assistant"（AI 回复）/ "system"（系统提示），
         * 对应 AI 平台消息角色的约定；缺失时底层默认按 "user" 处理。</p>
         */
        private String role;

        /**
         * 消息内容。
         * <p>消息正文文本，为用户输入或 AI 生成的回答内容。</p>
         */
        private String content;
    }
}
