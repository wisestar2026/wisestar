package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员任务完成结果。
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Data
public class StudentTaskCompleteView {

	/** 是否成功 */
	private boolean ok;

	/** 任务 ID */
	private String taskId;

	/** 任务状态（completed） */
	private String status;

	/** 本次实际到账学习币（上限裁剪后） */
	private int coins;

	/** 学习币是否因单科上限被裁剪 */
	private boolean coinsCapped;

	/** 本次是否为首次发放任务奖励 */
	private boolean firstTime;

	/** 提示信息 */
	private String message;

}
