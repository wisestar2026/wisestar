package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * AI 服务配置状态视图（用于仪表盘「AI 接口未配置」功能提醒）。
 *
 * <p>只读聚合视图，不落库：在接口层根据系统 AI 设置实时计算，
 * 仅暴露「是否已配置」等布尔状态，不含 token 等敏感值。</p>
 *
 * @author wisestar
 * @date 2026/10/11
 */
@Data
public class AiStatusView {

	/** AI 功能开关是否打开 */
	private boolean enabled;

	/** 是否已填写 API Token（不回显 token 本身） */
	private boolean hasToken;

	/** 是否已配置可用（enabled 且 token 非空） */
	private boolean configured;

	/** 接口地址（可为空，空表示使用默认 SiliconFlow 地址） */
	private String baseUrl;

	/** 首选模型（models 列表第一项，可为空） */
	private String model;

}
