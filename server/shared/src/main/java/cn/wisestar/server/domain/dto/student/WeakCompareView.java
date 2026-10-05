package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 薄弱点对比视图（成长基线 vs 当前）。
 *
 * <p>基线取自首次全面检测冻结的薄弱点，当前取自学员 active 薄弱知识点；
 * 仅呈现「已攻克 / 仍薄弱 / 新出现」的集合变化，不做提分数值评测。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class WeakCompareView {

	/** 学科ID（可空，空为全部学科） */
	private String subjectId;

	/** 基线学期键 */
	private String semester;

	/** 是否存在成长基线（首次全面检测） */
	private boolean hasBaseline;

	/** 基线薄弱点数 */
	private int baselineCount;

	/** 当前薄弱点数 */
	private int currentCount;

	/** 已攻克薄弱点数 */
	private int resolvedCount;

	/** 已攻克（基线有、当前无） */
	private List<Item> resolved = new ArrayList<>();

	/** 仍薄弱（基线有、当前仍有） */
	private List<Item> remaining = new ArrayList<>();

	/** 新出现薄弱（基线无、当前有） */
	private List<Item> newlyWeak = new ArrayList<>();

	/**
	 * 对比条目。
	 */
	@Data
	public static class Item {

		/** 知识点ID（基线条目可能为空） */
		private String kpId;

		/** 知识点名称 */
		private String name;

		/** 所属章节名称 */
		private String chapterName;

		/** 基线正确率（0-100，基线条目才有） */
		private Integer baselineAccuracy;

		/** 当前掌握度（0-100，当前薄弱条目才有） */
		private Integer mastery;

	}

}
