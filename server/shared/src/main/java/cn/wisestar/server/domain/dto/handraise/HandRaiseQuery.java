package cn.wisestar.server.domain.dto.handraise;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 举手反馈分页查询条件（教师端/题目管理端列表）。
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class HandRaiseQuery extends PageQuery {

	/** 反馈类型 explain / question_error / answer_error（空=全部；error=两类题目报错） */
	private String raiseType;

	/** 处理状态 0待处理 2已处理 3已忽略（null=全部） */
	private Integer status;

	/** 学科 */
	private String subject;

	/** 关键词（学员姓名/学号/题干） */
	private String keyword;

	/** 学员ID */
	private String studentId;

}
