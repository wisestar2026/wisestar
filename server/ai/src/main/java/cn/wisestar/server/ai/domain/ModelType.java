package cn.wisestar.server.ai.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 模型类型（ModelType）。
 *
 * <p><b>所属模块</b>：ai 模块（AI 对话能力模块，domain 包）。</p>
 * <p><b>类职责</b>：AI 模型在系统内的描述结构——显示名、实际值、描述，
 * 供前端模型选择下拉框展示与回传使用。</p>
 * <p><b>被谁调用</b>：{@code SiliconflowChatServiceImpl#getSupportedModels}
 * （由系统配置的模型字符串列表转换生成）、{@code ChatController#getModels}
 * （作为 GET /api/ai/chat/models 的 JSON 响应元素）。</p>
 * <p><b>依赖</b>：Lombok @Data / @NoArgsConstructor / @AllArgsConstructor——
 * 支持无参与全参构造（displayName, value, description）。</p>
 *
 * <p><b>数据流</b>：系统 AI 设置中配置的模型列表（String）→ getSupportedModels
 * 转换为 List&lt;ModelType&gt; → ChatController 返回 JSON → 前端下拉框渲染；
 * 用户选择后以 value 作为 model 参数回传给流式聊天接口。</p>
 *
 * @author zzr
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelType {
    /**
     * 模型显示名称。
     * <p>展示给用户看的模型名（当前实现与 value 相同，取配置中的模型字符串）。</p>
     */
    private String displayName;

    /**
     * 模型实际值。
     * <p>发送给 AI 平台请求体中的 model 字段值（如 deepseek-chat）；
     * 前端选择后回传给 /api/ai/chat/stream 的 model 参数。</p>
     */
    private String value;

    /**
     * 模型描述。
     * <p>模型说明文案（当前实现固定为 "AI模型: " + 模型名）。</p>
     */
    private String description;
}
