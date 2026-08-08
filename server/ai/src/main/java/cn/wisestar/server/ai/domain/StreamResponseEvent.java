package cn.wisestar.server.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流式响应事件（StreamResponseEvent）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：SSE 流式响应中的单条事件载体——事件类型、响应内容、
 * 推理内容（针对 DeepSeek 等带思维链的模型）。前端按 eventType 分支处理
 * content（正文）与 reasoningContent（推理过程）。</p>
 * <p><b>被谁调用</b>：{@code SiliconflowChatServiceImpl#createChatStream}
 * （每个流式数据块映射为一个事件）；{@code ChatController#createChatStream}
 * 以 Flux&lt;StreamResponseEvent&gt; 形式序列化为 text/event-stream 推送给前端。</p>
 * <p><b>依赖</b>：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor；
 * 另提供简化双参构造器（eventType + content，reasoningContent 为 null）。</p>
 *
 * <p><b>数据流</b>：SiliconFlow 平台 SSE 数据块 → 解析为增量文本 → 封装为
 * StreamResponseEvent → Flux → SSE 推送给前端 → 前端根据 eventType 渲染。</p>
 *
 * @author zzr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StreamResponseEvent {
    /**
     * 事件类型。
     * <p>{@link EventTypeEnum}：in_progress（增量内容）/ done（结束）/ error（错误）。</p>
     */
    private EventTypeEnum eventType;

    /**
     * 响应内容。
     * <p>in_progress 时携带正文增量片段（可能为空，当只有推理内容时）；
     * error 时携带错误描述；done 时为空串。</p>
     */
    private String content;

    /**
     * 推理内容（用于 DeepSeek 等模型）。
     * <p>DeepSeek-R1 等模型在正式回答前输出思维链（reasoning_content），
     * 单独放入此字段，便于前端区分展示"推理过程"与"正式回答"。</p>
     */
    private String reasoningContent;

    /**
     * 简化构造器：仅设置事件类型与响应内容。
     *
     * <p><b>功能</b>：快速构造不含推理内容的事件（reasoningContent 保持 null），
     * 用于 done/error 及普通正文增量事件。</p>
     *
     * @param eventType 事件类型（in_progress/done/error）
     * @param content   响应内容（正文增量或错误信息）
     */
    public StreamResponseEvent(EventTypeEnum eventType, String content) {
        this.eventType = eventType;
        this.content = content;
    }
}
