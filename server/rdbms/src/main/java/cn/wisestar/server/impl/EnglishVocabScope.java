package cn.wisestar.server.impl;

import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishUnit;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishUnitMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 英语题目词汇范围（本单元及之前所学）。
 *
 * <p>练习题的空必须是已学单词：范围 = 当前册排序不晚于当前单元的单词与重点句子 + 更早册别的全部单词与重点句子，
 * 再加上功能词（冠词、代词、be 动词、介词等）。判定时做简单词形还原（复数、第三人称单数、-ing / -ed），
 * 句中首字母大写的人名地名不计入超纲。</p>
 *
 * @author wisestar
 * @date 2026/10/24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishVocabScope {

	/** 年级顺序，用于判断「之前所学」涵盖哪些册别 */
	private static final List<String> GRADE_ORDER = Arrays.asList("一年级", "二年级", "三年级", "四年级", "五年级",
			"六年级");

	/** 册别顺序 */
	private static final List<String> TERM_ORDER = Arrays.asList("上册", "下册");

	/** 功能词：不要求出现在单元词表里 */
	private static final Set<String> FUNCTION_WORDS = new HashSet<>(Arrays.asList(
			"a", "an", "the", "i", "you", "he", "she", "it", "we", "they", "me", "him", "us", "them",
			"my", "your", "his", "her", "its", "our", "their", "myself", "yourself", "himself", "herself",
			"am", "is", "are", "was", "were", "be", "been", "being", "do", "does", "did", "done",
			"have", "has", "had", "can", "could", "will", "would", "shall", "should", "may", "might", "must",
			"to", "of", "in", "on", "at", "for", "with", "from", "by", "about", "after", "before", "into",
			"up", "down", "out", "off", "over", "under", "near", "behind", "between", "here", "there",
			"and", "or", "but", "so", "because", "if", "than", "as", "not", "no", "yes", "ok", "okay",
			"what", "how", "who", "whose", "where", "when", "why", "which", "this", "that", "these", "those",
			"some", "any", "many", "much", "more", "most", "all", "both", "each", "every", "other",
			"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
			"eleven", "twelve", "first", "second", "third", "very", "too", "also", "only", "just",
			"please", "thanks", "thank", "hello", "hi", "goodbye", "bye", "sorry", "let", "let's",
			"s", "t", "m", "re", "ve", "ll", "d"));

	private static final Pattern TOKEN = Pattern.compile("[A-Za-z]+(?:'[A-Za-z]+)?");

	/** 单元名里的序号（Unit 3 -> 3） */
	private static final Pattern UNIT_NUMBER = Pattern.compile("(\\d+)");

	private final EnglishUnitMapper unitMapper;

	private final EnglishWordMapper wordMapper;

	private final EnglishSentenceMapper sentenceMapper;

	/**
	 * 计算某单元的已学词汇范围。
	 *
	 * @return 允许出现的单词形式（含常见词形变化）；单元信息缺失时只按功能词判定
	 */
	public Set<String> allowed(String version, String grade, String term, String unit) {
		Set<String> allowed = new HashSet<>(FUNCTION_WORDS);
		for (String spell : learnedWords(version, grade, term, unit)) {
			allowed.addAll(variantsOf(spell));
		}
		for (EnglishSentence sentence : scopedSentences(version, grade, term, unit)) {
			for (String token : tokens(sentence.getEn())) {
				allowed.addAll(variantsOf(token));
			}
		}
		return allowed;
	}

	/**
	 * 本单元及之前学过、且出现在教材词表里的单词（按字母排序，供生成提示词使用）。
	 */
	public List<String> learnedWords(String version, String grade, String term, String unit) {
		Set<String> result = new LinkedHashSet<>();
		for (EnglishWord word : scopedWords(version, grade, term, unit)) {
			if (StringUtils.hasText(word.getSpell())) {
				result.add(word.getSpell().trim().toLowerCase(Locale.ROOT));
			}
		}
		List<String> list = new ArrayList<>(result);
		Collections.sort(list);
		return list;
	}

	/**
	 * 返回文本中超出已学范围的单词；句中大写的人名地名不计入。
	 */
	public List<String> outOfScope(String text, Set<String> allowed) {
		List<String> result = new ArrayList<>();
		if (!StringUtils.hasText(text) || allowed == null || allowed.isEmpty()) {
			return result;
		}
		Matcher matcher = TOKEN.matcher(text);
		while (matcher.find()) {
			String raw = matcher.group();
			String lower = raw.toLowerCase(Locale.ROOT);
			if (lower.length() < 2 || FUNCTION_WORDS.contains(lower)) {
				continue;
			}
			if (Character.isUpperCase(raw.charAt(0)) && !allowed.contains(lower)) {
				// 句中大写按人名 / 地名处理，不算超纲
				continue;
			}
			if (Collections.disjoint(variantsOf(lower), allowed)) {
				result.add(lower);
			}
		}
		return result;
	}

	/**
	 * 文本中的单词是否都在已学范围内。
	 */
	public boolean inScope(String text, Set<String> allowed) {
		return outOfScope(text, allowed).isEmpty();
	}

	/**
	 * 当前单元及排序更早的单元词条 + 更早册别的全部词条。
	 */
	private List<EnglishWord> scopedWords(String version, String grade, String term, String unit) {
		List<String> units = scopedUnits(version, grade, term, unit);
		if (units.isEmpty()) {
			return new ArrayList<>();
		}
		List<String> grades = scopedGrades(grade);
		List<EnglishWord> rows = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.in(EnglishWord::getGrade, grades));
		List<EnglishWord> result = new ArrayList<>();
		for (EnglishWord row : rows) {
			if (isEarlierBook(row.getGrade(), row.getTerm(), grade, term)
					|| (grade.equals(row.getGrade()) && term.equals(row.getTerm())
							&& units.contains(row.getUnit()))) {
				result.add(row);
			}
		}
		return result;
	}

	/**
	 * 当前单元及排序更早的单元句子 + 更早册别的全部句子。
	 */
	private List<EnglishSentence> scopedSentences(String version, String grade, String term, String unit) {
		List<String> units = scopedUnits(version, grade, term, unit);
		if (units.isEmpty()) {
			return new ArrayList<>();
		}
		List<String> grades = scopedGrades(grade);
		List<EnglishSentence> rows = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.in(EnglishSentence::getGrade, grades));
		List<EnglishSentence> result = new ArrayList<>();
		for (EnglishSentence row : rows) {
			if (isEarlierBook(row.getGrade(), row.getTerm(), grade, term)
					|| (grade.equals(row.getGrade()) && term.equals(row.getTerm())
							&& units.contains(row.getUnit()))) {
				result.add(row);
			}
		}
		return result;
	}

	/**
	 * 查询范围覆盖的年级：当前年级与更低年级；年级无法识别时只查当前年级。
	 */
	private List<String> scopedGrades(String grade) {
		int index = GRADE_ORDER.indexOf(grade);
		if (index < 0) {
			return Collections.singletonList(grade);
		}
		return new ArrayList<>(GRADE_ORDER.subList(0, index + 1));
	}

	/**
	 * 取当前单元及排序更早的单元。
	 */
	private List<String> scopedUnits(String version, String grade, String term, String unit) {
		Set<String> result = new LinkedHashSet<>();
		if (!StringUtils.hasText(unit)) {
			return new ArrayList<>(result);
		}
		List<String> units = orderedUnits(version, grade, term);
		int current = units.indexOf(unit);
		if (current < 0) {
			result.add(unit);
			return new ArrayList<>(result);
		}
		result.addAll(units.subList(0, current + 1));
		return new ArrayList<>(result);
	}

	/**
	 * 册别内的单元顺序：优先读 `t_english_unit.sort`；单元表为空时（内容由词表/句子推导）按
	 * 「Welcome 在前、Unit N 按数字升序」自然排序。
	 */
	private List<String> orderedUnits(String version, String grade, String term) {
		Map<String, Integer> sortByUnit = new LinkedHashMap<>();
		List<EnglishUnit> rows = unitMapper.selectList(Wrappers.<EnglishUnit>lambdaQuery()
				.eq(EnglishUnit::getVersion, version)
				.eq(EnglishUnit::getGrade, grade)
				.eq(EnglishUnit::getTerm, term));
		for (EnglishUnit row : rows) {
			if (!StringUtils.hasText(row.getUnit())) {
				continue;
			}
			String name = row.getUnit().trim();
			Integer sort = row.getSort() == null ? unitOrder(name) : row.getSort();
			sortByUnit.putIfAbsent(name, sort);
		}
		Set<String> names = new LinkedHashSet<>(sortByUnit.keySet());
		for (EnglishWord word : bookContentWords(version, grade, term)) {
			if (StringUtils.hasText(word.getUnit())) {
				names.add(word.getUnit().trim());
			}
		}
		for (EnglishSentence sentence : bookContentSentences(version, grade, term)) {
			if (StringUtils.hasText(sentence.getUnit())) {
				names.add(sentence.getUnit().trim());
			}
		}
		List<String> result = new ArrayList<>(names);
		Comparator<String> order = Comparator
				.comparingInt((String name) -> sortByUnit.getOrDefault(name, unitOrder(name)))
				.thenComparingInt(this::unitOrder)
				.thenComparing(Comparator.naturalOrder());
		Collections.sort(result, order);
		return result;
	}

	/**
	 * 单元自然顺序：无数字的（Welcome）排最前，其余按数字升序，同数字按名称。
	 */
	private int unitOrder(String unit) {
		Matcher matcher = UNIT_NUMBER.matcher(unit);
		if (!matcher.find()) {
			return -1;
		}
		return Integer.parseInt(matcher.group(1));
	}

	private List<EnglishWord> bookContentWords(String version, String grade, String term) {
		return wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term));
	}

	private List<EnglishSentence> bookContentSentences(String version, String grade, String term) {
		return sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term));
	}

	/**
	 * 词条 (grade, term) 是否早于当前册别。
	 */
	private boolean isEarlierBook(String rowGrade, String rowTerm, String grade, String term) {
		int row = bookOrder(rowGrade, rowTerm);
		int current = bookOrder(grade, term);
		return row >= 0 && current >= 0 && row < current;
	}

	/**
	 * 册别排序值：年级 * 10 + 册别，无法识别时返回 -1。
	 */
	private int bookOrder(String grade, String term) {
		int gradeIndex = GRADE_ORDER.indexOf(grade);
		int termIndex = TERM_ORDER.indexOf(term);
		if (gradeIndex < 0 || termIndex < 0) {
			return -1;
		}
		return gradeIndex * 10 + termIndex;
	}

	/**
	 * 把词条拆成单词（词表里存在 "sit down"、"a (an)"、"look at sb / sth" 这类写法）。
	 */
	private List<String> tokens(String text) {
		List<String> result = new ArrayList<>();
		if (!StringUtils.hasText(text)) {
			return result;
		}
		Matcher matcher = TOKEN.matcher(text);
		while (matcher.find()) {
			String token = matcher.group().toLowerCase(Locale.ROOT);
			if (token.length() < 2) {
				continue;
			}
			result.add(token);
			if (token.contains("'")) {
				for (String part : token.replace("'", " ").split("\\s+")) {
					if (part.length() >= 2) {
						result.add(part);
					}
				}
			}
		}
		return result;
	}

	/**
	 * 常见词形变化：复数 / 第三人称单数 / -ing / -ed / 比较级。
	 */
	private Set<String> variantsOf(String word) {
		Set<String> result = new HashSet<>();
		if (!StringUtils.hasText(word)) {
			return result;
		}
		for (String token : tokens(word)) {
			result.add(token);
			if (token.length() <= 3) {
				continue;
			}
			if (token.endsWith("ies")) {
				result.add(token.substring(0, token.length() - 3) + "y");
			} else if (token.endsWith("es")) {
				result.add(token.substring(0, token.length() - 2));
			} else if (token.endsWith("s")) {
				result.add(token.substring(0, token.length() - 1));
			}
			if (token.endsWith("ing")) {
				result.add(token.substring(0, token.length() - 3));
				result.add(token.substring(0, token.length() - 3) + "e");
			}
			if (token.endsWith("ed")) {
				result.add(token.substring(0, token.length() - 2));
				result.add(token.substring(0, token.length() - 2) + "e");
			}
			if (token.endsWith("er")) {
				result.add(token.substring(0, token.length() - 2));
			}
			if (token.endsWith("est")) {
				result.add(token.substring(0, token.length() - 3));
			}
		}
		return result;
	}

}
