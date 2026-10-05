package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

/**
 * 成长报告生成请求。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class GrowthReportRequest {

	/** 学员ID（管理端生成他人报告时传入；学员端忽略取当前登录） */
	private String studentId;

	/** 学科ID，可空（空表示全科/通用档案） */
	private String subjectId;

	/** 学期键，为空取当前学期 */
	private String semester;

}
