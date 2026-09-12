package cn.wisestar.server.core.constant;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 小节-题库绑定用途常量（t_section_repo.usage_type）。
 *
 * <p>用途决定绑定题库服务的学员端场景：
 * 预习（{@link #PREVIEW}）只取预习专用与通用题库；
 * 专项练习与小节通关（{@link #PRACTICE}）只取练习专用与通用题库。
 * 历史数据缺省按通用处理。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
public final class SectionRepoUsage {

	/** 预习专用 */
	public static final String PREVIEW = "preview";

	/** 练习专用 */
	public static final String PRACTICE = "practice";

	/** 通用（预习与练习共用） */
	public static final String BOTH = "both";

	private static final Set<String> VALID = new HashSet<>(
			Arrays.asList(PREVIEW, PRACTICE, BOTH));

	private SectionRepoUsage() {
	}

	/**
	 * 归一化用途：空值或非法值统一回退为通用。
	 *
	 * @param usage 原始用途
	 * @return 合法用途（preview/practice/both）
	 */
	public static String normalize(String usage) {
		if (usage == null) {
			return BOTH;
		}
		String trimmed = usage.trim().toLowerCase();
		return VALID.contains(trimmed) ? trimmed : BOTH;
	}

	/**
	 * 判断用途是否属于预习链路可用范围（预习专用或通用）。
	 *
	 * @param usage 原始用途
	 * @return 预习可用返回 true
	 */
	public static boolean availableForPreview(String usage) {
		String normalized = normalize(usage);
		return PREVIEW.equals(normalized) || BOTH.equals(normalized);
	}

	/**
	 * 判断用途是否属于练习链路可用范围（练习专用或通用）。
	 *
	 * @param usage 原始用途
	 * @return 练习/通关可用返回 true
	 */
	public static boolean availableForPractice(String usage) {
		String normalized = normalize(usage);
		return PRACTICE.equals(normalized) || BOTH.equals(normalized);
	}

}
