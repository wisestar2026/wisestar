package cn.wisestar.server.core.uitls;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * 国际化辅助，统一管理题库 Excel 模板的 sheet 名称和表头
 */
public final class RepoTemplateI18n {

	private static final ResourceBundleMessageSource MESSAGE_SOURCE = initMessageSource();

	private static final List<Locale> SUPPORTED_LOCALES = Arrays.asList(Locale.SIMPLIFIED_CHINESE, Locale.CHINA,
			Locale.ENGLISH, Locale.US);

	private static final Map<String, Set<String>> ALIAS_CACHE = new ConcurrentHashMap<>();

	private RepoTemplateI18n() {
	}

	/**
	 * 初始化题库模板国际化消息源，加载 messages 资源包并固定使用 UTF-8 编码。
	 *
	 * @return 配置完成的国际化消息源
	 */
	private static ResourceBundleMessageSource initMessageSource() {
		ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
		messageSource.setBasenames("messages");
		messageSource.setDefaultEncoding("UTF-8");
		messageSource.setFallbackToSystemLocale(false);
		return messageSource;
	}

	/**
	 * 将文本去除首尾空白并转为小写，用于表头与名称的忽略大小写匹配。
	 *
	 * @param value 原始文本
	 * @return 规范化后的文本；入参为 null 时返回 null
	 */
	private static String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	/**
	 * 获取指定消息键在各支持语言下的全部别名（含默认文案），结果带缓存。
	 *
	 * @param key 消息资源键
	 * @param defaultMessage 默认文案
	 * @return 规范化后的别名集合
	 */
	private static Set<String> getAliases(String key, String defaultMessage) {
		return ALIAS_CACHE.computeIfAbsent(key, k -> buildAliases(key, defaultMessage, null));
	}

	/**
	 * 获取带占位参数的消息键在各支持语言下的全部别名（含默认文案），按参数区分缓存。
	 *
	 * @param key 消息资源键
	 * @param defaultMessage 默认文案
	 * @param args 消息占位符参数
	 * @return 规范化后的别名集合
	 */
	private static Set<String> getAliases(String key, String defaultMessage, Object[] args) {
		String cacheKey = key + Arrays.deepToString(args);
		return ALIAS_CACHE.computeIfAbsent(cacheKey, k -> buildAliases(key, defaultMessage, args));
	}

	/**
	 * 遍历所有支持的语言解析消息键，构建规范化后的别名集合。
	 *
	 * @param key 消息资源键
	 * @param defaultMessage 默认文案
	 * @param args 消息占位符参数
	 * @return 规范化后的别名集合
	 */
	private static Set<String> buildAliases(String key, String defaultMessage, Object[] args) {
		Set<String> aliases = new LinkedHashSet<>();
		for (Locale locale : SUPPORTED_LOCALES) {
			String message = MESSAGE_SOURCE.getMessage(key, args, defaultMessage, locale);
			if (StringUtils.hasText(message)) {
				aliases.add(normalize(message));
			}
		}
		if (StringUtils.hasText(defaultMessage)) {
			aliases.add(normalize(defaultMessage));
		}
		return aliases.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * 判断输入文本是否与别名集合中的任一项匹配（忽略大小写与首尾空白）。
	 *
	 * @param input 输入文本
	 * @param aliases 别名集合
	 * @return 匹配返回 true，否则返回 false
	 */
	private static boolean matches(String input, Set<String> aliases) {
		return input != null && aliases.contains(normalize(input));
	}

	/**
	 * 按当前请求 Locale 获取国际化消息，缺失时回退默认文案。
	 *
	 * @param key 消息资源键
	 * @param defaultMessage 默认文案
	 * @return 本地化消息文本
	 */
	public static String message(String key, String defaultMessage) {
		return MESSAGE_SOURCE.getMessage(key, null, defaultMessage, LocaleContextHolder.getLocale());
	}

	/**
	 * 按当前请求 Locale 获取带占位参数的国际化消息，缺失时回退默认模板。
	 *
	 * @param key 消息资源键
	 * @param defaultPattern 默认消息模板
	 * @param args 占位符参数
	 * @return 本地化消息文本
	 */
	public static String message(String key, String defaultPattern, Object... args) {
		return MESSAGE_SOURCE.getMessage(key, args, defaultPattern, LocaleContextHolder.getLocale());
	}

	/**
	 * 判断表头文本是否为选项列（如「选项A」）。
	 *
	 * @param cellText 单元格文本
	 * @return 是选项列返回 true，否则返回 false
	 */
	public static boolean isOptionColumn(String cellText) {
		return matches(cellText, OptionHeader.ALL_LABELS);
	}

	/**
	 * 判断表头文本是否为空列（如「空1」）。
	 *
	 * @param cellText 单元格文本
	 * @return 是空列返回 true，否则返回 false
	 */
	public static boolean isBlankColumn(String cellText) {
		return matches(cellText, BlankHeader.ALL_LABELS);
	}

	/**
	 * 生成选项列的本地化表头文案（如「选项A」）。
	 *
	 * @param suffix 选项后缀（A/B/C 等）
	 * @return 本地化表头文案
	 */
	public static String optionLabel(String suffix) {
		return message("repo.template.header.option", "选项{0}", suffix);
	}

	/**
	 * 生成空列的本地化表头文案（如「空1」）。
	 *
	 * @param index 空位序号
	 * @return 本地化表头文案
	 */
	public static String blankLabel(String index) {
		return message("repo.template.header.blank", "空{0}", index);
	}

	/**
	 * 获取题库导出工作簿的本地化名称。
	 *
	 * @return 本地化工作簿名称
	 */
	public static String workbookName() {
		return message("repo.template.workbook.name", "题库");
	}

	private static class OptionHeader {

		private static final List<String> OPTION_SUFFIXES = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");

		private static final Set<String> ALL_LABELS = OPTION_SUFFIXES.stream()
			.flatMap(suffix -> getAliases("repo.template.header.option", "选项{0}", new Object[] { suffix }).stream())
			.collect(Collectors.toCollection(LinkedHashSet::new));

	}

	private static class BlankHeader {

		private static final List<String> BLANK_INDEXES = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
				"11", "12");

		private static final Set<String> ALL_LABELS = BLANK_INDEXES.stream()
			.flatMap(index -> getAliases("repo.template.header.blank", "空{0}", new Object[] { index }).stream())
			.collect(Collectors.toCollection(LinkedHashSet::new));

	}

	public enum SheetType {

		SINGLE_CHOICE("repo.template.sheet.singleChoice", "单选题"),
		MULTIPLE_CHOICE("repo.template.sheet.multipleChoice", "多选题"),
		TRUE_FALSE("repo.template.sheet.trueFalse", "判断题"),
		FILL_BLANK("repo.template.sheet.fillBlank", "填空题"),
		TEXTAREA("repo.template.sheet.textarea", "简答题");

		private final String key;

		private final String defaultLabel;

		private final Set<String> aliases;

		SheetType(String key, String defaultLabel) {
			this.key = key;
			this.defaultLabel = defaultLabel;
			this.aliases = getAliases(key, defaultLabel);
		}

		/**
		 * 获取该题型对应的本地化 sheet 名称。
		 *
		 * @return 本地化 sheet 名称
		 */
		public String displayName() {
			return message(key, defaultLabel);
		}

		/**
		 * 判断给定名称是否匹配该题型的 sheet 名（含多语言别名）。
		 *
		 * @param name sheet 名称
		 * @return 匹配返回 true，否则返回 false
		 */
		public boolean matches(String name) {
			return RepoTemplateI18n.matches(name, aliases);
		}

		/**
		 * 根据 sheet 名称解析出对应的题型枚举。
		 *
		 * @param name sheet 名称
		 * @return 匹配的题型；无匹配时返回 null
		 */
		public static SheetType fromName(String name) {
			return Arrays.stream(values()).filter(type -> type.matches(name)).findFirst().orElse(null);
		}

	}

	public enum HeaderLabel {

		SERIAL_NO("repo.template.header.serialNo", "序号", (map, index) -> map.put("serialNo", index)),
		TITLE("repo.template.header.title", "题干", (map, index) -> map.put("title", index)),
		ANALYSIS("repo.template.header.analysis", "解析", (map, index) -> map.put("examAnalysis", index)),
		SCORE("repo.template.header.score", "分数", (map, index) -> map.put("examScore", index)),
		SINGLE_BLANK_SCORE("repo.template.header.singleBlankScore", "单空分数", (map, index) -> map.put("examScore", index)),
		ANSWER("repo.template.header.answer", "答案", (map, index) -> map.put("examCorrectAnswer", index)),
		TAGS("repo.template.header.tags", "标签", (map, index) -> map.put("tags", index));

		private final String key;

		private final String defaultLabel;

		private final Set<String> aliases;

		private final BiConsumer<Map<String, Integer>, Integer> mapper;

		HeaderLabel(String key, String defaultLabel, BiConsumer<Map<String, Integer>, Integer> mapper) {
			this.key = key;
			this.defaultLabel = defaultLabel;
			this.aliases = getAliases(key, defaultLabel);
			this.mapper = mapper;
		}

		/**
		 * 判断给定表头文本是否匹配该列标签（含多语言别名）。
		 *
		 * @param name 表头文本
		 * @return 匹配返回 true，否则返回 false
		 */
		public boolean matches(String name) {
			return RepoTemplateI18n.matches(name, aliases);
		}

		/**
		 * 将该表头字段对应到目标映射的指定列索引。
		 *
		 * @param target 字段名到列索引的映射
		 * @param columnIndex 列索引
		 */
		public void apply(Map<String, Integer> target, int columnIndex) {
			mapper.accept(target, columnIndex);
		}

		/**
		 * 获取该表头列的本地化标签文本。
		 *
		 * @return 本地化标签文本
		 */
		public String displayLabel() {
			return message(key, defaultLabel);
		}

		/**
		 * 根据表头文本解析出对应的列标签枚举。
		 *
		 * @param text 表头文本
		 * @return 匹配的列标签；无匹配时返回 null
		 */
		public static HeaderLabel fromText(String text) {
			return Arrays.stream(values()).filter(label -> label.matches(text)).findFirst().orElse(null);
		}

	}

}
