package cn.wisestar.server.impl.fsrs;

import cn.wisestar.server.core.constant.ReviewConsts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 作答方式（demand）规则。
 *
 * <p>把英语的 19 种考察角度与知识点练习折算为四种作答方式，并给出中位用时、
 * 快慢阈值倍数与严重度权重。角度未知时按带提示填空处理。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class ReviewDemand {

	private ReviewDemand() {
	}

	private static final Map<String, String> ANGLE_DEMAND;

	/** 中位用时（毫秒） */
	private static final Map<String, Long> MEDIAN;

	/** 慢速倍数 */
	private static final Map<String, Double> HARD_RATIO;

	/** 严重度权重 */
	private static final Map<String, Double> WEIGHT;

	static {
		Map<String, String> angle = new HashMap<>();
		// 单词
		angle.put("word-meaning", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("meaning-word", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("image-word", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("word-listen", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("word-collocation", ReviewConsts.DEMAND_CUED);
		angle.put("word-dictation", ReviewConsts.DEMAND_FULL);
		// 句式
		angle.put("sentence-listen-build", ReviewConsts.DEMAND_APPLICATION);
		angle.put("sentence-build", ReviewConsts.DEMAND_APPLICATION);
		angle.put("sentence-fill", ReviewConsts.DEMAND_CUED);
		angle.put("sentence-scenario", ReviewConsts.DEMAND_CUED);
		angle.put("sentence-zh-en", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("sentence-en-zh", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("sentence-listen-meaning", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("sentence-dictation", ReviewConsts.DEMAND_FULL);
		// 语法
		angle.put("grammar-rule-id", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("grammar-choice", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("grammar-judge", ReviewConsts.DEMAND_RECOGNITION);
		angle.put("grammar-transform", ReviewConsts.DEMAND_CUED);
		angle.put("grammar-rewrite", ReviewConsts.DEMAND_APPLICATION);
		ANGLE_DEMAND = Collections.unmodifiableMap(angle);

		Map<String, Long> median = new HashMap<>();
		median.put(ReviewConsts.DEMAND_RECOGNITION, 6000L);
		median.put(ReviewConsts.DEMAND_CUED, 15000L);
		median.put(ReviewConsts.DEMAND_APPLICATION, 20000L);
		median.put(ReviewConsts.DEMAND_FULL, 25000L);
		MEDIAN = Collections.unmodifiableMap(median);

		Map<String, Double> hard = new HashMap<>();
		hard.put(ReviewConsts.DEMAND_RECOGNITION, 2.0);
		hard.put(ReviewConsts.DEMAND_CUED, 1.7);
		hard.put(ReviewConsts.DEMAND_APPLICATION, 1.75);
		hard.put(ReviewConsts.DEMAND_FULL, 1.8);
		HARD_RATIO = Collections.unmodifiableMap(hard);

		Map<String, Double> weight = new HashMap<>();
		weight.put(ReviewConsts.DEMAND_RECOGNITION, 1.0);
		weight.put(ReviewConsts.DEMAND_CUED, 1.15);
		weight.put(ReviewConsts.DEMAND_APPLICATION, 1.25);
		weight.put(ReviewConsts.DEMAND_FULL, 1.35);
		WEIGHT = Collections.unmodifiableMap(weight);
	}

	/**
	 * 角度折算作答方式。
	 *
	 * @param angle 考察角度
	 * @param source 作答来源（单词模块用于兜底）
	 * @return 作答方式
	 */
	public static String of(String angle, String source) {
		if (angle != null) {
			String demand = ANGLE_DEMAND.get(angle);
			if (demand != null) {
				return demand;
			}
		}
		if (source != null) {
			switch (source) {
				case "word-dictation":
				case "sentence-dictation":
					return ReviewConsts.DEMAND_FULL;
				case "use":
				case "word-use":
				case "scenario":
				case "word-scenario":
					return ReviewConsts.DEMAND_CUED;
				case "learn":
					return ReviewConsts.DEMAND_RECOGNITION;
				default:
					break;
			}
		}
		return ReviewConsts.DEMAND_CUED;
	}

	/**
	 * 是否产出型作答方式。
	 *
	 * @param demand 作答方式
	 * @return true 表示需要产出
	 */
	public static boolean isOutput(String demand) {
		return ReviewConsts.DEMAND_CUED.equals(demand)
				|| ReviewConsts.DEMAND_APPLICATION.equals(demand)
				|| ReviewConsts.DEMAND_FULL.equals(demand);
	}

	/**
	 * 中位用时。
	 *
	 * @param demand 作答方式
	 * @return 毫秒
	 */
	public static long medianMillis(String demand) {
		return MEDIAN.getOrDefault(demand, 15000L);
	}

	/**
	 * 慢速倍数。
	 *
	 * @param demand 作答方式
	 * @return 倍数
	 */
	public static double hardRatio(String demand) {
		return HARD_RATIO.getOrDefault(demand, 1.8);
	}

	/**
	 * 快速阈值：低于该用时评为 Easy。
	 *
	 * @param demand 作答方式
	 * @return 毫秒
	 */
	public static long easyMillis(String demand) {
		return Math.round(medianMillis(demand) * 0.55);
	}

	/**
	 * 慢速阈值：高于该用时评为 Hard。
	 *
	 * @param demand 作答方式
	 * @return 毫秒
	 */
	public static long hardMillis(String demand) {
		return Math.round(medianMillis(demand) * hardRatio(demand));
	}

	/**
	 * 严重度权重。
	 *
	 * @param demand 作答方式
	 * @return 权重
	 */
	public static double weight(String demand) {
		return WEIGHT.getOrDefault(demand, 1.15);
	}

	/**
	 * 成功稳定度增益。
	 *
	 * @param demand 作答方式
	 * @return 增益系数
	 */
	public static double successGain(String demand) {
		return isOutput(demand) ? 1.08 : 1.0;
	}

	/**
	 * 失败稳定度惩罚。
	 *
	 * @param demand 作答方式
	 * @return 惩罚系数
	 */
	public static double failurePenalty(String demand) {
		return isOutput(demand) ? 0.92 : 1.0;
	}

}
