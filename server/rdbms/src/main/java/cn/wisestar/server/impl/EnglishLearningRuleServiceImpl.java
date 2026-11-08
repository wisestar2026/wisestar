package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.english.EnglishLearningStateView;
import cn.wisestar.server.domain.dto.english.EnglishReviewCandidateView;
import cn.wisestar.server.domain.dto.english.EnglishSectionProgressView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishGrammarBook;
import cn.wisestar.server.domain.model.EnglishLearningState;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentenceBook;
import cn.wisestar.server.domain.model.EnglishUnit;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.mapper.EnglishExamAngleUseMapper;
import cn.wisestar.server.mapper.EnglishGrammarBookMapper;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishLearningStateMapper;
import cn.wisestar.server.mapper.EnglishSentenceBookMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishUnitMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.domain.model.EnglishExamAngleUse;
import cn.wisestar.server.service.EnglishLearningRuleService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 英语学习规则服务实现。
 *
 * <p>把三阶通关、三态掌握、薄弱判定与攻克计数、复习候选收敛到一处；
 * 学员端各页面只消费这里的结果，不再自行计算阈值。</p>
 *
 * <p>与既有表的边界：{@code t_english_word_book} 等三张本表继续保存熟练度与复习时间，
 * 本服务只在 {@code t_english_learning_state} 上做累加，不改写熟练度口径。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishLearningRuleServiceImpl implements EnglishLearningRuleService {

	private static final String MODULE_WORD = EnglishExamAngle.MODULE_WORD;
	private static final String MODULE_SENTENCE = EnglishExamAngle.MODULE_SENTENCE;
	private static final String MODULE_GRAMMAR = EnglishExamAngle.MODULE_GRAMMAR;

	private static final String STAGE_MEMORY = "memory";
	private static final String STAGE_PRACTICE = "practice";
	private static final String STAGE_USE = "use";

	private static final String UNSECTIONED = "未分节";

	private final EnglishLearningStateMapper stateMapper;
	private final EnglishExamAngleUseMapper angleUseMapper;
	private final EnglishWordMapper wordMapper;
	private final EnglishWordBookMapper wordBookMapper;
	private final EnglishSentenceMapper sentenceMapper;
	private final EnglishSentenceBookMapper sentenceBookMapper;
	private final EnglishGrammarMapper grammarMapper;
	private final EnglishGrammarBookMapper grammarBookMapper;
	private final EnglishUnitMapper unitMapper;

	@Override
	public Map<String, EnglishLearningStateView> loadStates(String userId, List<String> modules) {
		Map<String, EnglishLearningStateView> result = new LinkedHashMap<>();
		if (userId == null || modules == null || modules.isEmpty()) {
			return result;
		}
		List<EnglishLearningState> list = stateMapper.selectList(Wrappers.<EnglishLearningState>lambdaQuery()
				.eq(EnglishLearningState::getUserId, userId)
				.in(EnglishLearningState::getModule, modules));
		for (EnglishLearningState state : list) {
			result.put(key(state.getModule(), state.getKnowledgeId()), toView(state));
		}
		return result;
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public void recordAnswer(String userId, String module, String knowledgeId, String angle, boolean correct,
			String stage, boolean firstAttempt) {
		if (userId == null || module == null || knowledgeId == null) {
			return;
		}
		try {
			EnglishLearningState state = stateMapper.selectOne(Wrappers.<EnglishLearningState>lambdaQuery()
					.eq(EnglishLearningState::getUserId, userId)
					.eq(EnglishLearningState::getModule, module)
					.eq(EnglishLearningState::getKnowledgeId, knowledgeId));
			if (state == null) {
				state = new EnglishLearningState();
				state.setUserId(userId);
				state.setModule(module);
				state.setKnowledgeId(knowledgeId);
				state.setState(EnglishStateRule.UNFAMILIAR);
				state.setConsecutiveCorrect(0);
				state.setConsecutiveWrong(0);
				state.setOutputCorrectCount(0);
				state.setMemoryTotal(0);
				state.setMemoryCorrect(0);
				state.setPracticeTotal(0);
				state.setPracticeCorrect(0);
				state.setUseTotal(0);
				state.setUseCorrect(0);
				state.setWeakFlag(0);
			}
			boolean output = EnglishExamAngle.isOutputAngle(angle);
			// 阶段计数：仅首答计入，重做不重复累计
			if (firstAttempt) {
				if (STAGE_MEMORY.equals(stage)) {
					state.setMemoryTotal(nz(state.getMemoryTotal()) + 1);
					state.setMemoryCorrect(nz(state.getMemoryCorrect()) + (correct ? 1 : 0));
				} else if (STAGE_USE.equals(stage)) {
					state.setUseTotal(nz(state.getUseTotal()) + 1);
					state.setUseCorrect(nz(state.getUseCorrect()) + (correct ? 1 : 0));
				} else {
					state.setPracticeTotal(nz(state.getPracticeTotal()) + 1);
					state.setPracticeCorrect(nz(state.getPracticeCorrect()) + (correct ? 1 : 0));
				}
			}
			// 连续正确 / 连续错误互斥累加
			if (correct) {
				state.setConsecutiveCorrect(nz(state.getConsecutiveCorrect()) + 1);
				state.setConsecutiveWrong(0);
				if (output) {
					state.setOutputCorrectCount(nz(state.getOutputCorrectCount()) + 1);
				}
			} else {
				state.setConsecutiveWrong(nz(state.getConsecutiveWrong()) + 1);
				state.setConsecutiveCorrect(0);
			}
			state.setLastAngle(angle);

			int total = nz(state.getMemoryTotal()) + nz(state.getPracticeTotal()) + nz(state.getUseTotal());
			int correctTotal = nz(state.getMemoryCorrect()) + nz(state.getPracticeCorrect())
					+ nz(state.getUseCorrect());
			boolean outputWrong = output && !correct;
			String reason = EnglishStateRule.weakReason(nz(state.getConsecutiveWrong()), total, correctTotal,
					outputWrong);
			// 已有薄弱原因的沿用，避免选择题错一次就把知识点重新拉回薄弱池
			String currentReason = state.getWeakReason();
			boolean wasWeak = nz(state.getWeakFlag()) == 1;
			if (reason != null) {
				state.setWeakFlag(1);
				state.setWeakReason(reason);
			} else if (wasWeak && EnglishStateRule.canBreakthrough(nz(state.getConsecutiveCorrect()),
					nz(state.getOutputCorrectCount()),
					EnglishStateRule.requireOutputForBreakthrough(currentReason))) {
				state.setWeakFlag(0);
				state.setWeakReason(null);
			}
			state.setState(EnglishStateRule.stateOf(total, correctTotal, nz(state.getWeakFlag()) == 1));

			if (state.getId() == null) {
				stateMapper.insert(state);
			} else {
				stateMapper.updateById(state);
			}
			recordAngleUse(userId, module, knowledgeId, angle);
		} catch (Exception e) {
			// 学习状态属于附加统计，写失败不影响答题主流程
			log.warn("记录英语学习状态失败 user={} module={} kp={}", userId, module, knowledgeId, e);
		}
	}

	@Override
	public Map<String, List<String>> angleUsed(String userId, String module, List<String> knowledgeIds) {
		return loadAngleUse(userId, module, knowledgeIds);
	}

	@Override
	public List<EnglishSectionProgressView> sectionProgress(String userId, String version, String grade, String term,
			String unit) {
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit));
		if (words.isEmpty()) {
			return new ArrayList<>();
		}
		return aggregateSections(words, loadStates(userId, List.of(MODULE_WORD)));
	}

	@Override
	public List<EnglishReviewCandidateView> reviewCandidates(String userId, String version, String grade, String term,
			String unit, String module) {
		List<EnglishReviewCandidateView> result = new ArrayList<>();
		if (userId == null) {
			return result;
		}
		// unit 为空时覆盖本册全部单元（由内容表推导，不依赖单元目录表），供复习页做全册复习；
		// 指定单元时严格隔离在该单元内
		String scope = unit == null || unit.isBlank() ? null : unit;
		Date now = new Date();
		if (module == null || MODULE_WORD.equals(module)) {
			collectWordCandidates(userId, version, grade, term, scope, now, result);
		}
		if (module == null || MODULE_SENTENCE.equals(module)) {
			collectSentenceCandidates(userId, version, grade, term, scope, now, result);
		}
		if (module == null || MODULE_GRAMMAR.equals(module)) {
			collectGrammarCandidates(userId, version, grade, term, scope, now, result);
		}
		// 薄弱优先，其次到期，最后一般
		result.sort((a, b) -> {
			int wa = Boolean.TRUE.equals(a.getWeak()) ? 0 : 1;
			int wb = Boolean.TRUE.equals(b.getWeak()) ? 0 : 1;
			if (wa != wb) {
				return wa - wb;
			}
			return String.valueOf(a.getName()).compareTo(String.valueOf(b.getName()));
		});
		return result;
	}

	@Override
	public Map<String, EnglishUnitProgressView> wordPassSummary(String userId, String version, String grade,
			String term) {
		Map<String, EnglishUnitProgressView> result = new LinkedHashMap<>();
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(version != null, EnglishWord::getVersion, version)
				.eq(grade != null, EnglishWord::getGrade, grade)
				.eq(term != null, EnglishWord::getTerm, term));
		if (words.isEmpty()) {
			return result;
		}
		Map<String, EnglishLearningStateView> states = loadStates(userId, List.of(MODULE_WORD));
		Map<String, List<EnglishWord>> byUnit = new LinkedHashMap<>();
		for (EnglishWord word : words) {
			byUnit.computeIfAbsent(word.getUnit(), k -> new ArrayList<>()).add(word);
		}
		for (Map.Entry<String, List<EnglishWord>> entry : byUnit.entrySet()) {
			List<EnglishSectionProgressView> sections = aggregateSections(entry.getValue(), states);
			int passed = 0;
			for (EnglishSectionProgressView section : sections) {
				if (Boolean.TRUE.equals(section.getSectionPass())) {
					passed++;
				}
			}
			EnglishUnitProgressView view = new EnglishUnitProgressView();
			view.setUnit(entry.getKey());
			view.setSections(sections);
			view.setWordPass(!sections.isEmpty()
					&& sections.stream().allMatch(s -> Boolean.TRUE.equals(s.getSectionPass())));
			view.setWordPassRate(EnglishStateRule.percent(passed, sections.size()));
			result.put(entry.getKey(), view);
		}
		return result;
	}

	/**
	 * 按小节聚合三阶进度（纯内存计算，调用方负责传入单词与学习状态）。
	 */
	private List<EnglishSectionProgressView> aggregateSections(List<EnglishWord> words,
			Map<String, EnglishLearningStateView> states) {
		Map<String, List<EnglishWord>> grouped = new LinkedHashMap<>();
		List<EnglishWord> unsectioned = new ArrayList<>();
		for (EnglishWord word : words) {
			String section = word.getSection() == null ? "" : word.getSection().trim();
			if (section.isEmpty()) {
				unsectioned.add(word);
				continue;
			}
			grouped.computeIfAbsent(section, k -> new ArrayList<>()).add(word);
		}
		if (!unsectioned.isEmpty()) {
			grouped.put(UNSECTIONED, unsectioned);
		}
		List<EnglishSectionProgressView> result = new ArrayList<>();
		for (Map.Entry<String, List<EnglishWord>> entry : grouped.entrySet()) {
			EnglishSectionProgressView view = new EnglishSectionProgressView();
			view.setSection(entry.getKey());
			view.setWordCount(entry.getValue().size());
			int memoryDone = 0;
			int memoryCorrect = 0;
			int practiceDone = 0;
			int practiceCorrect = 0;
			int useDone = 0;
			int useCorrect = 0;
			int familiar = 0;
			int ordinary = 0;
			int unfamiliar = 0;
			for (EnglishWord word : entry.getValue()) {
				EnglishLearningStateView state = states.get(key(MODULE_WORD, word.getId()));
				if (state == null) {
					unfamiliar++;
					continue;
				}
				memoryDone += nz(state.getMemoryTotal());
				memoryCorrect += nz(state.getMemoryCorrect());
				practiceDone += nz(state.getPracticeTotal());
				practiceCorrect += nz(state.getPracticeCorrect());
				useDone += nz(state.getUseTotal());
				useCorrect += nz(state.getUseCorrect());
				String stateValue = state.getState() == null ? EnglishStateRule.UNFAMILIAR : state.getState();
				if (EnglishStateRule.FAMILIAR.equals(stateValue)) {
					familiar++;
				} else if (EnglishStateRule.ORDINARY.equals(stateValue)) {
					ordinary++;
				} else {
					unfamiliar++;
				}
			}
			view.setMemoryDone(memoryDone);
			view.setMemoryCorrect(memoryCorrect);
			view.setMemoryRate(EnglishStateRule.percent(memoryCorrect, memoryDone));
			view.setMemoryPass(memoryDone > 0
					&& view.getMemoryRate() >= EnglishMastery.WORD_MEMORY_PASS_RATE
					&& unfamiliar == 0);
			view.setPracticeDone(practiceDone);
			view.setPracticeCorrect(practiceCorrect);
			view.setPracticeRate(EnglishStateRule.percent(practiceCorrect, practiceDone));
			view.setPracticePass(practiceDone > 0 && view.getPracticeRate() >= EnglishMastery.WORD_PRACTICE_PASS_RATE);
			view.setUseDone(useDone);
			view.setUseCorrect(useCorrect);
			view.setUseRate(EnglishStateRule.percent(useCorrect, useDone));
			view.setUsePass(useDone > 0 && view.getUseRate() >= EnglishMastery.WORD_USE_PASS_RATE);
			view.setFamiliarCount(familiar);
			view.setOrdinaryCount(ordinary);
			view.setUnfamiliarCount(unfamiliar);
			view.setSectionPass(Boolean.TRUE.equals(view.getMemoryPass())
					&& Boolean.TRUE.equals(view.getPracticePass())
					&& Boolean.TRUE.equals(view.getUsePass()));
			result.add(view);
		}
		result.sort((a, b) -> {
			boolean aUn = UNSECTIONED.equals(a.getSection());
			boolean bUn = UNSECTIONED.equals(b.getSection());
			if (aUn != bUn) {
				return aUn ? 1 : -1;
			}
			return 0;
		});
		return result;
	}

	@Override
	public EnglishUnitProgressView unitPass(String userId, String version, String grade, String term, String unit) {
		EnglishUnitProgressView view = new EnglishUnitProgressView();
		view.setUnit(unit);
		List<EnglishUnit> units = unitMapper.selectList(Wrappers.<EnglishUnit>lambdaQuery()
				.eq(EnglishUnit::getVersion, version)
				.eq(EnglishUnit::getGrade, grade)
				.eq(EnglishUnit::getTerm, term)
				.eq(EnglishUnit::getUnit, unit));
		if (!units.isEmpty()) {
			view.setSort(units.get(0).getSort());
		}
		view.setWordCount(wordMapper.selectCount(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(EnglishWord::getUnit, unit)).intValue());
		view.setSentenceCount(sentenceMapper.selectCount(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(EnglishSentence::getUnit, unit)).intValue());
		view.setGrammarCount(grammarMapper.selectCount(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(EnglishGrammar::getVersion, version)
				.eq(EnglishGrammar::getGrade, grade)
				.eq(EnglishGrammar::getTerm, term)
				.eq(EnglishGrammar::getUnit, unit)).intValue());
		List<EnglishSectionProgressView> sections = sectionProgress(userId, version, grade, term, unit);
		view.setSections(sections);
		view.setWordPass(!sections.isEmpty() && sections.stream().allMatch(s -> Boolean.TRUE.equals(s.getSectionPass())));
		int passSections = 0;
		for (EnglishSectionProgressView section : sections) {
			if (Boolean.TRUE.equals(section.getSectionPass())) {
				passSections++;
			}
		}
		view.setWordPassRate(EnglishStateRule.percent(passSections, sections.size()));
		List<EnglishReviewCandidateView> candidates = reviewCandidates(userId, version, grade, term, unit, null);
		view.setReviewDue(candidates.size());
		int dueWord = 0;
		int dueSentence = 0;
		int dueGrammar = 0;
		for (EnglishReviewCandidateView candidate : candidates) {
			if (MODULE_SENTENCE.equals(candidate.getModule())) {
				dueSentence++;
			} else if (MODULE_GRAMMAR.equals(candidate.getModule())) {
				dueGrammar++;
			} else {
				dueWord++;
			}
		}
		view.setReviewDueWord(dueWord);
		view.setReviewDueSentence(dueSentence);
		view.setReviewDueGrammar(dueGrammar);
		view.setSentencePassRate(sentencePassRate(userId, version, grade, term, unit));
		return view;
	}

	/**
	 * 句式通关率：句子本熟练度 >= 3 的比例。
	 *
	 * <p>句式按单元整体判定，不再细分小节；分子只统计已进入句子本的句子。</p>
	 */
	private Integer sentencePassRate(String userId, String version, String grade, String term, String unit) {
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(unit != null, EnglishSentence::getUnit, unit));
		if (sentences.isEmpty()) {
			return 0;
		}
		List<String> ids = new ArrayList<>();
		for (EnglishSentence sentence : sentences) {
			ids.add(sentence.getId());
		}
		int passed = 0;
		for (EnglishSentenceBook book : sentenceBookMapper.selectList(Wrappers.<EnglishSentenceBook>lambdaQuery()
				.eq(EnglishSentenceBook::getUserId, userId)
				.in(EnglishSentenceBook::getSentenceId, ids))) {
			if (nz(book.getFamiliarity()) >= 3) {
				passed++;
			}
		}
		return EnglishStateRule.percent(passed, sentences.size());
	}

	private void collectWordCandidates(String userId, String version, String grade, String term, String unit,
			Date now, List<EnglishReviewCandidateView> result) {
		List<EnglishWord> words = wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
				.eq(EnglishWord::getVersion, version)
				.eq(EnglishWord::getGrade, grade)
				.eq(EnglishWord::getTerm, term)
				.eq(unit != null, EnglishWord::getUnit, unit));
		if (words.isEmpty()) {
			return;
		}
		Map<String, EnglishLearningStateView> states = loadStates(userId, List.of(MODULE_WORD));
		Map<String, EnglishWordBook> books = new LinkedHashMap<>();
		List<String> wordIds = new ArrayList<>();
		for (EnglishWord word : words) {
			wordIds.add(word.getId());
		}
		for (EnglishWordBook book : wordBookMapper.selectList(Wrappers.<EnglishWordBook>lambdaQuery()
				.eq(EnglishWordBook::getUserId, userId)
				.in(EnglishWordBook::getWordId, wordIds))) {
			books.put(book.getWordId(), book);
		}
		Map<String, List<String>> usedAngles = loadAngleUse(userId, MODULE_WORD, wordIds);
		for (EnglishWord word : words) {
			EnglishLearningStateView state = states.get(key(MODULE_WORD, word.getId()));
			EnglishWordBook book = books.get(word.getId());
			List<String> reasons = new ArrayList<>();
			if (state != null && Boolean.TRUE.equals(state.getWeak())) {
				reasons.add("weak");
			}
			if (book != null && nz(book.getWrongCount()) > 0) {
				reasons.add("wrong");
			}
			if (book != null && isDue(book.getNextReviewTime(), now)) {
				reasons.add("due");
			}
			if (reasons.isEmpty()) {
				continue;
			}
			EnglishReviewCandidateView view = new EnglishReviewCandidateView();
			view.setModule(MODULE_WORD);
			view.setKnowledgeId(word.getId());
			view.setName(word.getSpell());
			view.setUnit(word.getUnit());
			view.setSection(word.getSection());
			view.setState(state == null ? EnglishStateRule.UNFAMILIAR : state.getState());
			view.setStateLabel(EnglishMastery.stateLabel(view.getState()));
			view.setWeak(state != null && Boolean.TRUE.equals(state.getWeak()));
			view.setWeakReason(state == null ? null : state.getWeakReason());
			view.setReasons(reasons);
			List<String> angles = EnglishExamAngle.anglesOf(MODULE_WORD);
			String next = EnglishExamAngle.nextAngle(MODULE_WORD, usedAngles.get(word.getId()));
			view.setAngles(orderedByRecommend(angles, next));
			view.setNextAngleLabel(EnglishExamAngle.labelOf(next));
			result.add(view);
		}
	}

	private void collectSentenceCandidates(String userId, String version, String grade, String term, String unit,
			Date now, List<EnglishReviewCandidateView> result) {
		List<EnglishSentence> sentences = sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
				.eq(EnglishSentence::getVersion, version)
				.eq(EnglishSentence::getGrade, grade)
				.eq(EnglishSentence::getTerm, term)
				.eq(unit != null, EnglishSentence::getUnit, unit));
		if (sentences.isEmpty()) {
			return;
		}
		Map<String, EnglishLearningStateView> states = loadStates(userId, List.of(MODULE_SENTENCE));
		Map<String, EnglishSentenceBook> books = new LinkedHashMap<>();
		List<String> ids = new ArrayList<>();
		for (EnglishSentence sentence : sentences) {
			ids.add(sentence.getId());
		}
		for (EnglishSentenceBook book : sentenceBookMapper.selectList(Wrappers.<EnglishSentenceBook>lambdaQuery()
				.eq(EnglishSentenceBook::getUserId, userId)
				.in(EnglishSentenceBook::getSentenceId, ids))) {
			books.put(book.getSentenceId(), book);
		}
		Map<String, List<String>> usedAngles = loadAngleUse(userId, MODULE_SENTENCE, ids);
		for (EnglishSentence sentence : sentences) {
			EnglishLearningStateView state = states.get(key(MODULE_SENTENCE, sentence.getId()));
			EnglishSentenceBook book = books.get(sentence.getId());
			List<String> reasons = new ArrayList<>();
			if (state != null && Boolean.TRUE.equals(state.getWeak())) {
				reasons.add("weak");
			}
			if (book != null && nz(book.getWrongCount()) > 0) {
				reasons.add("wrong");
			}
			if (book != null && isDue(book.getNextReviewTime(), now)) {
				reasons.add("due");
			}
			if (reasons.isEmpty()) {
				continue;
			}
			EnglishReviewCandidateView view = new EnglishReviewCandidateView();
			view.setModule(MODULE_SENTENCE);
			view.setKnowledgeId(sentence.getId());
			view.setName(sentence.getEn());
			view.setUnit(sentence.getUnit());
			view.setSection(sentence.getSection());
			view.setState(state == null ? EnglishStateRule.UNFAMILIAR : state.getState());
			view.setStateLabel(EnglishMastery.stateLabel(view.getState()));
			view.setWeak(state != null && Boolean.TRUE.equals(state.getWeak()));
			view.setWeakReason(state == null ? null : state.getWeakReason());
			view.setReasons(reasons);
			List<String> angles = EnglishExamAngle.anglesOf(MODULE_SENTENCE);
			String next = EnglishExamAngle.nextAngle(MODULE_SENTENCE, usedAngles.get(sentence.getId()));
			view.setAngles(orderedByRecommend(angles, next));
			view.setNextAngleLabel(EnglishExamAngle.labelOf(next));
			result.add(view);
		}
	}

	private void collectGrammarCandidates(String userId, String version, String grade, String term, String unit,
			Date now, List<EnglishReviewCandidateView> result) {
		List<EnglishGrammar> grammars = grammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
				.eq(EnglishGrammar::getVersion, version)
				.eq(EnglishGrammar::getGrade, grade)
				.eq(EnglishGrammar::getTerm, term)
				.eq(unit != null, EnglishGrammar::getUnit, unit));
		if (grammars.isEmpty()) {
			return;
		}
		Map<String, EnglishLearningStateView> states = loadStates(userId, List.of(MODULE_GRAMMAR));
		Map<String, EnglishGrammarBook> books = new LinkedHashMap<>();
		List<String> ids = new ArrayList<>();
		for (EnglishGrammar grammar : grammars) {
			ids.add(grammar.getId());
		}
		for (EnglishGrammarBook book : grammarBookMapper.selectList(Wrappers.<EnglishGrammarBook>lambdaQuery()
				.eq(EnglishGrammarBook::getUserId, userId)
				.in(EnglishGrammarBook::getGrammarId, ids))) {
			books.put(book.getGrammarId(), book);
		}
		Map<String, List<String>> usedAngles = loadAngleUse(userId, MODULE_GRAMMAR, ids);
		for (EnglishGrammar grammar : grammars) {
			EnglishLearningStateView state = states.get(key(MODULE_GRAMMAR, grammar.getId()));
			EnglishGrammarBook book = books.get(grammar.getId());
			List<String> reasons = new ArrayList<>();
			if (state != null && Boolean.TRUE.equals(state.getWeak())) {
				reasons.add("weak");
			}
			if (book != null && nz(book.getWrongCount()) > 0) {
				reasons.add("wrong");
			}
			if (book != null && isDue(book.getNextReviewTime(), now)) {
				reasons.add("due");
			}
			if (reasons.isEmpty()) {
				continue;
			}
			EnglishReviewCandidateView view = new EnglishReviewCandidateView();
			view.setModule(MODULE_GRAMMAR);
			view.setKnowledgeId(grammar.getId());
			view.setName(grammar.getTitle());
			view.setUnit(grammar.getUnit());
			view.setSection(grammar.getSection());
			view.setState(state == null ? EnglishStateRule.UNFAMILIAR : state.getState());
			view.setStateLabel(EnglishMastery.stateLabel(view.getState()));
			view.setWeak(state != null && Boolean.TRUE.equals(state.getWeak()));
			view.setWeakReason(state == null ? null : state.getWeakReason());
			view.setReasons(reasons);
			List<String> angles = EnglishExamAngle.anglesOf(MODULE_GRAMMAR);
			String next = EnglishExamAngle.nextAngle(MODULE_GRAMMAR, usedAngles.get(grammar.getId()));
			view.setAngles(orderedByRecommend(angles, next));
			view.setNextAngleLabel(EnglishExamAngle.labelOf(next));
			result.add(view);
		}
	}

	private Map<String, List<String>> loadAngleUse(String userId, String module, List<String> knowledgeIds) {
		Map<String, List<String>> result = new LinkedHashMap<>();
		if (knowledgeIds.isEmpty()) {
			return result;
		}
		List<EnglishExamAngleUse> uses = angleUseMapper.selectList(Wrappers.<EnglishExamAngleUse>lambdaQuery()
				.eq(EnglishExamAngleUse::getUserId, userId)
				.eq(EnglishExamAngleUse::getModule, module)
				.in(EnglishExamAngleUse::getKnowledgeId, knowledgeIds)
				.orderByAsc(EnglishExamAngleUse::getLastUseAt));
		for (EnglishExamAngleUse use : uses) {
			result.computeIfAbsent(use.getKnowledgeId(), k -> new ArrayList<>()).add(use.getAngle());
		}
		return result;
	}

	private void recordAngleUse(String userId, String module, String knowledgeId, String angle) {
		if (angle == null) {
			return;
		}
		EnglishExamAngleUse use = angleUseMapper.selectOne(Wrappers.<EnglishExamAngleUse>lambdaQuery()
				.eq(EnglishExamAngleUse::getUserId, userId)
				.eq(EnglishExamAngleUse::getModule, module)
				.eq(EnglishExamAngleUse::getKnowledgeId, knowledgeId)
				.eq(EnglishExamAngleUse::getAngle, angle));
		if (use == null) {
			use = new EnglishExamAngleUse();
			use.setUserId(userId);
			use.setModule(module);
			use.setKnowledgeId(knowledgeId);
			use.setAngle(angle);
			use.setUseCount(1);
			use.setLastUseAt(new Date());
			angleUseMapper.insert(use);
			return;
		}
		use.setUseCount(nz(use.getUseCount()) + 1);
		use.setLastUseAt(new Date());
		angleUseMapper.updateById(use);
	}

	private List<String> orderedByRecommend(List<String> angles, String next) {
		List<String> result = new ArrayList<>();
		if (next != null && angles.contains(next)) {
			result.add(next);
		}
		for (String angle : angles) {
			if (!result.contains(angle)) {
				result.add(angle);
			}
		}
		return result;
	}

	private boolean isDue(Date nextReviewTime, Date now) {
		return nextReviewTime != null && !nextReviewTime.after(now);
	}

	private static String key(String module, String knowledgeId) {
		return module + "|" + knowledgeId;
	}

	private static int nz(Integer value) {
		return value == null ? 0 : value;
	}

	private EnglishLearningStateView toView(EnglishLearningState state) {
		EnglishLearningStateView view = new EnglishLearningStateView();
		view.setModule(state.getModule());
		view.setKnowledgeId(state.getKnowledgeId());
		view.setState(state.getState());
		view.setConsecutiveCorrect(state.getConsecutiveCorrect());
		view.setConsecutiveWrong(state.getConsecutiveWrong());
		view.setOutputCorrectCount(state.getOutputCorrectCount());
		view.setLastAngle(state.getLastAngle());
		view.setWeak(state.getWeakFlag() != null && state.getWeakFlag() == 1);
		view.setWeakReason(state.getWeakReason());
		view.setMemoryTotal(state.getMemoryTotal());
		view.setMemoryCorrect(state.getMemoryCorrect());
		view.setPracticeTotal(state.getPracticeTotal());
		view.setPracticeCorrect(state.getPracticeCorrect());
		view.setUseTotal(state.getUseTotal());
		view.setUseCorrect(state.getUseCorrect());
		return view;
	}
}
