package cn.wisestar.server.ai.domain;

import lombok.Data;
import java.util.Map;

/**
 * 会话响应（ConversationResponse）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：创建 AI 对话会话成功后的返回结构——会话 id、创建时间戳、
 * 元数据（模型类型/模型 id 等）。前端以会话 id 关联后续的流式聊天请求。</p>
 * <p><b>被谁调用</b>：{@code ChatController#createConversation} 直接作为 JSON 响应体返回；
 * {@code SiliconflowChatServiceImpl#createConversation} 负责填充字段。</p>
 * <p><b>依赖</b>：Lombok @Data 自动生成 getter/setter。</p>
 *
 * <p><b>数据流</b>：SiliconflowChatServiceImpl#createConversation 生成并填充
 * → ChatServiceImpl → ChatController 返回 JSON → 前端保存会话 id
 * → 后续 GET /api/ai/chat/stream?conversation_id=xxx 使用。</p>
 *
 * @author zzr
 */
@Data
public class ConversationResponse {
    /**
     * 会话ID。
     * <p>新会话的唯一标识，格式为 UUID 随机串去掉横线（32 位十六进制）；
     * 前端需保存并在后续流式聊天请求中通过 conversation_id 参数回传。</p>
     */
    private String id;

    /**
     * 创建时间。
     * <p>会话创建时间戳（Long，毫秒值，System.currentTimeMillis() 的存储格式）。</p>
     */
    private Long createdAt;

    /**
     * 元数据。
     * <p>会话附加信息（Map&lt;String,String&gt;）：当前包含
     * "modelType"（恒为 siliconflow）与 "modelId"（选中的模型标识）。</p>
     */
    private Map<String, String> metaData;
}
