package cn.wisestar.server.core.uitls;

/**
 * 知识结构（章节/小节）字段归一化工具。
 *
 * <p>Excel 导入时「学期/版本」常见多种写法（如「上册」「人教版RJ」），而管理端筛选下拉
 * 使用统一口径（学期「上/下」、版本「人教版/苏教版/北师大版/外研版」）。入库前归一化，
 * 避免存值与筛选值不一致导致按条件查不到数据。</p>
 */
public final class KnowledgeValueNormalizer {

	private KnowledgeValueNormalizer() {
	}

	/** 已知教材版本（与前端 VERSION_OPTIONS 一致）。 */
	private static final String[] KNOWN_VERSIONS = { "人教版", "苏教版", "北师大版", "外研版" };

	/**
	 * 学期归一化：含「上」→「上」，含「下」→「下」；其余原样返回。
	 *
	 * @param text 原始文本（如「上册」「下学期」「上」）
	 * @return 归一化后的学期；空文本返回 null
	 */
	public static String term(String text) {
		String term = blankToNull(text);
		if (term == null) {
			return null;
		}
		if (term.contains("上")) {
			return "上";
		}
		if (term.contains("下")) {
			return "下";
		}
		return term;
	}

	/**
	 * 教材版本归一化：命中已知版本（兼容去掉「版」的简称，如 人教/苏教）时返回基础版本名，否则原样返回。
	 *
	 * @param text 原始文本（如「人教版RJ」「人教」「外研版」）
	 * @return 归一化后的版本名；空文本返回 null
	 */
	public static String version(String text) {
		String version = blankToNull(text);
		if (version == null) {
			return null;
		}
		for (String known : KNOWN_VERSIONS) {
			if (version.contains(known) || version.contains(known.replace("版", ""))) {
				return known;
			}
		}
		return version;
	}

	/**
	 * 重点程度归一化：把 Excel「重点程度」列的中文口径映射为系统三档 importance。
	 *
	 * <p>映射规则：核心/核心重点→key（核心并入重点），次重点→minor，重点→key，一般/普通/非重点/非核心→normal；
	 * 已是系统原值（key/minor/normal）时原样返回，历史值 core 收敛为 key；
	 * 无法识别或为空返回 null（不写该字段）。</p>
	 *
	 * @param text 原始文本（如「核心重点」「重点」「次重点」「一般」「非重点」）
	 * @return key/minor/normal；无法识别返回 null
	 */
	public static String importance(String text) {
		String value = blankToNull(text);
		if (value == null) {
			return null;
		}
		switch (value) {
			case "key":
			case "minor":
			case "normal":
				return value;
			case "core":
				return "key";
			default:
				break;
		}
		if (value.contains("非重点") || value.contains("非核心")) {
			return "normal";
		}
		if (value.contains("核心")) {
			return "key";
		}
		if (value.contains("次")) {
			return "minor";
		}
		if (value.contains("重点")) {
			return "key";
		}
		if (value.contains("一般") || value.contains("普通")) {
			return "normal";
		}
		return null;
	}

	private static String blankToNull(String text) {
		return text != null && !text.trim().isEmpty() ? text.trim() : null;
	}
}
