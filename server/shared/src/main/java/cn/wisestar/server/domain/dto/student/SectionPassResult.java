package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 小节通关结果（交卷响应与小节列表展示共用）。
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
public class SectionPassResult {

	/** 本次是否通关 */
	private boolean passed;

	/** 本次是否首次通关 */
	private boolean firstPass;

	/** 本次正确率（百分比） */
	private int rate;

	/** 本次星级 0-5 */
	private int stars;

	/** 历史最佳正确率（百分比） */
	private int bestRate;

	/** 历史最佳星级 0-5 */
	private int bestStars;

	/** 通关阈值（百分比） */
	private int passRate;

	/** 本节通关后是否已解锁下一小节 */
	private boolean unlockedNext;

}
