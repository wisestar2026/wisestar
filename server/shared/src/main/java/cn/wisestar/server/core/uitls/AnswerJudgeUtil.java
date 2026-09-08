package cn.wisestar.server.core.uitls;

import cn.wisestar.server.domain.dto.SurveySchema;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 练习答题判分工具（纯静态，无状态）。
 *
 * <p><b>定位</b>：供练习落库（PracticeServiceImpl）对"前端提交的学生答案"复核判分，
 * 语义与前端 utils/practiceHelpers.js 一致：单选/判断按选项标题文本等值、
 * 多选按选项标题集合相等（与顺序无关）、填空/文本按输入内容等值。
 * 与 AnswerServiceImpl 的判分规则对齐，避免前后端判分不一致。</p>
 *
 * <p><b>答案格式</b>：学生答案由前端提交，结构为
 * <code>{type: 'option', optionId} / {type: 'options', optionIds: []} / {type: 'text', text}</code>；
 * 判分前先按题目选项映射为"选项标题文本"再比较。</p>
 *
 * <p><b>返回值约定</b>：1 正确 / 0 错误（含未作答）/ null 无标准答案（不计分）。</p>
 *
 * @author zhanghaiyang
 */
public final class AnswerJudgeUtil {

	private AnswerJudgeUtil() {
	}

	/**
	 * 提取题目标准答案列表；无任何标准答案时返回 null。
	 *
	 * <p><b>提取优先级</b>：</p>
	 * 1. 整题级答案：题目 attribute.examCorrectAnswer（多选多个答案以 \n 分隔）；
	 * 2. 选项级答案：遍历子选项，收集 attribute.examCorrectAnswer 非空的选项标题。
	 *
	 * @param question 题目 schema（含 attribute/children）
	 * @return 标准答案字符串列表；无答案返回 null
	 */
	public static List<String> extractCorrectAnswers(SurveySchema question) {
		if (question.getAttribute() != null && StringUtils.hasText(question.getAttribute().getExamCorrectAnswer())) {
			return Arrays.asList(question.getAttribute().getExamCorrectAnswer().split("\n"));
		}
		List<String> correct = new ArrayList<>();
		if (question.getChildren() != null) {
			for (SurveySchema child : question.getChildren()) {
				if (child.getAttribute() != null
						&& StringUtils.hasText(child.getAttribute().getExamCorrectAnswer())) {
					correct.add(child.getTitle());
				}
			}
		}
		return correct.isEmpty() ? null : correct;
	}

	/**
	 * 判定题目对错。
	 *
	 * @param question     题目 schema
	 * @param studentAnswer 前端提交的学生答案 Map（type/optionId/optionIds/text）
	 * @return 1 正确 / 0 错误 / null 无标准答案
	 */
	public static Integer evaluate(SurveySchema question, Map<String, Object> studentAnswer) {
		List<String> correctAnswers = extractCorrectAnswers(question);
		if (correctAnswers == null) {
			return null;
		}
		String student = formatAnswer(question, studentAnswer);
		if (student == null || student.trim().isEmpty()) {
			return 0;
		}
		// 标准答案归一化：支持 选项文本 / 选项字母(A/B/C…) / 选项序号(1/2/3…) → 选项标题
		List<SurveySchema> children = question.getChildren() == null ? java.util.Collections.emptyList()
				: question.getChildren();
		List<String> normalized = correctAnswers.stream().map(ans -> {
			String a = ans == null ? "" : ans.trim();
			for (SurveySchema c : children) {
				if (StringUtils.hasText(c.getTitle()) && c.getTitle().trim().equals(a)) {
					return c.getTitle().trim();
				}
			}
			String upper = a.toUpperCase();
			int li = "ABCDEFGHIJ".indexOf(upper);
			if (li >= 0 && li < children.size() && StringUtils.hasText(children.get(li).getTitle())) {
				return children.get(li).getTitle().trim();
			}
			try {
				int ni = Integer.parseInt(a) - 1;
				if (ni >= 0 && ni < children.size() && StringUtils.hasText(children.get(ni).getTitle())) {
					return children.get(ni).getTitle().trim();
				}
			}
			catch (NumberFormatException ignored) {
				// 非序号
			}
			return a;
		}).collect(Collectors.toList());
		if (SurveySchema.QuestionType.Checkbox.equals(question.getType())) {
			Set<String> studentSet = splitAnswerSet(student);
			Set<String> correctSet = new HashSet<>();
			for (String correct : normalized) {
				correctSet.addAll(splitAnswerSet(correct));
			}
			return studentSet.equals(correctSet) ? 1 : 0;
		}
		// 填空/简答：支持多空按顺序比对（正确答案以 | 分隔多空）
		for (String correct : normalized) {
			String[] correctBlanks = correct.split("\\|");
			String[] studentBlanks = student.split("\\|");
			if (correctBlanks.length == studentBlanks.length) {
				boolean allMatch = true;
				for (int i = 0; i < correctBlanks.length; i++) {
					if (!blankEquals(correctBlanks[i], studentBlanks[i])) {
						allMatch = false;
						break;
					}
				}
				if (allMatch) return 1;
			}
			// 兼容单空：归一化后直接比对
			if (correct != null && blankEquals(correct, student)) {
				return 1;
			}
		}
		return 0;
	}

	/**
	 * 填空类题目（单项填空/多项填空）逐空判分。
	 *
	 * <p><b>语义</b>：多项填空允许多个空位部分正确，返回每个空位的命中标记（1=对 / 0=错），
	 * 供练习计分按"空位分值"累加（全对时与 evaluate=1 等价）。</p>
	 *
	 * <p><b>空位定义</b>：题目标准答案 examCorrectAnswer 以 {@code |} 分隔各空答案；
	 * 单项填空无分隔符视为单空。学生答案同样按 {@code |} 拆空。</p>
	 *
	 * @param question      题目 schema
	 * @param studentAnswer 前端提交的学生答案 Map
	 * @return int[]：下标 0 = 空位总数（0 表示非填空题型 / 无标准答案 / 不可计分），
	 *         下标 1..n = 各空位命中标记（1 正确 / 0 错误）；未作答时全部为 0 但总数保留
	 */
	public static int[] evaluateBlanks(SurveySchema question, Map<String, Object> studentAnswer) {
		if (question == null || question.getAttribute() == null) {
			return new int[] { 0 };
		}
		SurveySchema.QuestionType type = question.getType();
		if (type != SurveySchema.QuestionType.FillBlank
				&& type != SurveySchema.QuestionType.MultipleBlank) {
			return new int[] { 0 };
		}
		List<String> correctAnswers = extractCorrectAnswers(question);
		int total = correctAnswers == null ? 0 : countBlanks(correctAnswers);
		if (total == 0) {
			return new int[] { 0 };
		}
		int[] result = new int[total + 1];
		result[0] = total;
		String student = formatAnswer(question, studentAnswer);
		if (student == null || student.trim().isEmpty()) {
			return result; // 未作答：全部空位标记 0
		}
		String[] studentBlanks = student.split("\\|");
		if (studentBlanks.length != total) {
			// 空位数不一致：尝试整体等值兜底（兼容单空直答）
			for (String correct : correctAnswers) {
				if (correct != null && blankEquals(correct, student)) {
					Arrays.fill(result, 1, result.length, 1);
					return result;
				}
			}
			return result; // 空位数不一致且整体不等值：不得分
		}
		for (String correct : correctAnswers) {
			if (correct == null) {
				continue;
			}
			String[] correctBlanks = correct.split("\\|");
			if (correctBlanks.length != total) {
				continue;
			}
			for (int i = 0; i < total; i++) {
				if (blankEquals(correctBlanks[i], studentBlanks[i])) {
					result[i + 1] = 1;
				}
			}
			return result;
		}
		return result;
	}

	/**
	 * 统计标准答案中的最大空位数（{@code |} 分隔的段数；无分隔为 1）。
	 */
	private static int countBlanks(List<String> correctAnswers) {
		int max = 0;
		for (String correct : correctAnswers) {
			if (correct == null) {
				continue;
			}
			int n = correct.split("\\|", -1).length;
			if (n > max) {
				max = n;
			}
		}
		return max;
	}

	/**
	 * 把前端答案 Map 格式化为可比较/可展示的文本。
	 *
	 * <p><b>映射规则</b>：</p>
	 * - type=option：optionId 对应子选项的 title（单选/判断）；
	 * - type=options：多个 optionIds 对应 titles 逗号拼接（多选）；
	 * - type=text：直接取 text（填空/文本）；
	 * - 其他/无效：null。
	 *
	 * @param question 题目 schema（children 提供选项 id→title 映射）
	 * @param answer   前端答案 Map
	 * @return 选项标题文本；无效返回 null
	 */
	public static String formatAnswer(SurveySchema question, Map<String, Object> answer) {
		if (answer == null) {
			return null;
		}
		Object type = answer.get("type");
		if ("option".equals(type)) {
			String optionId = String.valueOf(answer.get("optionId"));
			if (question.getChildren() != null) {
				for (SurveySchema child : question.getChildren()) {
					if (optionId.equals(child.getId())) {
						return child.getTitle();
					}
				}
			}
			return null;
		}
		if ("options".equals(type)) {
			Object raw = answer.get("optionIds");
			if (raw == null) {
				return null;
			}
			@SuppressWarnings("unchecked")
			List<String> optionIds = (List<String>) raw;
			if (optionIds.isEmpty()) {
				return null;
			}
			List<String> titles = new ArrayList<>();
			for (String optionId : optionIds) {
				String title = findOptionTitle(question, optionId);
				if (title != null) {
					titles.add(title);
				}
			}
			return titles.isEmpty() ? null : String.join(",", titles);
		}
		if ("text".equals(type)) {
			Object text = answer.get("text");
			return text == null ? null : String.valueOf(text);
		}
		return null;
	}

	/**
	 * 按选项 id 查找选项标题；未命中返回 null。
	 */
	private static String findOptionTitle(SurveySchema question, String optionId) {
		if (question.getChildren() == null) {
			return null;
		}
		for (SurveySchema child : question.getChildren()) {
			if (optionId.equals(child.getId())) {
				return child.getTitle();
			}
		}
		return null;
	}

	/**
	 * 填空/文本答案归一化（比较前对两侧统一处理，避免格式差异误判）:
	 * 全角 ASCII 字母/数字/符号（含 ＜＞＝ 等）转半角、各类全角/不间断空白转普通空格、
	 * 移除零宽字符、连续空白折叠、去首尾。
	 */
	private static String normalizeBlank(String s) {
		if (s == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c >= 0xFF01 && c <= 0xFF5E) {
				sb.append((char) (c - 0xFEE0));
			}
			else if (c == 0x3000 || c == 0x00A0 || c == 0x1680 || c == 0x202F || c == 0x205F
					|| (c >= 0x2000 && c <= 0x200A)) {
				sb.append(' ');
			}
			else if (c == 0x200B || c == 0x200C || c == 0x200D || c == 0xFEFF) {
				// 零宽字符直接移除
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString().replaceAll("\\s+", " ").trim();
	}

	/**
	 * 填空空位等值比较（两边先归一化；纯 ASCII 字母串如选项字母 A/B/C 忽略大小写）。
	 */
	private static boolean blankEquals(String a, String b) {
		String na = normalizeBlank(a);
		String nb = normalizeBlank(b);
		if (na.equals(nb)) {
			return true;
		}
		if (na.matches("[A-Za-z]+") && nb.matches("[A-Za-z]+")) {
			return na.equalsIgnoreCase(nb);
		}
		return false;
	}

	/**
	 * 将答案字符串按逗号/换行拆分为去重集合（用于多选题无序比较）。
	 *
	 * @param answer 答案字符串（如 "A,B" 或 "A\nB"）
	 * @return 去重后的选项集合；null/空串返回空集
	 */
	private static Set<String> splitAnswerSet(String answer) {
		if (answer == null || answer.isEmpty()) {
			return new HashSet<>();
		}
		return Arrays.stream(answer.split("[,\n]")).map(String::trim)
				.filter(x -> !x.isEmpty()).collect(Collectors.toSet());
	}
}
