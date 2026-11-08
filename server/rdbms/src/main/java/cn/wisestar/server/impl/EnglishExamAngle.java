package cn.wisestar.server.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语考察角度规则（纯函数，无数据库依赖）。
 *
 * <p>「考察角度」回答的是「同一个知识点换哪种考法」，用于复习时避免把原题重刷一遍。
 * 三个模块各有自己的角度池；角度耗尽后允许复用，但必须先把未使用角度全部走完。</p>
 *
 * <p>判定「换汤不换药」的口径：题型与角度都相同、只有名词或人名不同，视为同一角度。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
public final class EnglishExamAngle {

	private EnglishExamAngle() {
	}

	/** 模块：单词 */
	public static final String MODULE_WORD = "word";

	/** 模块：句子 */
	public static final String MODULE_SENTENCE = "sentence";

	/** 模块：语法 */
	public static final String MODULE_GRAMMAR = "grammar";

	/** 单词角度：词 → 义 */
	public static final String WORD_MEANING = "word-meaning";

	/** 单词角度：义 → 词 */
	public static final String MEANING_WORD = "meaning-word";

	/** 单词角度：看图识词 */
	public static final String IMAGE_WORD = "image-word";

	/** 单词角度：固定搭配填空 */
	public static final String WORD_COLLOCATION = "word-collocation";

	/** 单词角度：听音选词 */
	public static final String WORD_LISTEN = "word-listen";

	/** 单词角度：默写 */
	public static final String WORD_DICTATION = "word-dictation";

	/** 句式角度：听音组句 */
	public static final String SENTENCE_LISTEN_BUILD = "sentence-listen-build";

	/** 句式角度：连词成句 */
	public static final String SENTENCE_BUILD = "sentence-build";

	/** 句式角度：词汇填空 */
	public static final String SENTENCE_FILL = "sentence-fill";

	/** 句式角度：中 → 英选择 */
	public static final String SENTENCE_ZH_EN = "sentence-zh-en";

	/** 句式角度：英 → 中选择 */
	public static final String SENTENCE_EN_ZH = "sentence-en-zh";

	/** 句式角度：听音选义 */
	public static final String SENTENCE_LISTEN_MEANING = "sentence-listen-meaning";

	/** 句式角度：句子默写 */
	public static final String SENTENCE_DICTATION = "sentence-dictation";

	/** 语法角度：规则识别 */
	public static final String GRAMMAR_RULE_ID = "grammar-rule-id";

	/** 语法角度：语法单选 */
	public static final String GRAMMAR_CHOICE = "grammar-choice";

	/** 语法角度：语法判断 */
	public static final String GRAMMAR_JUDGE = "grammar-judge";

	/** 语法角度：变形填空 */
	public static final String GRAMMAR_TRANSFORM = "grammar-transform";

	/** 语法角度：改写 */
	public static final String GRAMMAR_REWRITE = "grammar-rewrite";

	private static final Map<String, List<String>> POOLS;
	private static final Map<String, String> LABELS;

	static {
		Map<String, List<String>> pools = new LinkedHashMap<>();
		pools.put(MODULE_WORD, Collections.unmodifiableList(Arrays.asList(
				WORD_MEANING, MEANING_WORD, IMAGE_WORD, WORD_COLLOCATION, WORD_LISTEN, WORD_DICTATION)));
		pools.put(MODULE_SENTENCE, Collections.unmodifiableList(Arrays.asList(
				SENTENCE_LISTEN_BUILD, SENTENCE_BUILD, SENTENCE_FILL, SENTENCE_ZH_EN,
				SENTENCE_EN_ZH, SENTENCE_LISTEN_MEANING, SENTENCE_DICTATION)));
		pools.put(MODULE_GRAMMAR, Collections.unmodifiableList(Arrays.asList(
				GRAMMAR_RULE_ID, GRAMMAR_CHOICE, GRAMMAR_JUDGE, GRAMMAR_TRANSFORM, GRAMMAR_REWRITE)));
		POOLS = Collections.unmodifiableMap(pools);

		Map<String, String> labels = new LinkedHashMap<>();
		labels.put(WORD_MEANING, "看词选义");
		labels.put(MEANING_WORD, "看义选词");
		labels.put(IMAGE_WORD, "看图识词");
		labels.put(WORD_COLLOCATION, "搭配填空");
		labels.put(WORD_LISTEN, "听音选词");
		labels.put(WORD_DICTATION, "单词默写");
		labels.put(SENTENCE_LISTEN_BUILD, "听音组句");
		labels.put(SENTENCE_BUILD, "连词成句");
		labels.put(SENTENCE_FILL, "句式填空");
		labels.put(SENTENCE_ZH_EN, "看中文选英文");
		labels.put(SENTENCE_EN_ZH, "看英文选中文");
		labels.put(SENTENCE_LISTEN_MEANING, "听英文选中文");
		labels.put(SENTENCE_DICTATION, "句子默写");
		labels.put(GRAMMAR_RULE_ID, "规则识别");
		labels.put(GRAMMAR_CHOICE, "语法单选");
		labels.put(GRAMMAR_JUDGE, "语法判断");
		labels.put(GRAMMAR_TRANSFORM, "变形填空");
		labels.put(GRAMMAR_REWRITE, "语法改写");
		LABELS = Collections.unmodifiableMap(labels);
	}

	/**
	 * 某模块允许的角度池（按默认顺序）。
	 *
	 * @param module word / sentence / grammar
	 * @return 角度列表，未知模块返回空列表
	 */
	public static List<String> anglesOf(String module) {
		List<String> pool = POOLS.get(module);
		return pool == null ? Collections.emptyList() : pool;
	}

	/**
	 * 角度中文标签。
	 *
	 * @param angle 角度编码
	 * @return 标签，未知角度原样返回
	 */
	public static String labelOf(String angle) {
		return LABELS.getOrDefault(angle, angle);
	}

	/**
	 * 是否输出类题型（默写、填空、组句、改写）。输出类错误在薄弱判定里权重更高。
	 *
	 * @param angle 角度编码
	 * @return true 表示输出类
	 */
	public static boolean isOutputAngle(String angle) {
		return WORD_COLLOCATION.equals(angle)
				|| WORD_DICTATION.equals(angle)
				|| SENTENCE_LISTEN_BUILD.equals(angle)
				|| SENTENCE_BUILD.equals(angle)
				|| SENTENCE_FILL.equals(angle)
				|| SENTENCE_DICTATION.equals(angle)
				|| GRAMMAR_TRANSFORM.equals(angle)
				|| GRAMMAR_REWRITE.equals(angle);
	}

	/**
	 * 在未使用角度中挑选下一个；全部用过时按使用次数最少的优先轮换。
	 *
	 * @param module 模块
	 * @param usedAngles 已使用角度（可空，可含未知角度，自动忽略）
	 * @return 推荐角度；模块未知时返回 null
	 */
	public static String nextAngle(String module, List<String> usedAngles) {
		List<String> pool = anglesOf(module);
		if (pool.isEmpty()) {
			return null;
		}
		List<String> used = usedAngles == null ? Collections.emptyList() : usedAngles;
		for (String angle : pool) {
			if (!used.contains(angle)) {
				return angle;
			}
		}
		// 角度耗尽：按池内顺序回落到第一个，调用方负责换语境而不是重复同一道题
		return pool.get(0);
	}

	/**
	 * 判断两个角度是否相同（用于防「换汤不换药」）。
	 *
	 * @param a 角度 A
	 * @param b 角度 B
	 * @return true 表示同一角度
	 */
	public static boolean isSameAngle(String a, String b) {
		return a != null && a.equals(b);
	}

	/**
	 * 已使用角度中去掉空值与未知编码。
	 *
	 * @param module 模块
	 * @param usedAngles 原始使用记录
	 * @return 过滤后的角度列表
	 */
	public static List<String> normalizeUsed(String module, List<String> usedAngles) {
		List<String> pool = anglesOf(module);
		List<String> result = new ArrayList<>();
		if (usedAngles == null) {
			return result;
		}
		for (String angle : usedAngles) {
			if (angle != null && pool.contains(angle) && !result.contains(angle)) {
				result.add(angle);
			}
		}
		return result;
	}
}
