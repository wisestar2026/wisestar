package cn.wisestar.server.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 消息（AiMessage）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：通用 AI 消息载体——会话 id、消息角色、消息内容。
 * 两个用途：</p>
 * <ul>
 *   <li>系统提示词（getPrompt 返回值：role=system + prompt 文本）；</li>
 *   <li>流式对话结束时通过 consumer 回调完整 AI 回复
 *       （role=assistant + 拼接后的完整正文）。</li>
 * </ul>
 * <p><b>被谁调用</b>：{@code SiliconflowChatServiceImpl}（getPrompt 构造、流结束构造）、
 * {@code ChatServiceImpl}（consumer 接收 AiMessage 并打印日志）。</p>
 * <p><b>依赖</b>：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor——
 * 支持无参构造与全参构造（conversationId, role, content）。</p>
 *
 * @author zzr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessage {
    /**
     * 会话ID。
     * <p>消息所属的对话会话 id（UUID 去横线格式）；在流式结束回调中
     * 由 createChatStream 的 conversationId 参数透传填充。</p>
     */
    private String conversationId;

    /**
     * 消息角色。
     * <p>取值："system"（系统提示词）/ "assistant"（AI 回复）/ "user"（用户消息）。</p>
     */
    private String role;

    /**
     * 消息内容。
     * <p>消息正文：提示词文本或 AI 完整回复内容。</p>
     */
    private String content;
}
