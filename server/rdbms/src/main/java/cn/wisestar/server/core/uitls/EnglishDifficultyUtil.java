package cn.wisestar.server.core.uitls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 英语题目难度评分器。
 *
 * <p>题目模板、句子内容包、融合题三处共用同一套评分口径，避免同一道题在不同入口
 * 得到不同难度。评分由五部分构成：</p>
 * <ul>
 *   <li>题型基础分：识别类最低，主动产出类更高；</li>
 *   <li>英文长度：按年级分档，超出常用句长后逐词加分；</li>
 *   <li>结构复杂度：从句、转折因果、情态/将来、被动、比较级、多句；</li>
 *   <li>作答负担：答案词数、空位数；</li>
 *   <li>选项干扰：选项数量与近义干扰项。</li>
 * </ul>
 *
 * <p>得分映射为 5 个档位（D1~D5），对外仍按三档输出：D1 简单、D2~D3 中等、
 * D4~D5 困难。句子内容包的 1~3 与人机界面的 easy/medium/hard 均由该档位换算。</p>
 *
 * @author wisestar
 * @date 2026/10/21
 */
public final class EnglishDifficultyUtil {

	private EnglishDifficultyUtil() {
	}

	/**
	 * 作答方式，决定基础分与部分作答负担。
	 */
	public enum Demand {

		/** 识别类：选择、判断，只需认出正确答案 */
		RECOGNITION(8),

		/** 有提示的产出类：看中文或词库补全、挖空回填 */
		CUED(26),

		/** 应用类：语法变形、人称改写等需要主动生成词形 */
		APPLICATION(28),

		/** 完整产出类：连词成句、默写、整句作答 */
		FULL(36);

		private final int baseScore;

		Demand(int baseScore) {
			this.baseScore = baseScore;
		}

		public int getBaseScore() {
			return baseScore;
		}
	}

	/**
	 * 评分结果。
	 */
	public static final class Result {

		private final int score;

		private final int rank;

		private final int level;

		private Result(int score) {
			this.score = score;
			this.rank = rankOf(score);
			this.level = levelOf(this.rank);
		}

		public int getScore() {
			return score;
		}

		/** 细分档位 1~5 */
		public int getRank() {
			return rank;
		}

		/** 三档位 1~3：1 简单、2 中等、3 困难 */
		public int getLevel() {
			return level;
		}

		/** D1~D3，供只接受三档标签的场景使用 */
		public String getDLevel() {
			return "D" + level;
		}

		/** easy / medium / hard */
		public String getLabel() {
			return level >= 3 ? "hard" : level == 2 ? "medium" : "easy";
		}
	}

	private static final Pattern WORD = Pattern.compile("[A-Za-z0-9]+(?:[.'’-][A-Za-z0-9]+)*");

	private static final Pattern SUBORDINATE = Pattern.compile(
			"\\b(because|although|though|if|when|while|before|after|until|since|who|which|whose|where|unless|whether)\\b");

	private static final Pattern CONTRAST = Pattern.compile("\\b(but|however|so|therefore|instead|otherwise)\\b");

	private static final Pattern BASIC_MODAL = Pattern.compile("\\b(can|could)\\b");

	private static final Pattern ADVANCED_MODAL = Pattern.compile(
			"\\b(will|would|shall|should|must|may|might)\\b|\\bhave to\\b|\\bhas to\\b|\\bhad to\\b");

	private static final Pattern PASSIVE = Pattern.compile("\\b(is|are|was|were|be|been|being)\\s+[a-z]{3,}(ed|en)\\b");

	private static final Pattern COMPARISON = Pattern.compile("\\b(more|most)\\b|\\b[a-z]{3,}(er|est)\\s+than\\b");

	private static final Pattern TENSE = Pattern.compile(
			"\\b(was|were|went|bought|saw|had|did|made|took|gave|came|ate|drank|wrote|rode|flew|swam|ran|sang|taught|thought|caught)\\b");

	private static final List<String> EMPTY = Collections.emptyList();

	/**
	 * 计算难度。
	 *
	 * @param title 题干（中文会先剔除，只保留英文参与分析）
	 * @param answer 正确答案；识别类可传选项 ID，仅作补充信息
	 * @param grade 年级，如 三年级 / 五年级；为空按高年级阈值
	 * @param demand 作答方式
	 * @param blankCount 空位数，识别类传 0
	 * @param options 选项文本，可为空
	 * @param correctOptionText 正确选项文本，识别类用于补足英文内容，可为空
	 */
	public static Result evaluate(String title, String answer, String grade, Demand demand, int blankCount,
			List<String> options, String correctOptionText) {
		String normalized = normalize(title);
		String analysis = normalized;
		if (demand != Demand.RECOGNITION && !isBlank(answer) && !sameText(answer, normalized)) {
			analysis = analysis + " " + normalize(answer);
		}
		if (demand == Demand.RECOGNITION && !isBlank(correctOptionText) && !sameText(correctOptionText, normalized)) {
			analysis = analysis + " " + normalize(correctOptionText);
		}

		int score = demand.getBaseScore();
		int words = words(analysis).size();
		boolean lowerGrade = isLowerGrade(grade);
		int sentences = countSentences(analysis);
		int shortMax = lowerGrade ? 6 : 8;

		int lengthScore = 0;
		if (words > shortMax) {
			lengthScore = 4 + (words - shortMax) * 3;
		}
		score += Math.min(24, lengthScore);
		if (words >= 25) {
			// 超长文本即使结构简单，阅读负担也明显高于常用句。
			score += 4;
		}
		if (words >= 30) {
			score += 2;
		}

		int complexity = 0;
		if (SUBORDINATE.matcher(analysis).find()) {
			complexity += 5;
		}
		if (CONTRAST.matcher(analysis).find()) {
			complexity += 3;
		}
		if (BASIC_MODAL.matcher(analysis).find()) {
			complexity += 2;
		}
		if (ADVANCED_MODAL.matcher(analysis).find()) {
			complexity += 4;
		}
		if (PASSIVE.matcher(analysis).find()) {
			complexity += 3;
		}
		if (COMPARISON.matcher(analysis).find()) {
			complexity += 3;
		}
		if (TENSE.matcher(analysis).find()) {
			complexity += 3;
		}
		if (sentences > 1) {
			complexity += Math.min(6, (sentences - 1) * 3);
		}
		if (lowerGrade && (SUBORDINATE.matcher(analysis).find() || PASSIVE.matcher(analysis).find()
				|| COMPARISON.matcher(analysis).find())) {
			complexity += 3;
		}
		score += Math.min(18, complexity);

		score += longWordScore(analysis);

		if (demand == Demand.CUED || demand == Demand.APPLICATION) {
			int answerWords = words(answer).size();
			score += Math.min(8, Math.max(0, answerWords - 1) * 2);
			if (blankCount > 1) {
				score += Math.min(6, (blankCount - 1) * 2);
			}
		}

		if (options != null && !options.isEmpty()) {
			score += options.size() >= 4 ? 2 : 1;
			if (demand == Demand.RECOGNITION && hasNearMiss(options, correctOptionText)) {
				score += 6;
			}
		}

		return new Result(Math.min(100, Math.max(0, score)));
	}

	private static int rankOf(int score) {
		if (score < 26) {
			return 1;
		}
		if (score < 42) {
			return 2;
		}
		if (score < 54) {
			return 3;
		}
		if (score < 74) {
			return 4;
		}
		return 5;
	}

	private static int levelOf(int rank) {
		return rank <= 1 ? 1 : rank <= 3 ? 2 : 3;
	}

	/**
	 * 一~四年级按更短的句子判断；其余年级按高年级阈值。
	 */
	private static boolean isLowerGrade(String grade) {
		if (isBlank(grade)) {
			return false;
		}
		String value = grade.replace(" ", "");
		return value.contains("一年级") || value.contains("二年级") || value.contains("三年级")
				|| value.contains("四年级") || value.contains("1年级") || value.contains("2年级")
				|| value.contains("3年级") || value.contains("4年级");
	}

	private static int countSentences(String text) {
		int count = 0;
		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (c == '.' || c == '?' || c == '!') {
				count++;
			}
		}
		return count;
	}

	private static int longWordScore(String text) {
		int score = 0;
		for (String token : words(text)) {
			String letters = token.replaceAll("[^A-Za-z]", "");
			if (letters.length() >= 10) {
				score++;
			}
			if (score >= 2) {
				break;
			}
		}
		return score;
	}

	/**
	 * 近义干扰：词数相同且仅一个词不同，或前两个词相同而长度相差一个词。
	 */
	private static boolean hasNearMiss(List<String> options, String correct) {
		if (isBlank(correct)) {
			return false;
		}
		List<String> expected = words(correct);
		for (String option : options) {
			if (isBlank(option) || sameText(option, correct)) {
				continue;
			}
			List<String> actual = words(option);
			if (expected.size() == actual.size()) {
				int diff = 0;
				for (int i = 0; i < expected.size(); i++) {
					if (!expected.get(i).equalsIgnoreCase(actual.get(i))) {
						diff++;
					}
				}
				if (diff == 1) {
					return true;
				}
			}
			if (expected.size() >= 3 && actual.size() >= 2
					&& expected.get(0).equalsIgnoreCase(actual.get(0))
					&& Math.abs(expected.size() - actual.size()) == 1) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 去掉中文与全角字符，保留英文、数字和标点，避免中文译文影响长度与复杂度判断。
	 */
	private static String normalize(String text) {
		if (text == null) {
			return "";
		}
		String cleaned = text.replaceAll("（[^）]*）", " ")
				.replaceAll("\\([^)]*[\\u4e00-\\u9fff][^)]*\\)", " ");
		return cleaned.replaceAll("[\\u4e00-\\u9fff\\u3000-\\u303f\\uff00-\\uffef]", " ")
				.replace('’', '\'');
	}

	/**
	 * 统计题干中的空位数：每连续 3 个下划线计 1 空，题干应统一使用 3 个下划线。
	 */
	public static int countBlanks(String text) {
		if (isBlank(text)) {
			return 0;
		}
		int count = 0;
		int index = text.indexOf("___");
		while (index >= 0) {
			count++;
			index = text.indexOf("___", index + 3);
		}
		return count;
	}

	/**
	 * 提取英文/数字词，中文与标点不计入。
	 */
	public static List<String> words(String text) {
		if (isBlank(text)) {
			return EMPTY;
		}
		List<String> result = new ArrayList<>();
		Matcher matcher = WORD.matcher(text);
		while (matcher.find()) {
			result.add(matcher.group());
		}
		return result;
	}

	private static boolean sameText(String left, String right) {
		return normalize(left).trim().equalsIgnoreCase(normalize(right).trim());
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
