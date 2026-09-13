package cn.wisestar.server.domain.dto.student;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import lombok.Data;

/**
 * 学员每日签到视图。
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Data
public class StudentCheckinView {

	/** 今日是否已签到 */
	private boolean checkedToday;

	/** 本次实际到账学习币（上限裁剪后；未签到时为签到可得值） */
	private int coins = StudentRewardConstants.reward(StudentRewardConstants.ACTION_DAILY_CHECKIN, null)[0];

	/** 学习币是否因单科上限被裁剪 */
	private boolean coinsCapped;

	/** 本次是否首次签到（false 表示今日已签到） */
	private boolean firstTime;

	/** 提示信息 */
	private String message;

}
