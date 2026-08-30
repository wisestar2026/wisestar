package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员学币发放请求（老师手动加学币）。
 *
 * @author wisestar
 * @date 2026/8/29
 */
@Data
public class StudentCoinRequest {

	/** 学员ID（t_student.id） */
	private String studentId;

	/** 学币数量（正加负扣） */
	private Integer coins;

	/** 发放原因 */
	private String reason;

}
