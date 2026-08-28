package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 错题错误归因请求（学员在查看错题时标注）。
 *
 * @author wisestar
 * @date 2026/8/26
 */
@Data
public class WrongReasonRequest {

	/** 练习明细ID（t_practice_detail.id） */
	private String detailId;

	/** 错误归因（大意/计算错误/知识点不熟/题型不会等） */
	private String reason;

}
