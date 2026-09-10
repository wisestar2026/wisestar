package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 错题重做结果视图。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentWrongRedoView {

	private boolean ok;

	/** 是否作答正确并从错题本移除 */
	private boolean removed;

	/** 本次发放学习币 */
	private int coins;

	/** 本次发放学海积分 */
	private int points;

}
