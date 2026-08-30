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
					if (!correctBlanks[i].trim().equals(studentBlanks[i].trim())) {
						allMatch = false;
						break;
					}
				}
				if (allMatch) return 1;
			}
			// 兼容单空：直接比对
			if (correct != null && correct.trim().equals(student.trim())) {
				return 1;
			}
		}
		return 0;
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
