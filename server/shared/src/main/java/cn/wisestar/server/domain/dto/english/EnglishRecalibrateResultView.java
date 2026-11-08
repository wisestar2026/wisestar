package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语难度重算结果。
 *
 * <p>批量重算时逐条判定：缺少标签（题干、答案、选项均无英文内容）的题目跳过并计数，
 * 便于管理端一眼看出「更新了多少、跳过多少、失败多少」。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
public class EnglishRecalibrateResultView {

	/** 实际更新难度的数量 */
	private int updated;

	/** 缺少标签被跳过的数量 */
	private int skipped;

	/** 计算或落库失败的数量 */
	private int failed;

	/**
	 * 累加一次结果。
	 *
	 * @param delta 单条结果（updated / skipped / failed 只有一项为 1）
	 */
	public void add(EnglishRecalibrateResultView delta) {
		this.updated += delta.updated;
		this.skipped += delta.skipped;
		this.failed += delta.failed;
	}
}
