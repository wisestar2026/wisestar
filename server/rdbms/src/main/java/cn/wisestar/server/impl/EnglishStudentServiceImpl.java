package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishLearningLog;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentenceBook;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishLearningLogMapper;
import cn.wisestar.server.mapper.EnglishSentenceBookMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.EnglishStudentService;
import cn.wisestar.server.service.EnglishUnitService;
import cn.wisestar.server.service.RewardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	private final EnglishUnitService englishUnitService;
	private final SubjectMapper subjectMapper;
	private final RewardService rewardService;

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

		Map<String, String> wordUnitById = new HashMap<>();
		for (EnglishWord word : words) {
			wordUnitById.put(word.getId(), word.getUnit());
		}
		Map<String, String> sentenceUnitById = new HashMap<>();
		for (EnglishSentence sentence : sentences) {
			sentenceUnitById.put(sentence.getId(), sentence.getUnit());
		}

		Date now = new Date();
		List<EnglishWordBook> wordBooks = loadWordBooks(userId, new ArrayList<>(wordUnitById.keySet()));
		List<EnglishSentenceBook> sentenceBooks = loadSentenceBooks(userId, new ArrayList<>(sentenceUnitById.keySet()));

		List<EnglishUnitProgressView> result = new ArrayList<>();
		for (EnglishUnitView unit : units) {
			EnglishUnitProgressView view = new EnglishUnitProgressView();
			view.setUnit(unit.getUnit());
			view.setSort(unit.getSort());
			view.setWordCount(unit.getWordCount());
			view.setSentenceCount(unit.getSentenceCount());
			view.setGrammarCount(unit.getGrammarCount());

			int wordFinished = 0;
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

			// 综合熟练度 = (单词 + 句子 familiarity 之和) / (4 × 词句总数)，0~100
			int totalItems = (unit.getWordCount() == null ? 0 : unit.getWordCount())
					+ (unit.getSentenceCount() == null ? 0 : unit.getSentenceCount());
			int mastery = totalItems == 0 ? 0
					: (int) Math.round(familiaritySum * 100.0 / (4.0 * totalItems));

			view.setWordFinished(wordFinished);
			view.setSentenceFinished(sentenceFinished);
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
	public List<EnglishGrammarView> grammars(String version, String grade, String term, String unit) {
		List<EnglishGrammar> list = englishGrammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(version != null, EnglishGrammar::getVersion, version)
				.eq(grade != null, EnglishGrammar::getGrade, grade)
				.eq(term != null, EnglishGrammar::getTerm, term)
				.eq(unit != null, EnglishGrammar::getUnit, unit)
				.orderByAsc(EnglishGrammar::getSort)
				.orderByAsc(EnglishGrammar::getId));
		return list.stream().map(this::toGrammarView).collect(Collectors.toList());
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

	private List<EnglishWordBook> loadWordBooks(String userId, List<String> wordIds) {
		if (userId == null || wordIds.isEmpty()) {
			return new ArrayList<>();
		}
		return englishWordBookMapper.selectList(Wrappers.<EnglishWordBook>lambdaQuery()
				.eq(EnglishWordBook::getUserId, userId)
				.in(EnglishWordBook::getWordId, wordIds));
	}

	private List<EnglishSentenceBook> loadSentenceBooks(String userId, List<String> sentenceIds) {
		if (userId == null || sentenceIds.isEmpty()) {
			return new ArrayList<>();
		}
		return englishSentenceBookMapper.selectList(Wrappers.<EnglishSentenceBook>lambdaQuery()
				.eq(EnglishSentenceBook::getUserId, userId)
				.in(EnglishSentenceBook::getSentenceId, sentenceIds));
	}

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

	private boolean isDue(Date nextReviewTime, Date now) {
		return nextReviewTime != null && !nextReviewTime.after(now);
	}

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
