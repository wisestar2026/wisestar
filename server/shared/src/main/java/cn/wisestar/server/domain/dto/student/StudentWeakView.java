package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 薄弱知识点视图。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentWeakView {

	private String kpId;

	private String name;

	private String subjectId;

	/** 攻克后可获得的学海积分 */
	private int conquerPoints = 12;

	/** 攻克后可获得的学习币 */
	private int conquerCoins = 25;

	/** 当前掌握度 */
	private int mastery;

	public StudentWeakView() {
	}

	public StudentWeakView(String kpId, String name, String subjectId, int mastery) {
		this.kpId = kpId;
		this.name = name;
		this.subjectId = subjectId;
		this.mastery = mastery;
	}

}
