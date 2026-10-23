package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.core.uitls.AnswerJudgeUtil;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;
import cn.wisestar.server.domain.dto.english.EnglishWrongQuestionView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoRequest;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoView;
import cn.wisestar.server.domain.model.DetectRecord;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishGrammarBook;
import cn.wisestar.server.domain.model.EnglishLearningLog;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentenceBook;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.domain.model.EnglishWrongRedo;
import cn.wisestar.server.domain.model.EnglishWrongQuestion;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.EnglishGrammarBookMapper;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishLearningLogMapper;
import cn.wisestar.server.mapper.EnglishSentenceBookMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.mapper.EnglishWrongRedoMapper;
import cn.wisestar.server.mapper.EnglishWrongQuestionMapper;
import cn.wisestar.server.mapper.DetectRecordMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.EnglishStudentService;
import cn.wisestar.server.service.EnglishUnitService;
import cn.wisestar.server.service.GrowthArchiveService;
import cn.wisestar.server.service.RewardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 英语学员学习服务实现。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishStudentServiceImpl implements EnglishStudentService {

	private final EnglishWordMapper englishWordMapper;
	private final EnglishWordBookMapper englishWordBookMapper;
	private final EnglishSentenceMapper englishSentenceMapper;
	private final EnglishSentenceBookMapper englishSentenceBookMapper;
	private final EnglishLearningLogMapper englishLearningLogMapper;
	private final EnglishGrammarMapper englishGrammarMapper;
	private final EnglishGrammarBookMapper englishGrammarBookMapper;
	private final DetectRecordMapper detectRecordMapper;
	private final EnglishWrongRedoMapper englishWrongRedoMapper;
	private final EnglishWrongQuestionMapper englishWrongQuestionMapper;
	private final TemplateMapper templateMapper;
	private final EnglishUnitService englishUnitService;
	private final SubjectMapper subjectMapper;
	private final RewardService rewardService;
	private final GrowthArchiveService growthArchiveService;
	private final ObjectMapper objectMapper;
	private final EnglishPracticeEvaluator englishPracticeEvaluator;

	@Override
	public List<EnglishUnitProgressView> unitProgress(String userId, String version, String grade, String term) {
		List<EnglishUnitView> units = englishUnitService.listByBook(version, grade, term);
		List<EnglishWord> words = englishWordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(version != null, EnglishWord::getVersion, version)
				.eq(grade != null, EnglishWord::getGrade, grade)
				.eq(term != null, EnglishWord::getTerm, term));
		List<EnglishSentence> sentences = englishSentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(version != null, EnglishSentence::getVersion, version)
				.eq(grade != null, EnglishSentence::getGrade, grade)
				.eq(term != null, EnglishSentence::getTerm, term));
		List<EnglishGrammar> grammars = englishGrammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(version != null, EnglishGrammar::getVersion, version)
				.eq(grade != null, EnglishGrammar::getGrade, grade)
				.eq(term != null, EnglishGrammar::getTerm, term));

		Map<String, String> wordUnitById = new HashMap<>();
		for (EnglishWord word : words) {
			wordUnitById.put(word.getId(), word.getUnit());
		}
		Map<String, String> sentenceUnitById = new HashMap<>();
		for (EnglishSentence sentence : sentences) {
			sentenceUnitById.put(sentence.getId(), sentence.getUnit());
		}
		Map<String, String> grammarUnitById = new HashMap<>();
		for (EnglishGrammar grammar : grammars) {
			grammarUnitById.put(grammar.getId(), grammar.getUnit());
		}

		Date now = new Date();
		List<EnglishWordBook> wordBooks = loadWordBooks(userId, new ArrayList<>(wordUnitById.keySet()));
		List<EnglishSentenceBook> sentenceBooks = loadSentenceBooks(userId, new ArrayList<>(sentenceUnitById.keySet()));
		List<EnglishGrammarBook> grammarBooks = loadGrammarBooks(userId, new ArrayList<>(grammarUnitById.keySet()));

		List<EnglishUnitProgressView> result = new ArrayList<>();
		for (EnglishUnitView unit : units) {
			EnglishUnitProgressView view = new EnglishUnitProgressView();
			view.setUnit(unit.getUnit());
			view.setSort(unit.getSort());
			view.setWordCount(unit.getWordCount());
			view.setSentenceCount(unit.getSentenceCount());
			view.setGrammarCount(unit.getGrammarCount());

			int wordFinished = 0;
			int wordMastered = 0;
			int wordWeak = 0;
			int familiaritySum = 0;
			int reviewDue = 0;
			for (EnglishWordBook book : wordBooks) {
				if (!unit.getUnit().equals(wordUnitById.get(book.getWordId()))) {
					continue;
				}
				int familiarity = book.getFamiliarity() == null ? 0 : book.getFamiliarity();
				familiaritySum += Math.min(Math.max(familiarity, 0), 4);
				if (familiarity >= 1) {
					wordFinished++;
				}
				int wordMastery = EnglishMastery.level(book);
				if (wordMastery == EnglishMastery.LEVEL_MASTERED) {
					wordMastered++;
				}
				else if (wordMastery == EnglishMastery.LEVEL_WEAK) {
					wordWeak++;
				}
				if (isDue(book.getNextReviewTime(), now)) {
					reviewDue++;
				}
			}

			int sentenceFinished = 0;
			for (EnglishSentenceBook book : sentenceBooks) {
				if (!unit.getUnit().equals(sentenceUnitById.get(book.getSentenceId()))) {
					continue;
				}
				int familiarity = book.getFamiliarity() == null ? 0 : book.getFamiliarity();
				familiaritySum += Math.min(Math.max(familiarity, 0), 4);
				if (familiarity >= 1) {
					sentenceFinished++;
				}
				if (isDue(book.getNextReviewTime(), now)) {
					reviewDue++;
				}
			}

			int grammarFinished = 0;
			for (EnglishGrammarBook book : grammarBooks) {
				if (!unit.getUnit().equals(grammarUnitById.get(book.getGrammarId()))) {
					continue;
				}
				int familiarity = book.getFamiliarity() == null ? 0 : book.getFamiliarity();
				familiaritySum += Math.min(Math.max(familiarity, 0), 4);
				if (familiarity >= 1) {
					grammarFinished++;
				}
			}

			// 综合熟练度 = (单词 + 句子 + 语法 familiarity 之和) / (4 × 三类总数)，0~100
			int totalItems = (unit.getWordCount() == null ? 0 : unit.getWordCount())
					+ (unit.getSentenceCount() == null ? 0 : unit.getSentenceCount())
					+ (unit.getGrammarCount() == null ? 0 : unit.getGrammarCount());
			int mastery = totalItems == 0 ? 0
					: (int) Math.round(familiaritySum * 100.0 / (4.0 * totalItems));

			int wordTotal = unit.getWordCount() == null ? 0 : unit.getWordCount();
			// 单词通关：掌握 >= 80% 且待巩固 <= 10%（待巩固含历史遗留的「有记录无首答」单词）
			boolean wordPass = wordTotal > 0
					&& wordMastered * 100 >= wordTotal * EnglishMastery.UNIT_PASS_RATE
					&& wordWeak * 100 <= wordTotal * EnglishMastery.UNIT_WEAK_RATE;

			view.setWordFinished(wordFinished);
			view.setWordMastered(wordMastered);
			view.setWordWeak(wordWeak);
			view.setWordPass(wordPass);
			view.setSentenceFinished(sentenceFinished);
			view.setGrammarFinished(grammarFinished);
			view.setMastery(mastery);
			view.setReviewDue(reviewDue);
			result.add(view);
		}
		return result;
	}

	@Override
	public List<EnglishSentenceView> sentences(String userId, String version, String grade, String term, String unit) {
		List<EnglishSentence> list = englishSentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(version != null, EnglishSentence::getVersion, version)
				.eq(grade != null, EnglishSentence::getGrade, grade)
				.eq(term != null, EnglishSentence::getTerm, term)
				.eq(unit != null, EnglishSentence::getUnit, unit)
				.orderByAsc(EnglishSentence::getSection)
				.orderByAsc(EnglishSentence::getSort)
				.orderByAsc(EnglishSentence::getId));
		List<EnglishSentenceView> views = list.stream().map(this::toView).collect(Collectors.toList());
		fillSentenceFamiliarity(userId, views);
		return views;
	}

	@Override
	public List<EnglishGrammarView> grammars(String userId, String version, String grade, String term, String unit) {
		List<EnglishGrammar> list = englishGrammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(version != null, EnglishGrammar::getVersion, version)
				.eq(grade != null, EnglishGrammar::getGrade, grade)
				.eq(term != null, EnglishGrammar::getTerm, term)
				.eq(unit != null, EnglishGrammar::getUnit, unit)
				.orderByAsc(EnglishGrammar::getSort)
				.orderByAsc(EnglishGrammar::getId));
		List<EnglishGrammarView> views = list.stream().map(this::toGrammarView).collect(Collectors.toList());
		fillGrammarFamiliarity(userId, views);
		return views;
	}

	@Override
	public void recordGrammar(String userId, String grammarId, boolean correct) {
		Date now = new Date();
		EnglishGrammarBook book = englishGrammarBookMapper.selectOne(
				Wrappers.<EnglishGrammarBook>lambdaQuery()
						.eq(EnglishGrammarBook::getUserId, userId)
						.eq(EnglishGrammarBook::getGrammarId, grammarId));

		if (book == null) {
			book = new EnglishGrammarBook();
			book.setUserId(userId);
			book.setGrammarId(grammarId);
			book.setFamiliarity(correct ? 1 : 0);
			book.setCorrectCount(correct ? 1 : 0);
			book.setWrongCount(correct ? 0 : 1);
			book.setLastReviewTime(now);
			book.setNextReviewTime(EnglishReviewScheduler.nextReviewTime(book.getFamiliarity()));
			englishGrammarBookMapper.insert(book);
		} else {
			int familiarity = EnglishReviewScheduler.adjustFamiliarity(
					book.getFamiliarity() == null ? 0 : book.getFamiliarity(), correct);
			book.setFamiliarity(familiarity);
			book.setCorrectCount((book.getCorrectCount() == null ? 0 : book.getCorrectCount()) + (correct ? 1 : 0));
			book.setWrongCount((book.getWrongCount() == null ? 0 : book.getWrongCount()) + (correct ? 0 : 1));
			book.setLastReviewTime(now);
			book.setNextReviewTime(EnglishReviewScheduler.nextReviewTime(familiarity));
			englishGrammarBookMapper.updateById(book);
		}

		writeLog(userId, "grammar", grammarId, 0, correct ? 1 : 0);

		// 成长档案：语法练习轨迹留痕（按 grammarId + 日期累加，幂等；失败不阻断）
		try {
			String date = LocalDate.now().toString();
			GrowthEventContext growth = new GrowthEventContext();
			growth.setStudentId(userId);
			growth.setSubjectId(englishSubjectId());
			growth.setEventType("GRAMMAR");
			growth.setSourceType("grammar");
			growth.setSourceId(grammarId + ":" + date);
			growth.setEventDate(date);
			growth.setQuestionCount(1);
			growth.setCorrectCount(correct ? 1 : 0);
			growth.setAccuracy(correct ? 100 : 0);
			EnglishGrammar grammar = englishGrammarMapper.selectById(grammarId);
			if (grammar != null && grammar.getTitle() != null) {
				List<String> names = new ArrayList<>();
				names.add(grammar.getTitle());
				growth.setKnowledgePoints(names);
			}
			growth.setTitle("英语语法练习");
			growthArchiveService.record(growth);
		}
		catch (Exception e) {
			log.warn("record grammar: growth record failed, ignored", e);
		}
	}

	@Override
	public List<EnglishSentenceView> studySentences(String userId, int limit) {
		List<EnglishSentenceBook> books = englishSentenceBookMapper.selectList(
				Wrappers.<EnglishSentenceBook>lambdaQuery()
						.eq(EnglishSentenceBook::getUserId, userId)
						.le(EnglishSentenceBook::getNextReviewTime, new Date())
						.orderByAsc(EnglishSentenceBook::getNextReviewTime)
						.last("LIMIT " + limit));

		List<EnglishSentenceView> views;
		if (books.isEmpty()) {
			views = englishSentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
							.orderByDesc(EnglishSentence::getCreateAt)
							.last("LIMIT " + limit))
					.stream().map(this::toView).collect(Collectors.toList());
		} else {
			List<String> sentenceIds = books.stream()
					.map(EnglishSentenceBook::getSentenceId)
					.collect(Collectors.toList());
			views = englishSentenceMapper.selectBatchIds(sentenceIds).stream()
					.map(this::toView)
					.collect(Collectors.toList());
		}
		fillSentenceFamiliarity(userId, views);
		return views;
	}

	@Override
	public void recordSentence(String userId, String sentenceId, boolean correct) {
		Date now = new Date();
		EnglishSentenceBook book = englishSentenceBookMapper.selectOne(
				Wrappers.<EnglishSentenceBook>lambdaQuery()
						.eq(EnglishSentenceBook::getUserId, userId)
						.eq(EnglishSentenceBook::getSentenceId, sentenceId));

		if (book == null) {
			book = new EnglishSentenceBook();
			book.setUserId(userId);
			book.setSentenceId(sentenceId);
			book.setFamiliarity(correct ? 1 : 0);
			book.setCorrectCount(correct ? 1 : 0);
			book.setWrongCount(correct ? 0 : 1);
			book.setLastReviewTime(now);
			book.setNextReviewTime(EnglishReviewScheduler.nextReviewTime(book.getFamiliarity()));
			englishSentenceBookMapper.insert(book);
		} else {
			int familiarity = EnglishReviewScheduler.adjustFamiliarity(
					book.getFamiliarity() == null ? 0 : book.getFamiliarity(), correct);
			book.setFamiliarity(familiarity);
			book.setCorrectCount((book.getCorrectCount() == null ? 0 : book.getCorrectCount()) + (correct ? 1 : 0));
			book.setWrongCount((book.getWrongCount() == null ? 0 : book.getWrongCount()) + (correct ? 0 : 1));
			book.setLastReviewTime(now);
			book.setNextReviewTime(EnglishReviewScheduler.nextReviewTime(familiarity));
			englishSentenceBookMapper.updateById(book);
		}

		writeLog(userId, "sentence", sentenceId, 0, correct ? 1 : 0);
		// 逐句结果纳入「单元·句子」掌握度，供薄弱点检测使用
		EnglishSentence sentence = englishSentenceMapper.selectById(sentenceId);
		if (sentence != null) {
			englishPracticeEvaluator.record(userId, sentence.getVersion(), sentence.getGrade(), sentence.getTerm(),
					sentence.getUnit(), EnglishPracticeEvaluator.MODULE_SENTENCE, correct);
		}
	}

	@Override
	public void recordScenarioPractice(String userId, String version, String grade, String term, String unit,
			String module, int correct, int total) {
		// 情景练习按整组正确率折算，纳入「单元·模块」掌握度
		englishPracticeEvaluator.recordBatch(userId, version, grade, term, unit, module, correct, total);
	}

	@Override
	public List<ReviewSessionView> reviewSession(String userId, int limit) {
		Date now = new Date();
		List<EnglishWordBook> wordBooks = englishWordBookMapper.selectList(
				Wrappers.<EnglishWordBook>lambdaQuery()
						.eq(EnglishWordBook::getUserId, userId)
						.le(EnglishWordBook::getNextReviewTime, now)
						.orderByAsc(EnglishWordBook::getNextReviewTime)
						.last("LIMIT " + limit));
		List<EnglishSentenceBook> sentenceBooks = englishSentenceBookMapper.selectList(
				Wrappers.<EnglishSentenceBook>lambdaQuery()
						.eq(EnglishSentenceBook::getUserId, userId)
						.le(EnglishSentenceBook::getNextReviewTime, now)
						.orderByAsc(EnglishSentenceBook::getNextReviewTime)
						.last("LIMIT " + limit));

		Map<String, EnglishWord> wordById = new HashMap<>();
		if (!wordBooks.isEmpty()) {
			List<String> ids = wordBooks.stream().map(EnglishWordBook::getWordId).collect(Collectors.toList());
			for (EnglishWord word : englishWordMapper.selectBatchIds(ids)) {
				wordById.put(word.getId(), word);
			}
		}
		Map<String, EnglishSentence> sentenceById = new HashMap<>();
		if (!sentenceBooks.isEmpty()) {
			List<String> ids = sentenceBooks.stream().map(EnglishSentenceBook::getSentenceId).collect(Collectors.toList());
			for (EnglishSentence sentence : englishSentenceMapper.selectBatchIds(ids)) {
				sentenceById.put(sentence.getId(), sentence);
			}
		}

		List<ReviewSessionView> all = new ArrayList<>();
		for (EnglishWordBook book : wordBooks) {
			EnglishWord word = wordById.get(book.getWordId());
			if (word == null) {
				continue;
			}
			ReviewSessionView view = new ReviewSessionView();
			view.setType("word");
			view.setId(word.getId());
			view.setPrompt(word.getSpell());
			view.setAnswer(word.getMeaning());
			view.setPhonetic(word.getPhonetic());
			view.setExampleSentence(word.getExampleSentence());
			view.setAudioUrl(word.getAudioUrl());
			view.setImageUrl(word.getImageUrl());
			view.setFamiliarity(book.getFamiliarity());
			view.setNextReviewTime(book.getNextReviewTime());
			all.add(view);
		}
		for (EnglishSentenceBook book : sentenceBooks) {
			EnglishSentence sentence = sentenceById.get(book.getSentenceId());
			if (sentence == null) {
				continue;
			}
			ReviewSessionView view = new ReviewSessionView();
			view.setType("sentence");
			view.setId(sentence.getId());
			view.setPrompt(sentence.getEn());
			view.setAnswer(sentence.getZh());
			view.setAudioUrl(sentence.getAudioUrl());
			view.setFamiliarity(book.getFamiliarity());
			view.setNextReviewTime(book.getNextReviewTime());
			all.add(view);
		}

		all.sort(Comparator.comparing(ReviewSessionView::getNextReviewTime,
				Comparator.nullsLast(Comparator.naturalOrder())));
		return all.stream().limit(limit).collect(Collectors.toList());
	}

	@Override
	public StudentPreviewCompleteView recordSession(String userId, String type, int durationSeconds, int correctCount) {
		writeLog(userId, type, "-", durationSeconds, correctCount);
		return settleSessionReward(userId, type);
	}

	@Override
	public List<EnglishWrongQuestionView> wrongQuestions(String userId, Integer limit) {
		if (userId == null) {
			return new ArrayList<>();
		}
		Map<String, EnglishWrongQuestionView> byQuestion = new LinkedHashMap<>();
		String subjectId = englishSubjectId();
		// 已被消灭的题目（学员在复习页重做正确后落库），聚合错题时剔除
		Set<String> eliminated = englishWrongRedoMapper.selectList(Wrappers.<EnglishWrongRedo>lambdaQuery()
						.eq(EnglishWrongRedo::getStudentId, userId))
				.stream().map(EnglishWrongRedo::getQuestionId).filter(StringUtils::hasText)
				.collect(Collectors.toSet());
		// 该学员在英语学科下的全部检测记录（最近在前，取最近一次题目快照与做错时间）
		List<DetectRecord> records = subjectId == null ? new ArrayList<>()
				: detectRecordMapper.selectList(Wrappers.<DetectRecord>lambdaQuery()
						.eq(DetectRecord::getStudentId, userId)
						.eq(DetectRecord::getSubjectId, subjectId)
						.orderByDesc(DetectRecord::getCreateAt));
		for (DetectRecord record : records) {
			List<DetectReportView.Detail> details = fromJson(record.getDetails(),
					new TypeReference<List<DetectReportView.Detail>>() {
					});
			if (details == null) {
				continue;
			}
			for (DetectReportView.Detail detail : details) {
				// 只统计做错的题（correct=0），未作答/未判分不计入
				if (detail.getCorrect() == null || detail.getCorrect() != 0
						|| detail.getQuestion() == null
						|| !StringUtils.hasText(detail.getQuestionId())) {
					continue;
				}
				if (eliminated.contains(detail.getQuestionId())) {
					continue;
				}
				EnglishWrongQuestionView view = byQuestion.get(detail.getQuestionId());
				if (view == null) {
					view = new EnglishWrongQuestionView();
					view.setQuestionId(detail.getQuestionId());
					view.setQuestionType(detail.getQuestionType());
					view.setChapter(detail.getChapter());
					view.setKnowledgePoint(detail.getKnowledgePoint());
					view.setQuestion(detail.getQuestion());
					view.setWrongCount(0);
					view.setLastWrongTime(record.getCreateAt());
					byQuestion.put(detail.getQuestionId(), view);
				}
				view.setWrongCount(view.getWrongCount() + 1);
			}
		}
		// 练习错题本（阅读/单元/语法练习交卷时单独落库），与检测记录聚合结果合并
		List<EnglishWrongQuestion> stored = englishWrongQuestionMapper.selectList(
				Wrappers.<EnglishWrongQuestion>lambdaQuery()
						.eq(EnglishWrongQuestion::getUserId, userId)
						.eq(EnglishWrongQuestion::getEliminated, 0)
						.orderByDesc(EnglishWrongQuestion::getLastWrongTime));
		for (EnglishWrongQuestion wrong : stored) {
			if (!StringUtils.hasText(wrong.getQuestionId()) || byQuestion.containsKey(wrong.getQuestionId())) {
				continue;
			}
			EnglishWrongQuestionView view = new EnglishWrongQuestionView();
			view.setQuestionId(wrong.getQuestionId());
			view.setQuestionType(wrong.getQuestionType());
			view.setChapter(StringUtils.hasText(wrong.getChapter()) ? wrong.getChapter() : wrong.getUnit());
			view.setKnowledgePoint(wrong.getKnowledgePoint());
			view.setQuestion(stripAnswer(fromJson(wrong.getQuestionJson(),
					new TypeReference<StudentQuestionView>() {
					})));
			view.setWrongCount(wrong.getWrongCount() == null ? 1 : wrong.getWrongCount());
			view.setLastWrongTime(wrong.getLastWrongTime());
			byQuestion.put(wrong.getQuestionId(), view);
		}
		List<EnglishWrongQuestionView> list = new ArrayList<>(byQuestion.values());
		if (limit != null && limit > 0 && list.size() > limit) {
			return new ArrayList<>(list.subList(0, limit));
		}
		return list;
	}

	@Override
	public StudentQuestionView wrongQuestion(String userId, String questionId) {
		if (userId == null || !StringUtils.hasText(questionId)) {
			return null;
		}
		EnglishWrongQuestion stored = findStoredWrong(userId, questionId);
		if (stored != null) {
			// 内容包题目（阅读/单元练习）没有 t_template 记录，直接回放交卷时的题目快照
			return fromJson(stored.getQuestionJson(), new TypeReference<StudentQuestionView>() {
			});
		}
		// 仅允许取本人英语错题，防越权取题
		boolean isWrong = wrongQuestions(userId, null).stream()
				.anyMatch(view -> questionId.equals(view.getQuestionId()));
		if (!isWrong) {
			return null;
		}
		Template template = templateMapper.selectById(questionId);
		if (template == null) {
			return null;
		}
		StudentQuestionView view = new StudentQuestionView();
		view.setId(template.getId());
		view.setName(template.getName());
		view.setQuestionType(template.getQuestionType());
		view.setTag(template.getTag());
		// 重做需实时判分并展示正确答案，此处返回题目原始内容（含标准答案）
		view.setSchema(template.getTemplate());
		return view;
	}

	@Override
	public StudentWrongRedoView redoWrong(String userId, StudentWrongRedoRequest request) {
		if (userId == null || request == null || !StringUtils.hasText(request.getQuestionId())) {
			throw new ValidationException("题目ID不能为空");
		}
		String questionId = request.getQuestionId();
		EnglishWrongQuestion stored = findStoredWrong(userId, questionId);
		if (stored != null) {
			return redoStoredWrong(userId, stored, request);
		}
		// 仅允许重做本人英语错题，防越权
		boolean isWrong = wrongQuestions(userId, null).stream()
				.anyMatch(view -> questionId.equals(view.getQuestionId()));
		if (!isWrong) {
			throw new ValidationException("未找到该错题");
		}
		Template template = templateMapper.selectById(questionId);
		if (template == null || template.getTemplate() == null) {
			throw new ValidationException("题目不存在");
		}
		StudentWrongRedoView view = new StudentWrongRedoView();
		view.setOk(true);
		Integer correct;
		try {
			correct = AnswerJudgeUtil.evaluate(template.getTemplate(), request.getAnswer());
		}
		catch (Exception e) {
			throw new ValidationException("作答内容无法判分");
		}
		if (!Integer.valueOf(1).equals(correct)) {
			return view;
		}
		// 幂等落库：同一学员同一题只记一条「已消灭」
		Long exists = englishWrongRedoMapper.selectCount(Wrappers.<EnglishWrongRedo>lambdaQuery()
				.eq(EnglishWrongRedo::getStudentId, userId)
				.eq(EnglishWrongRedo::getQuestionId, questionId));
		if (exists == null || exists == 0) {
			EnglishWrongRedo redo = new EnglishWrongRedo();
			redo.setStudentId(userId);
			redo.setQuestionId(questionId);
			englishWrongRedoMapper.insert(redo);
		}
		view.setRemoved(true);
		return view;
	}

	/**
	 * 查询练习错题本中的未消灭错题（没有则返回 null）。
	 */
	private EnglishWrongQuestion findStoredWrong(String userId, String questionId) {
		EnglishWrongQuestion wrong = englishWrongQuestionMapper.selectOne(Wrappers.<EnglishWrongQuestion>lambdaQuery()
				.eq(EnglishWrongQuestion::getUserId, userId)
				.eq(EnglishWrongQuestion::getQuestionId, questionId)
				.eq(EnglishWrongQuestion::getEliminated, 0)
				.last("LIMIT 1"));
		return wrong;
	}

	/**
	 * 重做练习错题：按快照判分，答对后标记消灭。
	 */
	private StudentWrongRedoView redoStoredWrong(String userId, EnglishWrongQuestion stored,
			StudentWrongRedoRequest request) {
		StudentQuestionView question = fromJson(stored.getQuestionJson(),
				new TypeReference<StudentQuestionView>() {
				});
		if (question == null || question.getSchema() == null) {
			throw new ValidationException("题目不存在");
		}
		StudentWrongRedoView view = new StudentWrongRedoView();
		view.setOk(true);
		Integer correct;
		try {
			correct = AnswerJudgeUtil.evaluate(question.getSchema(), request.getAnswer());
		}
		catch (Exception e) {
			throw new ValidationException("作答内容无法判分");
		}
		if (!Integer.valueOf(1).equals(correct)) {
			return view;
		}
		stored.setEliminated(1);
		englishWrongQuestionMapper.updateById(stored);
		// 同题可能也出现在检测记录聚合结果里，同步写一条「已消灭」保证两处口径一致
		Long exists = englishWrongRedoMapper.selectCount(Wrappers.<EnglishWrongRedo>lambdaQuery()
				.eq(EnglishWrongRedo::getStudentId, userId)
				.eq(EnglishWrongRedo::getQuestionId, stored.getQuestionId()));
		if (exists == null || exists == 0) {
			EnglishWrongRedo redo = new EnglishWrongRedo();
			redo.setStudentId(userId);
			redo.setQuestionId(stored.getQuestionId());
			englishWrongRedoMapper.insert(redo);
		}
		view.setRemoved(true);
		return view;
	}

	/**
	 * 错题列表展示时剥离标准答案，避免直接看答案。
	 */
	private StudentQuestionView stripAnswer(StudentQuestionView question) {
		if (question == null || question.getSchema() == null || question.getSchema().getAttribute() == null) {
			return question;
		}
		question.getSchema().getAttribute().setExamCorrectAnswer(null);
		return question;
	}

	/**
	 * 将 JSON 字符串反序列化为指定类型（解析失败返回 null）。
	 *
	 * @param <T>    目标类型
	 * @param json   JSON 字符串
	 * @param type   目标类型引用
	 * @return 反序列化结果
	 */
	private <T> T fromJson(String json, TypeReference<T> type) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, type);
		}
		catch (Exception e) {
			log.warn("english review: detect record json parse failed, ignored", e);
			return null;
		}
	}

	/**
	 * 结算英语学习会话奖励（学习币 + 学海积分）。
	 *
	 * <p>与主系统共用 {@link RewardService}：同一份行为记录、积分累计、头衔晋升与单科学币上限。
	 * 幂等键按「英语行为类型 + 自然日」生成，保证每个类型每天只发一次，避免重复刷取。</p>
	 */
	private StudentPreviewCompleteView settleSessionReward(String userId, String type) {
		StudentPreviewCompleteView view = new StudentPreviewCompleteView();
		String action = englishAction(type);
		if (action == null) {
			view.setOk(false);
			return view;
		}
		RewardContext context = new RewardContext();
		context.setUserId(userId);
		context.setActionType(action);
		context.setSubjectId(englishSubjectId());
		context.setRefId("en:" + type + ":" + LocalDate.now());
		try {
			StudentPreviewCompleteView result = rewardService.settle(context);
			return result == null ? view : result;
		}
		catch (Exception e) {
			// 奖励结算异常不阻断英语学习
			log.warn("english session: reward settle failed, ignored. user={}, type={}", userId, type, e);
			view.setOk(false);
			return view;
		}
	}

	/**
	 * 英语学习类型 → 奖励行为类型。
	 *
	 * @param type 会话类型 word / sentence / word-quiz / review / drill
	 * @return 奖励行为类型；未知类型返回 null（不结算）
	 */
	private String englishAction(String type) {
		if (type == null) {
			return null;
		}
		switch (type) {
			case "word":
				return StudentRewardConstants.ACTION_EN_WORD;
			case "sentence":
				return StudentRewardConstants.ACTION_EN_SENTENCE;
			case "word-quiz":
				return StudentRewardConstants.ACTION_EN_WORD_QUIZ;
			case "review":
				return StudentRewardConstants.ACTION_EN_REVIEW;
			case "drill":
				return StudentRewardConstants.ACTION_EN_DRILL;
			case "scenario":
				return StudentRewardConstants.ACTION_EN_SCENARIO;
			case "pattern":
				return StudentRewardConstants.ACTION_EN_PATTERN;
			default:
				return null;
		}
	}

	/**
	 * 解析英语学科 ID（{@code t_subject.code='ENGLISH'}），使学习币计入英语学科学期上限。
	 */
	private String englishSubjectId() {
		Subject subject = subjectMapper.selectOne(Wrappers.<Subject>lambdaQuery()
				.eq(Subject::getCode, "ENGLISH")
				.last("limit 1"));
		return subject == null ? null : subject.getId();
	}

	/**
	 * 写入一条英语学习记录。
	 *
	 * @param userId       学员用户 ID
	 * @param type         学习类型
	 * @param contentId    学习内容 ID
	 * @param duration     学习时长
	 * @param correctCount 正确题数
	 */
	private void writeLog(String userId, String type, String contentId, int duration, int correctCount) {
		EnglishLearningLog log = new EnglishLearningLog();
		log.setUserId(userId);
		log.setType(type);
		log.setContentId(contentId);
		log.setDuration(duration);
		log.setCorrectCount(correctCount);
		log.setCreatedAt(new Date());
		englishLearningLogMapper.insert(log);
	}

	/**
	 * 查询指定学员在一批单词上的单词本记录。
	 *
	 * @param userId  学员用户 ID
	 * @param wordIds 单词 ID 列表
	 * @return 单词本记录列表，入参无效时返回空列表
	 */
	private List<EnglishWordBook> loadWordBooks(String userId, List<String> wordIds) {
		if (userId == null || wordIds.isEmpty()) {
			return new ArrayList<>();
		}
		return englishWordBookMapper.selectList(Wrappers.<EnglishWordBook>lambdaQuery()
				.eq(EnglishWordBook::getUserId, userId)
				.in(EnglishWordBook::getWordId, wordIds));
	}

	/**
	 * 查询指定学员在一批句子上的句子本记录。
	 *
	 * @param userId      学员用户 ID
	 * @param sentenceIds 句子 ID 列表
	 * @return 句子本记录列表，入参无效时返回空列表
	 */
	private List<EnglishSentenceBook> loadSentenceBooks(String userId, List<String> sentenceIds) {
		if (userId == null || sentenceIds.isEmpty()) {
			return new ArrayList<>();
		}
		return englishSentenceBookMapper.selectList(Wrappers.<EnglishSentenceBook>lambdaQuery()
				.eq(EnglishSentenceBook::getUserId, userId)
				.in(EnglishSentenceBook::getSentenceId, sentenceIds));
	}

	/**
	 * 查询指定学员在一批语法点上的语法本记录。
	 *
	 * @param userId     学员用户 ID
	 * @param grammarIds 语法点 ID 列表
	 * @return 语法本记录列表，入参无效时返回空列表
	 */
	private List<EnglishGrammarBook> loadGrammarBooks(String userId, List<String> grammarIds) {
		if (userId == null || grammarIds.isEmpty()) {
			return new ArrayList<>();
		}
		return englishGrammarBookMapper.selectList(Wrappers.<EnglishGrammarBook>lambdaQuery()
				.eq(EnglishGrammarBook::getUserId, userId)
				.in(EnglishGrammarBook::getGrammarId, grammarIds));
	}

	/**
	 * 为句子视图回填当前学员的熟悉度与对错次数。
	 *
	 * @param userId 学员用户 ID
	 * @param views  句子视图列表
	 */
	private void fillSentenceFamiliarity(String userId, List<EnglishSentenceView> views) {
		if (userId == null || views.isEmpty()) {
			return;
		}
		List<String> sentenceIds = views.stream().map(EnglishSentenceView::getId).collect(Collectors.toList());
		Map<String, EnglishSentenceBook> bookBySentence = englishSentenceBookMapper.selectList(
						Wrappers.<EnglishSentenceBook>lambdaQuery()
								.eq(EnglishSentenceBook::getUserId, userId)
								.in(EnglishSentenceBook::getSentenceId, sentenceIds))
				.stream()
				.collect(Collectors.toMap(EnglishSentenceBook::getSentenceId, b -> b, (a, b) -> b));
		views.forEach(v -> {
			EnglishSentenceBook book = bookBySentence.get(v.getId());
			v.setFamiliarity(book == null || book.getFamiliarity() == null ? 0 : book.getFamiliarity());
			v.setCorrectCount(book == null || book.getCorrectCount() == null ? 0 : book.getCorrectCount());
			v.setWrongCount(book == null || book.getWrongCount() == null ? 0 : book.getWrongCount());
		});
	}

	/**
	 * 为语法点视图回填当前学员的熟悉度与对错次数。
	 *
	 * @param userId 学员用户 ID
	 * @param views  语法点视图列表
	 */
	private void fillGrammarFamiliarity(String userId, List<EnglishGrammarView> views) {
		if (userId == null || views.isEmpty()) {
			return;
		}
		List<String> grammarIds = views.stream().map(EnglishGrammarView::getId).collect(Collectors.toList());
		Map<String, EnglishGrammarBook> bookByGrammar = englishGrammarBookMapper.selectList(
						Wrappers.<EnglishGrammarBook>lambdaQuery()
								.eq(EnglishGrammarBook::getUserId, userId)
								.in(EnglishGrammarBook::getGrammarId, grammarIds))
				.stream()
				.collect(Collectors.toMap(EnglishGrammarBook::getGrammarId, b -> b, (a, b) -> b));
		views.forEach(v -> {
			EnglishGrammarBook book = bookByGrammar.get(v.getId());
			v.setFamiliarity(book == null || book.getFamiliarity() == null ? 0 : book.getFamiliarity());
			v.setCorrectCount(book == null || book.getCorrectCount() == null ? 0 : book.getCorrectCount());
			v.setWrongCount(book == null || book.getWrongCount() == null ? 0 : book.getWrongCount());
		});
	}

	/**
	 * 判断指定复习时间是否已到期。
	 *
	 * @param nextReviewTime 下次复习时间
	 * @param now            当前时间
	 * @return 已到期返回 true，否则返回 false
	 */
	private boolean isDue(Date nextReviewTime, Date now) {
		return nextReviewTime != null && !nextReviewTime.after(now);
	}

	/**
	 * 将英语句子实体转换为视图对象。
	 *
	 * @param entity 句子实体
	 * @return 句子视图
	 */
	private EnglishSentenceView toView(EnglishSentence entity) {
		EnglishSentenceView view = new EnglishSentenceView();
		view.setId(entity.getId());
		view.setEn(entity.getEn());
		view.setZh(entity.getZh());
		view.setAudioUrl(entity.getAudioUrl());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setTerm(entity.getTerm());
		view.setUnit(entity.getUnit());
		view.setSection(entity.getSection());
		view.setSort(entity.getSort());
		return view;
	}

	/**
	 * 将英语语法点实体转换为视图对象。
	 *
	 * @param entity 语法点实体
	 * @return 语法点视图
	 */
	private EnglishGrammarView toGrammarView(EnglishGrammar entity) {
		EnglishGrammarView view = new EnglishGrammarView();
		view.setId(entity.getId());
		view.setTitle(entity.getTitle());
		view.setContent(entity.getContent());
		view.setExamples(entity.getExamples());
		view.setExercises(entity.getExercises());
		view.setVersion(entity.getVersion());
		view.setGrade(entity.getGrade());
		view.setTerm(entity.getTerm());
		view.setUnit(entity.getUnit());
		view.setSection(entity.getSection());
		view.setSort(entity.getSort());
		view.setUpdateAt(entity.getUpdateAt());
		return view;
	}

}
