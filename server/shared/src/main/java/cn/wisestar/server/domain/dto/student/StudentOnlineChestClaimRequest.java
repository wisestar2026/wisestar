package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 在线宝箱领取请求。
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Data
public class StudentOnlineChestClaimRequest {

	/** 目标档位分钟数（30/60/120） */
	private Integer tier;

	/** 学科ID（可空，缺省回退学员有效权限学科） */
	private String subjectId;

}
