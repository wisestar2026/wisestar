package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

/**
 * 学习轨迹查询条件。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class GrowthTimelineQuery {

	/** 学员ID（管理端查询他人时传入；学员端忽略取当前登录） */
	private String studentId;

	/** 学科ID，可空 */
	private String subjectId;

	/** 起始日期 yyyy-MM-dd，可空 */
	private String from;

	/** 结束日期 yyyy-MM-dd，可空 */
	private String to;

}
