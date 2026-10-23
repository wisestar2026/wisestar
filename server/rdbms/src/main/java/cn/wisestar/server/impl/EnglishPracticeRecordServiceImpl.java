package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeHistoryView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeItemView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeRecordRequest;
import cn.wisestar.server.domain.dto.english.EnglishPracticeRecordView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeSummaryView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.model.EnglishPracticeRecord;
import cn.wisestar.server.domain.model.EnglishWrongQuestion;
import cn.wisestar.server.mapper.EnglishPracticeRecordMapper;
import cn.wisestar.server.mapper.EnglishWrongQuestionMapper;
import cn.wisestar.server.service.EnglishPracticeRecordService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 英语练习作答记录与错题本实现。
 *
 * <p>只保存「练完之后的结果」：本次得分、逐题快照、错题。诊断报告仍由单元练习服务生成，
 * 本服务不计算掌握度，避免与学习诊断口径重复。</p>
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishPracticeRecordServiceImpl implements EnglishPracticeRecordService {

	private final EnglishPracticeRecordMapper recordMapper;
	private final EnglishWrongQuestionMapper wrongQuestionMapper;
	private final ObjectMapper objectMapper;

	/** 历史成绩最多回看条数（最高分取该范围内的最大值） */
	private static final int HISTORY_LIMIT = 100;

	/** 「查看过往记录」默认返回条数 */
	private static final int DEFAULT_HISTORY_PAGE = 20;

	@Override
	public EnglishPracticeSummaryView record(String userId, EnglishPracticeRecordRequest request) {
		if (!StringUtils.hasText(userId)) {
			throw new ValidationException("未登录");
		}
		if (request == null) {
			throw new ValidationException("交卷内容不能为空");
		}
		String module = normalizeModule(request.getModule());
		List<EnglishPracticeItemView> items = request.getItems() == null
				? Collections.<EnglishPracticeItemView>emptyList() : request.getItems();
		if (items.isEmpty()) {
			throw new ValidationException("本次练习没有作答记录");
		}
		int total = items.size();
		int correct = 0;
		for (EnglishPracticeItemView item : items) {
			if (Integer.valueOf(1).equals(item.getCorrect())) {
				correct++;
			}
		}
		int score = (int) Math.round(correct * 100.0 / total);
		Date now = new Date();

		EnglishPracticeRecord record = new EnglishPracticeRecord();
		record.setUserId(userId);
		record.setModule(module);
		record.setVersion(request.getVersion());
		record.setGrade(request.getGrade());
		record.setTerm(request.getTerm());
		record.setUnit(request.getUnit());
		record.setScore(score);
		record.setCorrectCount(correct);
		record.setTotalCount(total);
		record.setAccuracy(score);
		record.setDurationSeconds(request.getDurationSeconds() == null ? 0 : request.getDurationSeconds());
		record.setDetails(writeJson(items));
		recordMapper.insert(record);

		saveWrongQuestions(userId, module, request, items, now);

		return buildSummary(userId, module, request.getUnit(), record.getId(), score);
	}

	@Override
	public EnglishPracticeSummaryView recordFromReport(String userId, String module, String version, String grade,
			String term, String unit, DetectReportView report) {
		if (report == null || report.getDetails() == null || report.getDetails().isEmpty()) {
			return null;
		}
		List<EnglishPracticeItemView> items = new ArrayList<>();
		for (DetectReportView.Detail detail : report.getDetails()) {
			EnglishPracticeItemView item = new EnglishPracticeItemView();
			item.setQuestionId(detail.getQuestionId());
			item.setQuestionType(detail.getQuestionType());
			item.setChapter(detail.getChapter() == null ? unit : detail.getChapter());
			item.setKnowledgePoint(detail.getKnowledgePoint());
			item.setStudentAnswer(detail.getStudentAnswer());
			item.setAnalysis(detail.getAnalysis());
			item.setCorrect(detail.getCorrect());
			item.setCorrectAnswer(joinAnswers(detail.getCorrectAnswers()));
			item.setQuestion(withCorrectAnswer(detail.getQuestion(), detail.getCorrectAnswers()));
			items.add(item);
		}
		EnglishPracticeRecordRequest request = new EnglishPracticeRecordRequest();
		request.setModule(module);
		request.setVersion(version);
		request.setGrade(grade);
		request.setTerm(term);
		request.setUnit(unit);
		request.setItems(items);
		return record(userId, request);
	}

	@Override
	public EnglishPracticeSummaryView summary(String userId, String module, String unit) {
		if (!StringUtils.hasText(userId)) {
			return null;
		}
		return buildSummary(userId, normalizeModule(module), unit, null, null);
	}

	@Override
	public EnglishPracticeRecordView last(String userId, String module, String unit, String excludeId) {
		if (!StringUtils.hasText(userId)) {
			return null;
		}
		List<EnglishPracticeRecord> records = recent(userId, normalizeModule(module), unit, excludeId);
		if (records.isEmpty()) {
			return null;
		}
		return toView(records.get(0));
	}

	/**
	 * 组装成绩概览：最近一次为当前这次，上一次取时间上紧邻的前一条。
	 *
	 * @param currentRecordId 本次刚写入的记录 ID（查询概览时为空）
	 * @param currentScore    本次得分（查询概览时为空）
	 */
	private EnglishPracticeSummaryView buildSummary(String userId, String module, String unit,
			String currentRecordId, Integer currentScore) {
		List<EnglishPracticeRecord> records = recent(userId, module, unit, null);
		EnglishPracticeSummaryView summary = new EnglishPracticeSummaryView();
		summary.setModule(module);
		summary.setUnit(unit);
		summary.setAttempts(records.size());
		int best = 0;
		Date bestTime = null;
		for (EnglishPracticeRecord record : records) {
			int score = record.getScore() == null ? 0 : record.getScore();
			if (score > best) {
				best = score;
				bestTime = record.getCreateAt();
			}
		}
		Integer last = records.isEmpty() ? null : records.get(0).getScore();
		Integer previous = records.size() > 1 ? records.get(1).getScore() : null;
		if (currentScore != null) {
			last = currentScore;
			if (currentScore > best) {
				best = currentScore;
				bestTime = new Date();
			}
		}
		summary.setBestScore(best);
		summary.setBestTime(bestTime);
		summary.setLastScore(last);
		summary.setPreviousScore(previous);
		summary.setLastTime(records.isEmpty() ? null : records.get(0).getCreateAt());
		summary.setRecordId(currentRecordId);
		Long wrongCount = wrongQuestionMapper.selectCount(Wrappers.<EnglishWrongQuestion>lambdaQuery()
				.eq(EnglishWrongQuestion::getUserId, userId)
				.eq(EnglishWrongQuestion::getModule, module)
				.eq(StringUtils.hasText(unit), EnglishWrongQuestion::getUnit, unit)
				.eq(EnglishWrongQuestion::getEliminated, 0));
		summary.setWrongCount(wrongCount == null ? 0 : wrongCount.intValue());
		return summary;
	}

	/**
	 * 写入/累加错题（同一学员同一题只保留一条）。
	 */
	private void saveWrongQuestions(String userId, String module, EnglishPracticeRecordRequest request,
			List<EnglishPracticeItemView> items, Date now) {
		for (EnglishPracticeItemView item : items) {
			if (Integer.valueOf(1).equals(item.getCorrect())
					|| !StringUtils.hasText(item.getQuestionId())) {
				continue;
			}
			EnglishWrongQuestion wrong = wrongQuestionMapper.selectOne(Wrappers.<EnglishWrongQuestion>lambdaQuery()
					.eq(EnglishWrongQuestion::getUserId, userId)
					.eq(EnglishWrongQuestion::getQuestionId, item.getQuestionId())
					.last("LIMIT 1"));
			if (wrong == null) {
				wrong = new EnglishWrongQuestion();
				wrong.setUserId(userId);
				wrong.setQuestionId(item.getQuestionId());
				wrong.setModule(module);
				wrong.setVersion(request.getVersion());
				wrong.setGrade(request.getGrade());
				wrong.setTerm(request.getTerm());
				wrong.setUnit(request.getUnit());
				wrong.setChapter(item.getChapter());
				wrong.setKnowledgePoint(item.getKnowledgePoint());
				wrong.setQuestionType(item.getQuestionType());
				wrong.setQuestionJson(writeJson(item.getQuestion()));
				wrong.setCorrectAnswer(item.getCorrectAnswer());
				wrong.setAnswerAnalysis(item.getAnalysis());
				wrong.setWrongCount(1);
				wrong.setLastWrongTime(now);
				wrong.setEliminated(0);
				wrongQuestionMapper.insert(wrong);
			}
			else {
				wrong.setModule(module);
				wrong.setChapter(item.getChapter());
				wrong.setKnowledgePoint(item.getKnowledgePoint());
				wrong.setQuestionType(item.getQuestionType());
				wrong.setQuestionJson(writeJson(item.getQuestion()));
				wrong.setCorrectAnswer(item.getCorrectAnswer());
				wrong.setAnswerAnalysis(item.getAnalysis());
				wrong.setWrongCount((wrong.getWrongCount() == null ? 0 : wrong.getWrongCount()) + 1);
				wrong.setLastWrongTime(now);
				wrong.setEliminated(0);
				wrongQuestionMapper.updateById(wrong);
			}
		}
	}

	@Override
	public List<EnglishPracticeHistoryView> history(String userId, String module, String unit, Integer limit) {
		if (!StringUtils.hasText(userId)) {
			return Collections.emptyList();
		}
		int size = limit == null || limit <= 0 ? DEFAULT_HISTORY_PAGE : Math.min(limit, HISTORY_LIMIT);
		List<EnglishPracticeRecord> records = recordMapper.selectList(Wrappers.<EnglishPracticeRecord>lambdaQuery()
				.eq(EnglishPracticeRecord::getUserId, userId)
				.eq(EnglishPracticeRecord::getModule, normalizeModule(module))
				.eq(StringUtils.hasText(unit), EnglishPracticeRecord::getUnit, unit)
				.orderByDesc(EnglishPracticeRecord::getCreateAt)
				.last("LIMIT " + size));
		if (records == null || records.isEmpty()) {
			return Collections.emptyList();
		}
		List<EnglishPracticeHistoryView> list = new ArrayList<>(records.size());
		for (EnglishPracticeRecord record : records) {
			EnglishPracticeHistoryView view = new EnglishPracticeHistoryView();
			view.setId(record.getId());
			view.setModule(record.getModule());
			view.setUnit(record.getUnit());
			view.setScore(record.getScore());
			view.setCorrect(record.getCorrectCount());
			view.setTotal(record.getTotalCount());
			view.setAccuracy(record.getAccuracy());
			view.setDurationSeconds(record.getDurationSeconds());
			view.setCreateAt(record.getCreateAt());
			list.add(view);
		}
		return list;
	}

	@Override
	public EnglishPracticeRecordView detail(String userId, String recordId) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(recordId)) {
			return null;
		}
		// 只允许读取本人记录，避免用记录 ID 越权查看他人作答
		EnglishPracticeRecord record = recordMapper.selectOne(Wrappers.<EnglishPracticeRecord>lambdaQuery()
				.eq(EnglishPracticeRecord::getId, recordId)
				.eq(EnglishPracticeRecord::getUserId, userId)
				.last("LIMIT 1"));
		return record == null ? null : toView(record);
	}

	/**
	 * 最近作答记录（按时间倒序，最多 HISTORY_LIMIT 条）。
	 */
	private List<EnglishPracticeRecord> recent(String userId, String module, String unit, String excludeId) {
		List<EnglishPracticeRecord> records = recordMapper.selectList(Wrappers.<EnglishPracticeRecord>lambdaQuery()
				.eq(EnglishPracticeRecord::getUserId, userId)
				.eq(EnglishPracticeRecord::getModule, module)
				.eq(StringUtils.hasText(unit), EnglishPracticeRecord::getUnit, unit)
				.ne(excludeId != null && StringUtils.hasText(excludeId), EnglishPracticeRecord::getId, excludeId)
				.orderByDesc(EnglishPracticeRecord::getCreateAt)
				.last("LIMIT " + HISTORY_LIMIT));
		return records == null ? Collections.<EnglishPracticeRecord>emptyList() : records;
	}

	private EnglishPracticeRecordView toView(EnglishPracticeRecord record) {
		EnglishPracticeRecordView view = new EnglishPracticeRecordView();
		view.setId(record.getId());
		view.setModule(record.getModule());
		view.setUnit(record.getUnit());
		view.setScore(record.getScore());
		view.setCorrect(record.getCorrectCount());
		view.setTotal(record.getTotalCount());
		view.setAccuracy(record.getAccuracy());
		view.setDurationSeconds(record.getDurationSeconds());
		view.setCreateAt(record.getCreateAt());
		List<EnglishPracticeItemView> items = readItems(record.getDetails());
		view.setItems(items);
		return view;
	}

	/**
	 * 把报告里的题目补上标准答案，供错题重做判分（报告题目默认剥离答案）。
	 */
	private StudentQuestionView withCorrectAnswer(StudentQuestionView question, List<String> correctAnswers) {
		if (question == null || question.getSchema() == null || correctAnswers == null || correctAnswers.isEmpty()) {
			return question;
		}
		// 快照写入错题本前补标准答案；必须先深拷贝，避免把答案回写给本次交卷的报告
		StudentQuestionView copy = objectMapper.convertValue(question, StudentQuestionView.class);
		copy.getSchema().getAttribute().setExamCorrectAnswer(joinAnswers(correctAnswers).replace(" | ", "\n"));
		return copy;
	}

	private String joinAnswers(List<String> answers) {
		if (answers == null || answers.isEmpty()) {
			return null;
		}
		StringBuilder builder = new StringBuilder();
		for (String answer : answers) {
			if (builder.length() > 0) {
				builder.append(" | ");
			}
			builder.append(answer == null ? "" : answer);
		}
		return builder.toString();
	}

	private String normalizeModule(String module) {
		return StringUtils.hasText(module) ? module.trim().toLowerCase() : "unit";
	}

	private String writeJson(Object value) {
		if (value == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(value);
		}
		catch (Exception e) {
			log.warn("英语练习快照序列化失败: {}", e.getMessage());
			return null;
		}
	}

	private List<EnglishPracticeItemView> readItems(String json) {
		if (!StringUtils.hasText(json)) {
			return new ArrayList<>();
		}
		try {
			List<EnglishPracticeItemView> items = objectMapper.readValue(json,
					new TypeReference<List<EnglishPracticeItemView>>() {
					});
			return items == null ? new ArrayList<>() : items;
		}
		catch (Exception e) {
			log.warn("英语练习快照解析失败: {}", e.getMessage());
			return new ArrayList<>();
		}
	}
}
