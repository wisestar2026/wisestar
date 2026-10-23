package cn.wisestar.server.domain.dto.handraise;

import lombok.Data;

/**
 * 举手反馈处理请求（标记已处理/已忽略）。
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Data
public class HandRaiseHandleRequest {

	/** 反馈ID */
	private String id;

	/** 目标状态 2已处理 / 3已忽略 */
	private Integer status;

	/** 处理备注 */
	private String handleNote;

}
