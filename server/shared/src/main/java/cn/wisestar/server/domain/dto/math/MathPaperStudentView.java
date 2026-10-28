package cn.wisestar.server.domain.dto.math;

import lombok.Data;

/**
 * 组卷选人结果（后台管理端按学号/姓名搜索学员）。
 *
 * @author wisestar
 * @date 2026/10/26
 */
@Data
public class MathPaperStudentView {

	private String id;

	/** 学号（即登录账号，如 a000001） */
	private String studentNo;

	private String name;

	private String campus;

}
