package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 小节练习配置（t_section.practice JSON 的强类型视图）。
 *
 * <p>字段缺省策略：mode 缺省 normal；passRate 缺省 80 并收敛到 0-100；
 * unlockNext 缺省 false；preview 缺省题量 3、题型不限。
 * 用于专项练习、小节通关与知识点预习统一出题引擎的策略来源。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
public class SectionPracticeConfig {

	/** 常规模式：候选集为整节绑定题目，不做题量/难度/题型裁剪 */
	public static final String MODE_NORMAL = "normal";

	/** 随机模式：按题量/难度/题型随机抽题 */
	public static final String MODE_RANDOM = "random";

	/** 通关阈值缺省值（正确率百分比） */
	public static final int DEFAULT_PASS_RATE = 80;

	/** 预习例题缺省题量 */
	public static final int DEFAULT_PREVIEW_COUNT = 3;

	/** 出题模式：normal 常规 / random 随机 */
	private String mode;

	/** 随机模式题量 */
	private Integer questionCount;

	/** 难度过滤 */
	private String difficulty;

	/** 题型过滤 */
	private List<String> types;

	/** 通关阈值（正确率百分比） */
	private Integer passRate;

	/** 通关后是否解锁下一小节 */
	private Boolean unlockNext;

	/** 预习例题配置（题量/题型），缺省题量 3、题型不限 */
	private PreviewConfig preview;

	/** 是否为随机出题模式 */
	public boolean isRandom() {
		return MODE_RANDOM.equalsIgnoreCase(mode);
	}

	/** 补全缺省值（解析失败或字段缺失时调用） */
	public void applyDefaults() {
		if (mode == null || mode.trim().isEmpty()) {
			mode = MODE_NORMAL;
		}
		if (passRate == null || passRate < 0) {
			passRate = DEFAULT_PASS_RATE;
		}
		if (passRate > 100) {
			passRate = 100;
		}
		if (unlockNext == null) {
			unlockNext = Boolean.FALSE;
		}
		if (preview == null) {
			preview = new PreviewConfig();
		}
		if (preview.getQuestionCount() == null || preview.getQuestionCount() < 1) {
			preview.setQuestionCount(DEFAULT_PREVIEW_COUNT);
		}
		if (preview.getTypes() == null) {
			preview.setTypes(new ArrayList<>());
		}
	}

	/**
	 * 预习例题配置。
	 */
	@Data
	public static class PreviewConfig {

		/** 预习例题题量 */
		private Integer questionCount;

		/** 预习例题题型过滤（空表示不限） */
		private List<String> types;

	}

}
