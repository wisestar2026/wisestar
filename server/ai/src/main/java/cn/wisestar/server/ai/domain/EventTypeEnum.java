package cn.wisestar.server.ai.domain;

/**
 * 事件类型枚举（EventTypeEnum）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：定义流式聊天响应事件（{@link StreamResponseEvent}）的类型，
 * 用于前端区分流式推送中每条数据的语义。</p>
 * <p><b>被谁调用</b>：{@code SiliconflowChatServiceImpl#createChatStream}
 * （构造 StreamResponseEvent 时指定事件类型）；前端 SSE 消费端
 * （根据 eventType 渲染增量内容 / 结束提示 / 错误提示）。</p>
 * <p><b>取值说明</b>：枚举值采用小写下划线命名（in_progress/done/error），
 * 与 SSE 事件序列化后的字符串一致。</p>
 *
 * @author zzr
 */
public enum EventTypeEnum {
    /**
     * 进行中。
     * <p>表示当前事件是 AI 生成的增量内容（content 字段携带正文片段；
     * 对于 DeepSeek 等模型，reasoningContent 字段可能携带推理过程片段）。</p>
     */
    in_progress,

    /**
     * 完成。
     * <p>表示本次流式对话已结束（content 字段通常为空串，作为流的终止信号）。</p>
     */
    done,

    /**
     * 错误。
     * <p>表示本次对话出现错误（content 字段携带错误描述，
     * 如"AI功能未启用"、"HTTP error: 401"、"Network error: ..."）。</p>
     */
    error
}
