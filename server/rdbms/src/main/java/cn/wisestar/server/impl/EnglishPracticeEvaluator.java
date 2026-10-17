package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.EvaluationService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * 英语练习结果入账：把单词/句子/句式练习折算为英语模块知识点的掌握度与薄弱状态。
 *
 * <p>英语同步练习与情景练习不经过题库检测，历史上只更新熟悉度/学习日志，
 * 导致薄弱点检测缺少词汇、句子、语用三个模块的数据。这里按
 * {@code 单元·模块}（如 {@code Unit 6·单词}）的知识点口径统一入账，
 * 以章节 ID 收敛跨年级/册别的同名单元。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EnglishPracticeEvaluator {

	/** 句子同步练习模块名 */
	public static final String MODULE_SENTENCE = "句子";

	/** 单词情景对话模块名 */
	public static final String MODULE_WORD = "单词";

	/** 重点句式情景模块名 */
	public static final String MODULE_PATTERN = "句式";

	private final ChapterMapper chapterMapper;

	private final SubjectMapper subjectMapper;

	private final EvaluationService evaluationService;

	/**
	 * 记录单题结果。
	 *
	 * @param userId 学员 ID
	 * @param version 教材版本
	 * @param grade 年级
	 * @param term 册别
	 * @param unit 单元
	 * @param module 模块（句子/单词/句式）
	 * @param correct 是否答对
	 */
	public void record(String userId, String version, String grade, String term, String unit,
			String module, boolean correct) {
		recordBatch(userId, version, grade, term, unit, module, correct ? 1 : 0, 1);
	}

	/**
	 * 记录一批作答（用于情景练习整组提交，按正确数折算正确率）。
	 *
	 * @param userId 学员 ID
	 * @param version 教材版本
	 * @param grade 年级
	 * @param term 册别
	 * @param unit 单元
	 * @param module 模块（句子/单词/句式）
	 * @param correct 答对题数
	 * @param total 参与判定的题数
	 */
	public void recordBatch(String userId, String version, String grade, String term, String unit,
			String module, int correct, int total) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(unit) || !StringUtils.hasText(module)
				|| total <= 0) {
			return;
		}
		try {
			Chapter chapter = findChapter(version, grade, term, unit);
			if (chapter == null) {
				return;
			}
			String knowledgePoint = unit.trim() + "·" + module;
			PracticeEvaluationContext context = new PracticeEvaluationContext();
			context.setUserId(userId);
			context.setChapterId(chapter.getId());
			int hit = Math.max(0, Math.min(correct, total));
			for (int i = 0; i < total; i++) {
				PracticeEvaluationContext.Item item = new PracticeEvaluationContext.Item();
				item.setQuestionId("en-" + module + "-" + chapter.getId() + "-" + i);
				item.setCorrect(i < hit);
				item.setKnowledgePointNames(Collections.singletonList(knowledgePoint));
				context.getItems().add(item);
			}
			evaluationService.recordPractice(context);
		}
		catch (Exception e) {
			log.warn("english practice evaluation failed: user={}, unit={}, module={}", userId, unit, module, e);
		}
	}

	/**
	 * 记录一批带三态得分的作答（掌握 100 / 不稳定 60 / 漏洞 0）。
	 *
	 * <p>与 {@link #recordBatch} 的区别：得分参与掌握度加权，
	 * 使「不稳定」不落入薄弱阈值（55）以下、「漏洞」落入。</p>
	 */
	public void recordScores(String userId, String version, String grade, String term, String unit,
			String module, List<Integer> scores) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(unit) || !StringUtils.hasText(module)
				|| scores == null || scores.isEmpty()) {
			return;
		}
		try {
			Chapter chapter = findChapter(version, grade, term, unit);
			if (chapter == null) {
				return;
			}
			String knowledgePoint = unit.trim() + "·" + module;
			PracticeEvaluationContext context = new PracticeEvaluationContext();
			context.setUserId(userId);
			context.setChapterId(chapter.getId());
			for (int i = 0; i < scores.size(); i++) {
				int score = scores.get(i) == null ? 0 : Math.max(0, Math.min(scores.get(i), 100));
				PracticeEvaluationContext.Item item = new PracticeEvaluationContext.Item();
				item.setQuestionId("en-" + module + "-" + chapter.getId() + "-" + i);
				item.setScore(score);
				item.setCorrect(score >= 100);
				item.setKnowledgePointNames(Collections.singletonList(knowledgePoint));
				context.getItems().add(item);
			}
			evaluationService.recordPractice(context);
		}
		catch (Exception e) {
			log.warn("english practice evaluation failed: user={}, unit={}, module={}", userId, unit, module, e);
		}
	}

	/**
	 * 按版本+年级+册别+单元名定位英语章节。
	 */
	private Chapter findChapter(String version, String grade, String term, String unit) {
		String subjectId = englishSubjectId();
		if (subjectId == null) {
			return null;
		}
		return chapterMapper.selectOne(Wrappers.<Chapter>lambdaQuery()
				.eq(Chapter::getSubjectId, subjectId)
				.eq(StringUtils.hasText(version), Chapter::getVersion, version)
				.eq(StringUtils.hasText(grade), Chapter::getGrade, grade)
				.eq(StringUtils.hasText(term), Chapter::getTerm, term)
				.eq(Chapter::getName, unit.trim())
				.last("limit 1"));
	}

	/**
	 * 英语学科 ID。
	 */
	private String englishSubjectId() {
		Subject subject = subjectMapper.selectOne(Wrappers.<Subject>lambdaQuery()
				.eq(Subject::getCode, "ENGLISH")
				.last("limit 1"));
		return subject == null ? null : subject.getId();
	}

}
