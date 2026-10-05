package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 成长对比视图（基线 vs 当前）。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class GrowthCompareView {

	/** 学员ID */
	private String studentId;

	/** 学科ID */
	private String subjectId;

	/** 学科名称 */
	private String subjectName;

	/** 学期键 */
	private String semester;

	/** 基线检测记录ID */
	private String baselineDetectId;

	/** 基线定格时间 */
	private Date baselineAt;

	/** 基线整体正确率 */
	private Integer baselineAccuracy;

	/** 当前整体正确率（检测记录最新值或练习汇总） */
	private Integer currentAccuracy;

	/** 是否已定格基线 */
	private Boolean hasBaseline;

	/** 基线薄弱点数量 */
	private Integer baselineWeakCount;

	/** 已攻克数量 */
	private Integer resolvedCount;

	/** 仍需巩固数量 */
	private Integer remainingCount;

	/** 新增薄弱数量 */
	private Integer newlyWeakCount;

	/** 练习/检测总题量 */
	private Integer totalQuestionCount;

	/** 总正确数 */
	private Integer totalCorrectCount;

	/** 轨迹事件数 */
	private Integer eventCount;

	/** 逐知识点变化列表 */
	private List<DeltaItem> deltas = new ArrayList<>();

	/**
	 * 单知识点「基线掌握度 → 当前掌握度」变化。
	 */
	@Data
	public static class DeltaItem {

		/** 知识点ID */
		private String kpId;

		/** 知识点名称 */
		private String name;

		/** 基线正确率/掌握度 */
		private Integer baselineAccuracy;

		/** 当前掌握度 */
		private Integer currentMastery;

		/** 变化量（当前 - 基线） */
		private Integer delta;

		/** 是否已攻克 */
		private Boolean resolved;
	}

}
