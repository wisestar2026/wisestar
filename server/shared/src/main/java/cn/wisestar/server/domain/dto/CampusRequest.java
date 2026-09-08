package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 校区请求（行政管理-校区管理，create/update 共用）。
 *
 * @author wisestar
 * @date 2026/9/8
 */
@Data
public class CampusRequest {

	/**
	 * 校区ID（新增为空，更新必传）。
	 */
	private String id;

	/**
	 * 校区名称（必填，全局唯一）。
	 */
	private String name;

	/**
	 * 状态（1 启用 0 停用；新增默认启用）。
	 */
	private Integer status;

	/**
	 * 备注（选填）。
	 */
	private String remark;

}
