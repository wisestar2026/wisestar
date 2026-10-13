package cn.wisestar.server.core.constant;

import cn.wisestar.server.domain.dto.SystemInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 分场景系统提示词目录。
 *
 * <p>系统 AI 配置（{@code t_sys_info.ai_setting}）中的 {@code prompts} 是以本类 key 为键的映射。
 * 某个场景在配置中留空（或未配置）时，回退到本类的内置默认提示词；
 * 历史遗留字段 {@code aiSetting.prompt} 仅作为「AI 问答」「学习轨迹·每日叙述」两个场景的兜底。</p>
 *
 * @author wisestar
 */
public final class AiPromptKeys {

	private AiPromptKeys() {
	}

	/** AI 问答（智能陪学）/ 学员对话、学习轨迹报告生成共用 */
	public static final String QA = "qa";

	/** 学习轨迹·每日叙述（按天生成的一句话学习轨迹） */
	public static final String STUDY_DAILY = "study_daily";

	/** 学习总结（面向家长的阶段反馈） */
	public static final String STUDY_SUMMARY = "study_summary";

	/** 英语·单词情景对话 */
	public static final String EN_SCENARIO = "en_scenario";

	/** 英语·重点句式情景 */
	public static final String EN_PATTERN = "en_pattern";

	/** 英语·单元内容生成（AI 备课包） */
	public static final String EN_PACK = "en_pack";

	/**
	 * 提示词元数据：key、中文名称、用途说明、内置默认提示词。
	 */
	public static class PromptMeta {

		private final String key;

		private final String label;

		private final String description;

		private final String defaultPrompt;

		public PromptMeta(String key, String label, String description, String defaultPrompt) {
			this.key = key;
			this.label = label;
			this.description = description;
			this.defaultPrompt = defaultPrompt;
		}

		public String getKey() {
			return key;
		}

		public String getLabel() {
			return label;
		}

		public String getDescription() {
			return description;
		}

		public String getDefaultPrompt() {
			return defaultPrompt;
		}
	}

	/** 「AI 问答」内置默认提示词由 ai 模块的 {@code prompt/siliconflow.md} 提供，此处留空。 */
	private static final String QA_DEFAULT = "";

	private static final String STUDY_DAILY_DEFAULT =
			"你是一位耐心的学习教练，擅长用简洁、正向、鼓励的中文描述孩子每天的学习内容。";

	private static final String STUDY_SUMMARY_DEFAULT =
			"你是一位耐心细致的学习教练，擅长用简洁、鼓励的语言向家长反馈孩子的学习情况。";

	private static final String EN_SCENARIO_DEFAULT =
			"你是一名资深小学英语教研员，擅长把单元词汇编入真实生活情景对话。"
					+ "用户会给你：年级、单元，以及本单元要考查的单词。请据此编写情景对话练习，直接输出一个 JSON 对象，"
					+ "不要输出任何解释、前后缀或代码块之外的文字。\n"
					+ "JSON 结构必须严格如下：\n"
					+ "{\n"
					+ "  \"scenarios\": [\n"
					+ "    {\n"
					+ "      \"title\": \"情景标题(中文简短，如：在水果店)\",\n"
					+ "      \"words\": [\"apple\", \"buy\"],\n"
					+ "      \"lines\": [\n"
					+ "        {\"speaker\": \"A\", \"en\": \"I want to buy an ___.\", \"zh\": \"我想买一个苹果。\", \"answers\": [\"apple\"]},\n"
					+ "        {\"speaker\": \"B\", \"en\": \"OK, here you are.\", \"zh\": \"好的，给你。\", \"answers\": []}\n"
					+ "      ]\n"
					+ "    }\n"
					+ "  ]\n"
					+ "}\n"
					+ "要求：\n"
					+ "1) 对话自然、口语化、符合该年级水平；\n"
					+ "2) 被挖空的单词必须是给定单词表里的单词，用 ___ (三个下划线) 表示；\n"
					+ "3) 每行 en 的 ___ 数量必须与该行 answers 数组长度完全一致，answers 小写；\n"
					+ "4) 一行可有 0-2 个空，不要连续挖空相邻单词；\n"
					+ "5) 所有字符串中的双引号必须转义，输出必须是合法 JSON。";

	private static final String EN_PATTERN_DEFAULT =
			"你是一名资深小学英语教研员，擅长把单元重点句式编入真实生活情景对话。"
					+ "用户会给你：年级、单元，以及本单元要考查的重点句式。请据此编写情景对话练习，直接输出一个 JSON 对象，"
					+ "不要输出任何解释、前后缀或代码块之外的文字。\n"
					+ "JSON 结构必须严格如下：\n"
					+ "{\n"
					+ "  \"scenarios\": [\n"
					+ "    {\n"
					+ "      \"title\": \"情景标题(中文简短，如：开学第一天)\",\n"
					+ "      \"context\": \"一句话情景说明(中文)\",\n"
					+ "      \"patterns\": [\"What's your name?\", \"My name is Lingling.\"],\n"
					+ "      \"turns\": [\n"
					+ "        {\"speaker\": \"A\", \"en\": \"Hello!\", \"zh\": \"你好！\"},\n"
					+ "        {\"speaker\": \"B\", \"cue\": \"告诉对方你叫玲玲\", \"answer\": \"My name is Lingling.\", "
					+ "\"options\": [\"My name is Lingling.\", \"How old are you?\", \"It's my schoolbag.\", \"Goodbye.\"], "
					+ "\"explanation\": \"自我介绍用 My name is...\"}\n"
					+ "      ]\n"
					+ "    }\n"
					+ "  ]\n"
					+ "}\n"
					+ "要求：\n"
					+ "1) 每个情景 4-8 轮，A、B 交替，场景生活化、自然连贯；\n"
					+ "2) 至少一半轮次为作答轮：只给 cue，不给 en；answer 必须是给定重点句式之一；\n"
					+ "3) options 恰好 4 个，包含 answer 与 3 个明显不合本情景的干扰项，干扰项也要是合法的英文句子；\n"
					+ "4) 已给出轮次必须有 en 与 zh；cue 用中文描述学员要表达的意思；\n"
					+ "5) explanation 用简短中文说明选择依据；所有字符串中的双引号必须转义，输出必须是合法 JSON。";

	private static final String EN_PACK_DEFAULT =
			"你是一名资深小学英语教研员，精通各版本教材同步单元教学。"
					+ "用户会给你：教材版本、年级、单元（及主题）。请为该单元生成同步学习内容包，直接输出一个 JSON 对象，不要输出任何解释、前后缀或 Markdown 代码块之外的文字。\n"
					+ "JSON 结构必须严格如下：\n"
					+ "{\n"
					+ "  \"title\": \"单元学习内容标题(简短)\",\n"
					+ "  \"words\": [\n"
					+ "    {\"spell\": \"apple\", \"phonetic\": \"/ˈæpl/\", \"meaning\": \"n. 苹果\", \"example\": \"I eat an apple every day.\", \"exampleZh\": \"我每天吃一个苹果。\"}\n"
					+ "  ],\n"
					+ "  \"grammar\": {\n"
					+ "    \"title\": \"语法点标题\",\n"
					+ "    \"content\": \"面向小学生的中文语法讲解，简明易懂，配 1-2 个英文小例句\",\n"
					+ "    \"examples\": [{\"en\": \"英文例句\", \"zh\": \"中文翻译\"}],\n"
					+ "    \"exercises\": [{\"q\": \"题干\", \"options\": [\"A. xxx\", \"B. xxx\", \"C. xxx\", \"D. xxx\"], \"answer\": \"A\", \"explanation\": \"解析\"}]\n"
					+ "  }\n"
					+ "}\n"
					+ "要求：\n"
					+ "1) words 为单元核心词汇，spell 全小写，meaning 中文释义含词性，phoenetic 用 IPA 音标，例句简单符合该年级水平；\n"
					+ "2) exercises 为单选题，3-5 题，四个选项，answer 只填选项字母（A/B/C/D），且选项字母与选项内容一致；\n"
					+ "3) 单词表与语法点必须贴合该单元主题；\n"
					+ "4) 所有字符串中的双引号必须转义，输出必须是合法 JSON。";

	private static final List<PromptMeta> CATALOG = List.of(
			new PromptMeta(QA, "AI 问答（智能陪学）", "学员对话问答与学习轨迹报告生成；留空使用内置默认（siliconflow.md）。",
					QA_DEFAULT),
			new PromptMeta(STUDY_DAILY, "学习轨迹·每日叙述", "按天生成的一句话学习轨迹，需为纯文字、不含数字。", STUDY_DAILY_DEFAULT),
			new PromptMeta(STUDY_SUMMARY, "学习总结（家长反馈）", "阶段学习总结，面向家长口吻。", STUDY_SUMMARY_DEFAULT),
			new PromptMeta(EN_SCENARIO, "英语·单词情景对话", "把单元词汇编入生活情景对话并挖空，输出严格 JSON。",
					EN_SCENARIO_DEFAULT),
			new PromptMeta(EN_PATTERN, "英语·重点句式情景", "把单元重点句式编入情景选择练习，输出严格 JSON。",
					EN_PATTERN_DEFAULT),
			new PromptMeta(EN_PACK, "英语·单元内容生成", "为单元同步生成词汇/语法学习内容包，输出严格 JSON。", EN_PACK_DEFAULT));

	/**
	 * 返回全部场景提示词元数据（key、名称、说明、内置默认提示词）。
	 *
	 * @return 提示词目录副本
	 */
	public static List<PromptMeta> catalog() {
		return new ArrayList<>(CATALOG);
	}

	/**
	 * 查询某场景的内置默认提示词。
	 *
	 * @param key 场景 key
	 * @return 内置默认提示词；未知 key 返回空字符串
	 */
	public static String defaultPrompt(String key) {
		for (PromptMeta meta : CATALOG) {
			if (meta.getKey().equals(key)) {
				return meta.getDefaultPrompt();
			}
		}
		return "";
	}

	/**
	 * 解析某场景实际生效的系统提示词，优先级：配置中的分场景提示词 &gt; 历史通用提示词 &gt; 内置默认。
	 *
	 * @param aiSetting     系统 AI 配置（可为 null）
	 * @param key           场景 key
	 * @param legacyPrompt  历史通用提示词（仅 AI 问答 / 学习轨迹·每日叙述 传入，其余传 null）
	 * @return 生效的系统提示词
	 */
	public static String resolve(SystemInfo.AiSetting aiSetting, String key, String legacyPrompt) {
		if (aiSetting != null && aiSetting.getPrompts() != null) {
			String custom = aiSetting.getPrompts().get(key);
			if (custom != null && !custom.trim().isEmpty()) {
				return custom.trim();
			}
		}
		if (legacyPrompt != null && !legacyPrompt.trim().isEmpty()) {
			return legacyPrompt.trim();
		}
		return defaultPrompt(key);
	}

}
