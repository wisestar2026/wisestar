package cn.wisestar.server.impl;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 英语作答行为三态判定（掌握 / 不稳定 / 漏洞）。
 *
 * <p>依据《检测模型与题目处理策略》L4：不只看对错。各题型规则：</p>
 * <ul>
 * <li>连词成句：全对=掌握；词序错但词全用对=不稳定；漏词/多词/空白=漏洞</li>
 * <li>句子默写：全对=掌握；拼写错 1~2 处=不稳定；结构错/空白=漏洞</li>
 * <li>句子选择：首答对=掌握；改选后对=不稳定；选错=漏洞</li>
 * <li>挖空填空：首答对=掌握；提示后对/部分对=不稳定；答错/空白=漏洞</li>
 * <li>作答轮：完整正确=掌握；部分对/改选后对=不稳定；不对/空白=漏洞</li>
 * </ul>
 *
 * <p>得分与薄弱阈值对齐：掌握 100、不稳定 60、漏洞 0；薄弱阈值为 55，
 * 因此「不稳定」不进入薄弱清单，「漏洞」进入。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
final class EnglishBehaviorJudge {

	/** 掌握得分 */
	static final int SCORE_MASTERED = 100;

	/** 不稳定得分（需高于 {@link EvaluationServiceImpl#WEAK_THRESHOLD}） */
	static final int SCORE_UNSTABLE = 60;

	/** 漏洞得分 */
	static final int SCORE_FLAW = 0;

	private EnglishBehaviorJudge() {
	}

	/**
	 * 行为三态。
	 */
	enum Mastery {

		MASTERED("掌握", SCORE_MASTERED),

		UNSTABLE("不稳定", SCORE_UNSTABLE),

		FLAW("漏洞", SCORE_FLAW);

		private final String label;

		private final int score;

		Mastery(String label, int score) {
			this.label = label;
			this.score = score;
		}

		String getLabel() {
			return label;
		}

		int getScore() {
			return score;
		}

	}

	/**
	 * 选择题判定：首答对=掌握，改选后对=不稳定，选错/空白=漏洞。
	 */
	static Mastery judgeChoice(boolean correct, Integer changes) {
		if (!correct) {
			return Mastery.FLAW;
		}
		return changes != null && changes > 0 ? Mastery.UNSTABLE : Mastery.MASTERED;
	}

	/**
	 * 连词成句：词全用对但顺序错=不稳定；漏词/多词=漏洞。
	 */
	static Mastery judgeBuild(String expected, String actual) {
		if (!StringUtils.hasText(actual)) {
			return Mastery.FLAW;
		}
		List<String> exp = tokens(expected);
		List<String> act = tokens(actual);
		if (exp.isEmpty() || act.isEmpty()) {
			return Mastery.FLAW;
		}
		if (exp.equals(act)) {
			return Mastery.MASTERED;
		}
		return multiset(exp).equals(multiset(act)) ? Mastery.UNSTABLE : Mastery.FLAW;
	}

	/**
	 * 句子默写：拼写错 1~2 处=不稳定；词数不一致或错得更多=漏洞。
	 */
	static Mastery judgeDictation(String expected, String actual) {
		if (!StringUtils.hasText(actual)) {
			return Mastery.FLAW;
		}
		List<String> exp = tokens(expected);
		List<String> act = tokens(actual);
		if (exp.isEmpty() || act.isEmpty()) {
			return Mastery.FLAW;
		}
		if (exp.equals(act)) {
			return Mastery.MASTERED;
		}
		if (exp.size() != act.size()) {
			return Mastery.FLAW;
		}
		int diff = 0;
		for (int i = 0; i < exp.size(); i++) {
			if (!exp.get(i).equals(act.get(i))) {
				diff++;
			}
		}
		return diff > 0 && diff <= 2 ? Mastery.UNSTABLE : Mastery.FLAW;
	}

	/**
	 * 挖空填空：全对且未用提示=掌握；用提示后全对或部分对=不稳定；全错/空白=漏洞。
	 */
	static Mastery judgeBlanks(List<String> expected, String actualText, Boolean hintUsed) {
		if (expected == null || expected.isEmpty() || !StringUtils.hasText(actualText)) {
			return Mastery.FLAW;
		}
		String[] blanks = actualText.split("\\|", -1);
		int correct = 0;
		int compared = Math.min(blanks.length, expected.size());
		for (int i = 0; i < compared; i++) {
			if (normalize(blanks[i]).equals(normalize(expected.get(i)))) {
				correct++;
			}
		}
		if (correct == expected.size() && blanks.length == expected.size()) {
			return Boolean.TRUE.equals(hintUsed) ? Mastery.UNSTABLE : Mastery.MASTERED;
		}
		return correct > 0 ? Mastery.UNSTABLE : Mastery.FLAW;
	}

	/**
	 * 作答轮：完整正确=掌握；部分正确/改选后对=不稳定；全错/空白=漏洞。
	 */
	static Mastery judgeTurn(List<String> expected, String actualText, Integer changes) {
		if (expected == null || expected.isEmpty() || !StringUtils.hasText(actualText)) {
			return Mastery.FLAW;
		}
		String[] parts = actualText.split("\\|", -1);
		int correct = 0;
		for (int i = 0; i < Math.min(parts.length, expected.size()); i++) {
			if (normalize(parts[i]).equals(normalize(expected.get(i)))) {
				correct++;
			}
		}
		if (correct == expected.size() && parts.length == expected.size()) {
			return changes != null && changes > 0 ? Mastery.UNSTABLE : Mastery.MASTERED;
		}
		return correct > 0 ? Mastery.UNSTABLE : Mastery.FLAW;
	}

	/**
	 * 英文分词：小写、去标点、按空白切分。
	 */
	static List<String> tokens(String value) {
		if (!StringUtils.hasText(value)) {
			return new ArrayList<>();
		}
		String text = value.toLowerCase(Locale.ROOT)
				.replace('’', '\'')
				.replace('‘', '\'')
				.replaceAll("[\\p{Punct}]+", " ")
				.replaceAll("\\s+", " ")
				.trim();
		return text.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(text.split(" ")));
	}

	private static List<String> multiset(List<String> tokens) {
		List<String> sorted = new ArrayList<>(tokens);
		sorted.sort(String::compareTo);
		return sorted;
	}

	private static String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
				.replaceAll("^[\"'“”‘’]+|[\"'“”‘’]+$", "");
	}

}
