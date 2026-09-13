package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 在线宝箱领取结果（含最新宝箱状态）。
 *
 * @author wisestar
 * @date 2026/9/12
 */
@Data
public class StudentOnlineChestClaimView {

	/** 是否成功 */
	private boolean ok;

	/** 本次实际到账学习币（上限裁剪后） */
	private int coins;

	/** 学习币是否因单科上限被裁剪 */
	private boolean coinsCapped;

	/** 本次是否首次领取（false 表示当日已领取过） */
	private boolean firstTime;

	/** 提示信息 */
	private String message;

	/** 最新宝箱状态 */
	private StudentOnlineChestView onlineChest;

}
