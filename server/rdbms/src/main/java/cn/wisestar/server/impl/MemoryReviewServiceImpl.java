package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.ReviewConsts;
import cn.wisestar.server.domain.dto.review.ReviewAnswerCommand;
import cn.wisestar.server.domain.dto.review.ReviewAnswerResult;
import cn.wisestar.server.domain.dto.review.ReviewForecastView;
import cn.wisestar.server.domain.dto.review.ReviewPlanView;
import cn.wisestar.server.domain.dto.review.ReviewQueueItemView;
import cn.wisestar.server.domain.dto.review.ReviewQueueRequest;
import cn.wisestar.server.domain.dto.review.WeakBoardItemView;
import cn.wisestar.server.domain.dto.review.WeakSessionRequest;
import cn.wisestar.server.domain.model.EnglishGrammar;
import cn.wisestar.server.domain.model.EnglishGrammarBook;
import cn.wisestar.server.domain.model.EnglishLearningState;
import cn.wisestar.server.domain.model.EnglishSentence;
import cn.wisestar.server.domain.model.EnglishSentenceBook;
import cn.wisestar.server.domain.model.EnglishWord;
import cn.wisestar.server.domain.model.EnglishWordBook;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.MemoryReviewLog;
import cn.wisestar.server.domain.model.MemorySchedule;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.domain.model.WeakPointEvent;
import cn.wisestar.server.event.WeakPointEventRecorder;
import cn.wisestar.server.impl.fsrs.Fsrs5;
import cn.wisestar.server.impl.fsrs.ReviewDemand;
import cn.wisestar.server.impl.fsrs.ReviewGrade;
import cn.wisestar.server.impl.fsrs.Sm2ColdStart;
import cn.wisestar.server.mapper.EnglishGrammarBookMapper;
import cn.wisestar.server.mapper.EnglishGrammarMapper;
import cn.wisestar.server.mapper.EnglishLearningStateMapper;
import cn.wisestar.server.mapper.EnglishSentenceBookMapper;
import cn.wisestar.server.mapper.EnglishSentenceMapper;
import cn.wisestar.server.mapper.EnglishWordBookMapper;
import cn.wisestar.server.mapper.EnglishWordMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.MemoryReviewLogMapper;
import cn.wisestar.server.mapper.MemoryScheduleMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.MemoryReviewService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 记忆调度（遗忘曲线）服务实现。
 *
 * <p>用 FSRS-5 维护难度、稳定度与到期时间，历史不足 2 次时用 SM-2 冷启动；
 * 负责复习计划、队列、预测、薄弱看板与攻克训练，并把结果回写旧表，保证既有页面行为不变。</p>
 *
 * <p>线程模型：所有写入在调用方事务内完成；调度计算异常只记日志，不阻断答题主流程。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Slf4j
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class MemoryReviewServiceImpl implements MemoryReviewService {

	/** 目标保持率 */
	private static final double TARGET_RETENTION = 0.9;

	/** 攻克所需最低稳定度（天） */
	private static final double CONQUER_STABILITY_DAYS = 7.0;

	/** 遗忘阈值：可提取度降到该值视为已遗忘 */
	private static final double FORGETTING_RETRIEVABILITY = 0.6;

	/** 新项配额比例 */
	private static final double FRESH_QUOTA_RATIO = 0.3;

	/** 复习队列默认上限 */
	private static final int DEFAULT_LIMIT = 20;

	/** 复习队列最大上限 */
	private static final int MAX_LIMIT = 100;

	/** 攻克训练默认题量 */
	private static final int DEFAULT_SESSION_SIZE = 8;

	/** 攻克训练最小题量 */
	private static final int MIN_SESSION_SIZE = 6;

	/** 攻克训练最大题量 */
	private static final int MAX_SESSION_SIZE = 10;

	/** 攻克训练输出题占比下限 */
	private static final double OUTPUT_QUOTA_RATIO = 0.4;

	/** 知识点级计入调度的最少会话题量：一两道题的会话噪声太大 */
	private static final int MIN_KP_SESSION_QUESTIONS = 3;

	private final MemoryScheduleMapper scheduleMapper;

	private final MemoryReviewLogMapper logMapper;

	private final EnglishWordMapper wordMapper;

	private final EnglishWordBookMapper wordBookMapper;

	private final EnglishSentenceMapper sentenceMapper;

	private final EnglishSentenceBookMapper sentenceBookMapper;

	private final EnglishGrammarMapper grammarMapper;

	private final EnglishGrammarBookMapper grammarBookMapper;

	private final EnglishLearningStateMapper learningStateMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final UserKnowledgeProgressMapper progressMapper;

	private final UserWeakKnowledgeMapper weakMapper;

	private final WeakPointEventRecorder weakPointEventRecorder;

	@Override
	public ReviewAnswerResult recordAnswer(String userId, ReviewAnswerCommand command) {
		if (!StringUtils.hasText(userId) || command == null || !StringUtils.hasText(command.getKnowledgeId())
				|| !StringUtils.hasText(command.getModule())) {
			return null;
		}
		try {
			return apply(userId, command);
		}
		catch (Exception e) {
			log.warn("记忆调度计算失败，已跳过 user={} module={} kp={}", userId, command.getModule(),
					command.getKnowledgeId(), e);
			return null;
		}
	}

	@Override
	public ReviewAnswerResult recordKpPractice(String userId, String subjectId, String knowledgePointId,
			String knowledgeName, int correctRate, int total) {
		if (total < MIN_KP_SESSION_QUESTIONS) {
			return null;
		}
		ReviewAnswerCommand command = new ReviewAnswerCommand();
		command.setModule(ReviewConsts.MODULE_KP);
		command.setSubjectId(subjectId);
		command.setKnowledgeId(knowledgePointId);
		command.setKnowledgeName(knowledgeName);
		command.setDemand(ReviewConsts.DEMAND_CUED);
		command.setCorrect(correctRate >= 60);
		command.setCorrectRate(correctRate);
		command.setTotal(total);
		command.setFirstAttempt(true);
		return recordAnswer(userId, command);
	}

	@Override
	public ReviewPlanView plan(String userId, String subjectId) {
		ReviewPlanView view = new ReviewPlanView();
		view.setTargetRetention(TARGET_RETENTION);
		List<MemorySchedule> all = schedules(userId, null, subjectId);
		Date now = new Date();
		long dayMillis = 24L * 60 * 60 * 1000;
		Map<String, ReviewPlanView.ModuleStat> stats = new LinkedHashMap<>();
		for (String module : ReviewConsts.MODULES) {
			ReviewPlanView.ModuleStat stat = new ReviewPlanView.ModuleStat();
			stat.setModule(module);
			stats.put(module, stat);
		}
		int due = 0;
		int overdue = 0;
		int weak = 0;
		int fresh = 0;
		for (MemorySchedule schedule : all) {
			ReviewPlanView.ModuleStat stat = stats.computeIfAbsent(schedule.getModule(), key -> {
				ReviewPlanView.ModuleStat created = new ReviewPlanView.ModuleStat();
				created.setModule(key);
				return created;
			});
			boolean isFresh = nz(schedule.getReps()) == 0;
			boolean isDue = !isFresh && schedule.getDueAt() != null && !schedule.getDueAt().after(now);
			boolean isOverdue = !isFresh && schedule.getDueAt() != null
					&& schedule.getDueAt().getTime() < now.getTime() - dayMillis;
			boolean isWeak = !isFresh && nz(schedule.getWeakFlag()) == 1;
			if (isDue) {
				due++;
				stat.setDue(stat.getDue() + 1);
			}
			if (isOverdue) {
				overdue++;
				stat.setOverdue(stat.getOverdue() + 1);
			}
			if (isWeak) {
				weak++;
				stat.setWeak(stat.getWeak() + 1);
			}
			if (isFresh) {
				fresh++;
				stat.setFresh(stat.getFresh() + 1);
			}
		}
		view.setDue(due);
		view.setOverdue(overdue);
		view.setWeak(weak);
		view.setFresh(fresh);
		view.getModules().addAll(stats.values());
		int minutes = Math.round(due * 0.6f + weak * 0.5f + fresh * 0.4f);
		view.setEstimatedMinutes(due + weak + fresh == 0 ? 0 : Math.max(1, minutes));
		return view;
	}

	@Override
	public List<ReviewQueueItemView> queue(String userId, ReviewQueueRequest request) {
		ReviewQueueRequest query = request == null ? new ReviewQueueRequest() : request;
		int limit = query.getLimit() == null ? DEFAULT_LIMIT : Math.max(1, Math.min(query.getLimit(), MAX_LIMIT));
		Date now = new Date();
		List<String> modes = query.getModes() == null ? Collections.emptyList() : query.getModes();
		boolean allowDue = modes.isEmpty() || modes.contains(ReviewConsts.SOURCE_DUE);
		boolean allowWeak = modes.isEmpty() || modes.contains(ReviewConsts.SOURCE_WEAK);
		boolean allowNew = modes.isEmpty() || modes.contains(ReviewConsts.SOURCE_NEW);

		List<MemorySchedule> all = filteredSchedules(userId, query);
		List<MemorySchedule> due = new ArrayList<>();
		List<MemorySchedule> weak = new ArrayList<>();
		List<MemorySchedule> fresh = new ArrayList<>();
		for (MemorySchedule schedule : all) {
			boolean isDue = schedule.getDueAt() != null && !schedule.getDueAt().after(now);
			boolean isWeak = nz(schedule.getWeakFlag()) == 1;
			boolean isFresh = nz(schedule.getReps()) == 0;
			if (isFresh) {
				if (allowNew) {
					fresh.add(schedule);
				}
				continue;
			}
			if (isDue && allowDue) {
				due.add(schedule);
			}
			else if (isWeak && allowWeak) {
				weak.add(schedule);
			}
		}
		List<MemorySchedule> merged = new ArrayList<>(due);
		merged.addAll(weak);
		merged.sort(Comparator
				.comparingDouble((MemorySchedule schedule) -> -severity(schedule, now))
				.thenComparing(schedule -> schedule.getDueAt() == null ? new Date(0) : schedule.getDueAt())
				.thenComparingDouble(schedule -> nzDouble(schedule.getStability())));
		// 到期/薄弱至少占 1 席，新项上限为 limit 的 30%（到期不足时再补新项，但不突破上限）
		int freshCap = allowNew ? Math.max(1, (int) Math.round(limit * FRESH_QUOTA_RATIO)) : 0;
		int regularQuota = merged.isEmpty() ? 0 : Math.min(merged.size(), Math.min(limit, Math.max(1, limit - freshCap)));
		int freshQuota = allowNew ? Math.min(fresh.size(), Math.min(freshCap, Math.max(0, limit - regularQuota))) : 0;
		List<MemorySchedule> picked = new ArrayList<>();
		Set<String> seen = new LinkedHashSet<>();
		for (MemorySchedule schedule : merged) {
			if (picked.size() >= regularQuota) {
				break;
			}
			if (seen.add(schedule.getId())) {
				picked.add(schedule);
			}
		}
		if (allowNew) {
			fresh.sort(Comparator.comparing(schedule -> schedule.getDueAt() == null ? now : schedule.getDueAt()));
			int added = 0;
			for (MemorySchedule schedule : fresh) {
				if (added >= freshQuota || picked.size() >= limit) {
					break;
				}
				if (seen.add(schedule.getId())) {
					picked.add(schedule);
					added++;
				}
			}
		}
		return toQueueViews(picked, now, true);
	}

	@Override
	public ReviewForecastView forecast(String userId, String subjectId, int days) {
		ReviewForecastView view = new ReviewForecastView();
		view.setTargetRetention(TARGET_RETENTION);
		List<MemorySchedule> all = schedules(userId, null, subjectId);
		view.setTotal(all.size());
		Date now = new Date();
		int horizon = Math.max(1, Math.min(days, 90));
		Map<String, Integer> counts = new LinkedHashMap<>();
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		LocalDate today = LocalDate.now();
		for (int i = 0; i < horizon; i++) {
			counts.put(format.format(java.sql.Date.valueOf(today.plusDays(i))), 0);
		}
		double sum = 0;
		int dueToday = 0;
		for (MemorySchedule schedule : all) {
			sum += retrievability(schedule, now);
			if (schedule.getDueAt() != null && !schedule.getDueAt().after(now)) {
				dueToday++;
			}
			Date due = schedule.getDueAt() == null ? now : schedule.getDueAt();
			String key = format.format(due);
			if (counts.containsKey(key)) {
				counts.put(key, counts.get(key) + 1);
			}
		}
		view.setDueToday(dueToday);
		view.setAverageRetrievability(all.isEmpty() ? 0 : sum / all.size());
		counts.forEach((date, due) -> {
			ReviewForecastView.DayPoint point = new ReviewForecastView.DayPoint();
			point.setDate(date);
			point.setDue(due);
			view.getDays().add(point);
		});
		return view;
	}

	@Override
	public List<WeakBoardItemView> weakBoard(String userId, String subjectId, int limit) {
		int cap = Math.max(1, Math.min(limit, MAX_LIMIT));
		Date now = new Date();
		List<MemorySchedule> weak = schedules(userId, null, subjectId).stream()
				.filter(schedule -> nz(schedule.getWeakFlag()) == 1)
				.sorted(Comparator.comparingDouble((MemorySchedule schedule) -> -severity(schedule, now)))
				.collect(Collectors.toList());
		List<MemorySchedule> top = weak.stream().limit(cap).collect(Collectors.toList());
		Map<String, String> names = names(top);
		List<WeakBoardItemView> result = new ArrayList<>();
		for (MemorySchedule schedule : top) {
			WeakBoardItemView view = new WeakBoardItemView();
			fillBase(view, schedule, now, names);
			view.setSeverity(round3(severity(schedule, now)));
			view.setRetrievability(round3(retrievability(schedule, now)));
			view.setMastery(mastery(schedule, now));
			view.setForgettingAt(forgettingAt(schedule));
			view.setAngle(suggestedAngle(schedule));
			view.setDemand(suggestedDemand(schedule));
			result.add(view);
		}
		return result;
	}

	@Override
	public List<ReviewQueueItemView> weakSession(String userId, WeakSessionRequest request) {
		WeakSessionRequest query = request == null ? new WeakSessionRequest() : request;
		int size = query.getSize() == null ? DEFAULT_SESSION_SIZE
				: Math.max(MIN_SESSION_SIZE, Math.min(query.getSize(), MAX_SESSION_SIZE));
		ReviewQueueRequest scope = new ReviewQueueRequest();
		scope.setSubjectId(query.getSubjectId());
		scope.setModule(query.getModule());
		scope.setVersion(query.getVersion());
		scope.setGrade(query.getGrade());
		scope.setTerm(query.getTerm());
		scope.setUnit(query.getUnit());
		Date now = new Date();
		List<MemorySchedule> weak = filteredSchedules(userId, scope).stream()
				.filter(schedule -> nz(schedule.getWeakFlag()) == 1)
				.sorted(Comparator.comparingDouble((MemorySchedule schedule) -> -severity(schedule, now)))
				.collect(Collectors.toList());
		List<MemorySchedule> picked = new ArrayList<>();
		int outputQuota = Math.max(1, (int) Math.ceil(size * OUTPUT_QUOTA_RATIO));
		for (MemorySchedule schedule : weak) {
			if (outputCount(picked) >= outputQuota || picked.size() >= size) {
				break;
			}
			if (ReviewDemand.isOutput(suggestedDemand(schedule))) {
				picked.add(schedule);
			}
		}
		for (MemorySchedule schedule : weak) {
			if (picked.size() >= size) {
				break;
			}
			if (!picked.contains(schedule)) {
				picked.add(schedule);
			}
		}
		picked.sort(Comparator.comparingDouble((MemorySchedule schedule) -> -severity(schedule, now)));
		return toQueueViews(picked, now, false);
	}

	@Override
	public int backfill(String userId) {
		if (!StringUtils.hasText(userId)) {
			return 0;
		}
		Set<String> existing = scheduleMapper.selectList(Wrappers.<MemorySchedule>lambdaQuery()
						.eq(MemorySchedule::getUserId, userId))
				.stream().map(MemoryReviewServiceImpl::key).collect(Collectors.toSet());
		Date now = new Date();
		Map<String, String> englishWeak = new HashMap<>();
		for (EnglishLearningState state : learningStateMapper.selectList(Wrappers.<EnglishLearningState>lambdaQuery()
				.eq(EnglishLearningState::getUserId, userId)
				.eq(EnglishLearningState::getWeakFlag, 1))) {
			englishWeak.put(state.getModule() + ":" + state.getKnowledgeId(),
					state.getWeakReason() == null ? ReviewConsts.REASON_LOW_RETRIEVABILITY : state.getWeakReason());
		}
		Map<String, Date> kpWeak = new HashMap<>();
		for (UserWeakKnowledge weak : weakMapper.selectList(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getStatus, "active"))) {
			kpWeak.put(weak.getKnowledgePointId(),
					weak.getFirstWeakAt() == null ? now : weak.getFirstWeakAt());
		}
		int inserted = 0;
		for (EnglishWordBook book : wordBookMapper.selectList(Wrappers.<EnglishWordBook>lambdaQuery()
				.eq(EnglishWordBook::getUserId, userId))) {
			String weakReason = englishWeak.get(ReviewConsts.MODULE_WORD + ":" + book.getWordId());
			inserted += insertLegacy(userId, ReviewConsts.MODULE_WORD, null, book.getWordId(),
					book.getFamiliarity(), book.getNextReviewTime(), nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getWrongCount()), null, nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getCorrectCount()), weakReason != null, weakReason, null, existing, now);
		}
		for (EnglishSentenceBook book : sentenceBookMapper.selectList(Wrappers.<EnglishSentenceBook>lambdaQuery()
				.eq(EnglishSentenceBook::getUserId, userId))) {
			String weakReason = englishWeak.get(ReviewConsts.MODULE_SENTENCE + ":" + book.getSentenceId());
			inserted += insertLegacy(userId, ReviewConsts.MODULE_SENTENCE, null, book.getSentenceId(),
					book.getFamiliarity(), book.getNextReviewTime(), nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getWrongCount()), null, nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getCorrectCount()), weakReason != null, weakReason, null, existing, now);
		}
		for (EnglishGrammarBook book : grammarBookMapper.selectList(Wrappers.<EnglishGrammarBook>lambdaQuery()
				.eq(EnglishGrammarBook::getUserId, userId))) {
			String weakReason = englishWeak.get(ReviewConsts.MODULE_GRAMMAR + ":" + book.getGrammarId());
			inserted += insertLegacy(userId, ReviewConsts.MODULE_GRAMMAR, null, book.getGrammarId(),
					book.getFamiliarity(), book.getNextReviewTime(), nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getWrongCount()), null, nz(book.getCorrectCount()) + nz(book.getWrongCount()),
					nz(book.getCorrectCount()), weakReason != null, weakReason, null, existing, now);
		}
		for (UserKnowledgeProgress progress : progressMapper.selectList(Wrappers.<UserKnowledgeProgress>lambdaQuery()
				.eq(UserKnowledgeProgress::getUserId, userId))) {
			if (!StringUtils.hasText(progress.getKnowledgePointId())) {
				continue;
			}
			Date firstWeakAt = kpWeak.get(progress.getKnowledgePointId());
			inserted += insertLegacy(userId, ReviewConsts.MODULE_KP, progress.getSubjectId(),
					progress.getKnowledgePointId(), null, progress.getLastPracticeAt(),
					nz(progress.getTimes()), 0, nz(progress.getMastery()),
					nz(progress.getTotalCount()), nz(progress.getCorrectCount()),
					firstWeakAt != null, firstWeakAt == null ? null : ReviewConsts.REASON_LOW_RETRIEVABILITY,
					firstWeakAt, existing, now);
		}
		return inserted;
	}

	// ---------------- 调度核心 ----------------

	/**
	 * 单次作答的核心计算与落库。
	 */
	private ReviewAnswerResult apply(String userId, ReviewAnswerCommand command) {
		Date now = new Date();
		String module = command.getModule();
		String knowledgeId = command.getKnowledgeId();
		MemorySchedule schedule = scheduleMapper.selectOne(Wrappers.<MemorySchedule>lambdaQuery()
				.eq(MemorySchedule::getUserId, userId)
				.eq(MemorySchedule::getModule, module)
				.eq(MemorySchedule::getKnowledgeId, knowledgeId)
				.last("limit 1"));
		boolean created = schedule == null;
		if (created) {
			schedule = new MemorySchedule();
			schedule.setUserId(userId);
			schedule.setModule(module);
			schedule.setKnowledgeId(knowledgeId);
			schedule.setSubjectId(command.getSubjectId());
			schedule.setState(ReviewConsts.STATE_NEW);
			schedule.setDifficulty(0.0);
			schedule.setStability(0.0);
			schedule.setReps(0);
			schedule.setLapses(0);
			schedule.setConsecutiveWrong(0);
			schedule.setAnswers(0);
			schedule.setCorrects(0);
			schedule.setOutputWrong(0);
			schedule.setEaseFactor(Sm2ColdStart.DEFAULT_EASE);
			schedule.setWeakFlag(0);
			schedule.setReclaimCount(0);
		}
		String demand = StringUtils.hasText(command.getDemand()) ? command.getDemand()
				: ReviewDemand.of(command.getAngle(), null);
		boolean correct = Boolean.TRUE.equals(command.getCorrect());
		boolean hint = Boolean.TRUE.equals(command.getHintUsed());
		boolean retried = Boolean.TRUE.equals(command.getRetried());
		int grade = command.getCorrectRate() != null
				? ReviewGrade.gradeOfRate(command.getCorrectRate(), command.getTotal() == null ? 0 : command.getTotal())
				: ReviewGrade.gradeOf(correct, command.getLatencyMs(), hint, retried, demand);
		if (Boolean.FALSE.equals(command.getFirstAttempt())) {
			// 重做/补答只作训练：首答已记过失，重做再记会把稳定度洗白，故不重复入账
			return skipped(schedule, grade, now);
		}

		int oldReps = nz(schedule.getReps());
		double oldStability = nzDouble(schedule.getStability());
		double oldDifficulty = nzDouble(schedule.getDifficulty());
		double ease = nzDouble(schedule.getEaseFactor(), Sm2ColdStart.DEFAULT_EASE);
		double elapsed = schedule.getLastReviewAt() == null ? 0 : daysBetween(schedule.getLastReviewAt(), now);
		double beforeR = oldReps == 0 ? 0 : Fsrs5.retrievability(elapsed, oldStability);
		double newStability;
		double newDifficulty;
		double interval;
		boolean shortTerm = elapsed < 1;
		String successGain = demand;

		if (oldReps < 2) {
			Sm2ColdStart.Sm2Result sm2 = Sm2ColdStart.next(oldReps, nzDouble(schedule.getScheduledDays()), ease, grade);
			ease = sm2.easeFactor();
			interval = sm2.intervalDays();
			if (grade <= ReviewGrade.AGAIN) {
				newStability = Math.max(Fsrs5.MIN_STABILITY,
						(oldStability > 0 ? oldStability : Fsrs5.initStability(ReviewGrade.HARD)) * 0.5);
			}
			else {
				newStability = Math.max(Fsrs5.MIN_STABILITY, interval * (grade == ReviewGrade.EASY ? 1.2 : 1.0));
			}
			newDifficulty = oldDifficulty > 0 ? Fsrs5.nextDifficulty(oldDifficulty, grade)
					: Fsrs5.initDifficulty(grade);
		}
		else {
			newDifficulty = Fsrs5.nextDifficulty(oldDifficulty, grade);
			if (grade <= ReviewGrade.AGAIN) {
				newStability = Fsrs5.nextStabilityAfterLapse(oldDifficulty, oldStability, beforeR,
						ReviewDemand.failurePenalty(demand));
			}
			else if (shortTerm) {
				newStability = Fsrs5.nextShortTermStability(oldStability, grade,
						ReviewDemand.successGain(successGain));
			}
			else {
				newStability = Fsrs5.nextStability(oldDifficulty, oldStability, beforeR, grade,
						ReviewDemand.successGain(successGain));
			}
			interval = Math.max(1.0, Fsrs5.nextInterval(newStability, TARGET_RETENTION));
		}
		Date dueAt = new Date(now.getTime() + Math.round(interval * 24L * 60 * 60 * 1000));

		int consecutiveWrong = correct ? 0 : nz(schedule.getConsecutiveWrong()) + 1;
		// 知识点级按会话上报，没有「输出题」概念，避免把普通答错记成输出错误
		boolean kp = ReviewConsts.MODULE_KP.equals(module);
		boolean output = !kp && ReviewDemand.isOutput(demand);
		boolean outputWrong = output && !correct;
		WeakStateMachine.WeakSnapshot snapshot = new WeakStateMachine.WeakSnapshot(
				nz(schedule.getWeakFlag()) == 1, schedule.getWeakReason(), schedule.getLastAngle(),
				nz(schedule.getAnswers()), nz(schedule.getConsecutiveWrong()), nz(schedule.getLapses()),
				schedule.getConqueredAt() != null, nz(schedule.getCorrects()), nz(schedule.getLastGrade()));
		boolean outputWeakRequired = !kp && (ReviewConsts.REASON_OUTPUT_ERROR.equals(schedule.getWeakReason())
				|| nz(schedule.getOutputWrong()) > 0);
		WeakStateMachine.WeakEvidence evidence = new WeakStateMachine.WeakEvidence(correct, grade, beforeR,
				newStability, demand, output, command.getAngle(), outputWeakRequired);
		WeakStateMachine.WeakTransition transition = WeakStateMachine.evaluate(snapshot, evidence);

		schedule.setSubjectId(StringUtils.hasText(command.getSubjectId()) ? command.getSubjectId()
				: schedule.getSubjectId());
		schedule.setDifficulty(newDifficulty);
		schedule.setStability(newStability);
		schedule.setEaseFactor(ease);
		schedule.setReps(oldReps + 1);
		schedule.setAnswers(nz(schedule.getAnswers()) + 1);
		schedule.setCorrects(nz(schedule.getCorrects()) + (correct ? 1 : 0));
		schedule.setConsecutiveWrong(consecutiveWrong);
		schedule.setLapses(nz(schedule.getLapses()) + (grade <= ReviewGrade.AGAIN ? 1 : 0));
		schedule.setOutputWrong(nz(schedule.getOutputWrong()) + (outputWrong ? 1 : 0));
		schedule.setLastGrade(grade);
		schedule.setLastAngle(command.getAngle() == null ? schedule.getLastAngle() : command.getAngle());
		schedule.setLastDemand(demand);
		schedule.setLastReviewAt(now);
		schedule.setDueAt(dueAt);
		schedule.setScheduledDays(interval);
		schedule.setElapsedDays(elapsed);
		schedule.setState(stateOf(grade, oldReps));
		schedule.setWeakFlag(transition.weakFlag() ? 1 : 0);
		schedule.setWeakReason(transition.weakReason());
		if (transition.weakFlag() && schedule.getFirstWeakAt() == null) {
			schedule.setFirstWeakAt(now);
		}
		if (transition.conquered()) {
			schedule.setConqueredAt(now);
		}
		if (transition.lapseDelta() > 0) {
			schedule.setReclaimCount(nz(schedule.getReclaimCount()) + transition.lapseDelta());
		}
		if (created) {
			scheduleMapper.insert(schedule);
		}
		else {
			scheduleMapper.updateById(schedule);
		}

		int mastery = mastery(schedule, now);
		writeLog(userId, command, schedule, demand, grade, correct, beforeR, oldStability, oldDifficulty,
				newStability, newDifficulty, elapsed, interval, now);
		syncLegacy(userId, command, schedule, transition, mastery, now);
		recordWeakEvent(userId, command, schedule, transition, mastery, snapshot.weakFlag());

		ReviewAnswerResult result = new ReviewAnswerResult();
		result.setGrade(grade);
		result.setState(schedule.getState());
		result.setRetrievabilityBefore(round3(beforeR));
		result.setStability(round3(newStability));
		result.setDifficulty(round3(newDifficulty));
		result.setIntervalDays(round3(interval));
		result.setDueAt(dueAt);
		result.setMastery(mastery);
		result.setWeak(transition.weakFlag());
		result.setWeakReason(transition.weakReason());
		result.setConquered(transition.conquered());
		result.setReviews(schedule.getReps());
		result.setLapses(schedule.getLapses());
		return result;
	}

	/**
	 * 重做/补答：不写日志、不改调度，只回显当前状态。
	 *
	 * @param schedule 当前调度
	 * @param grade 本次尝试的评分
	 * @param now 当前时间
	 * @return 调度结果（ignored=true）
	 */
	private ReviewAnswerResult skipped(MemorySchedule schedule, int grade, Date now) {
		ReviewAnswerResult result = new ReviewAnswerResult();
		result.setIgnored(true);
		result.setGrade(grade);
		result.setState(schedule.getState());
		result.setRetrievabilityBefore(round3(retrievability(schedule, now)));
		result.setStability(round3(nzDouble(schedule.getStability())));
		result.setDifficulty(round3(nzDouble(schedule.getDifficulty())));
		result.setIntervalDays(round3(nzDouble(schedule.getScheduledDays())));
		result.setDueAt(schedule.getDueAt());
		result.setMastery(mastery(schedule, now));
		result.setWeak(nz(schedule.getWeakFlag()) == 1);
		result.setWeakReason(schedule.getWeakReason());
		result.setConquered(false);
		result.setReviews(nz(schedule.getReps()));
		result.setLapses(nz(schedule.getLapses()));
		return result;
	}

	/**
	 * 复习日志落库。
	 */
	private void writeLog(String userId, ReviewAnswerCommand command, MemorySchedule schedule, String demand,
			int grade, boolean correct, double beforeR, double oldStability, double oldDifficulty,
			double newStability, double newDifficulty, double elapsed, double interval, Date now) {
		MemoryReviewLog logRow = new MemoryReviewLog();
		logRow.setUserId(userId);
		logRow.setModule(schedule.getModule());
		logRow.setSubjectId(schedule.getSubjectId());
		logRow.setKnowledgeId(schedule.getKnowledgeId());
		logRow.setGrade(grade);
		logRow.setCorrect(correct ? 1 : 0);
		logRow.setLatencyMs(command.getLatencyMs() == null ? null
				: (int) Math.min(Integer.MAX_VALUE, command.getLatencyMs()));
		logRow.setAngle(command.getAngle());
		logRow.setDemand(demand);
		logRow.setStabilityBefore(oldStability);
		logRow.setStabilityAfter(newStability);
		logRow.setDifficultyBefore(oldDifficulty);
		logRow.setDifficultyAfter(newDifficulty);
		logRow.setRetrievabilityBefore(beforeR);
		logRow.setElapsedDays(elapsed);
		logRow.setScheduledDays(interval);
		logRow.setTargetRetention(TARGET_RETENTION);
		logRow.setSessionId(command.getSessionId());
		logRow.setReviewedAt(now);
		logMapper.insert(logRow);
	}

	/**
	 * 旧表回写：单词/句子/语法本与知识点进度、薄弱池。
	 */
	private void syncLegacy(String userId, ReviewAnswerCommand command, MemorySchedule schedule,
			WeakStateMachine.WeakTransition transition, int mastery, Date now) {
		String module = schedule.getModule();
		try {
			if (ReviewConsts.MODULE_WORD.equals(module)) {
				EnglishWordBook book = wordBookMapper.selectOne(Wrappers.<EnglishWordBook>lambdaQuery()
						.eq(EnglishWordBook::getUserId, userId)
						.eq(EnglishWordBook::getWordId, schedule.getKnowledgeId()).last("limit 1"));
				if (book != null) {
					book.setFamiliarity(familiarityOf(schedule.getStability()));
					book.setNextReviewTime(schedule.getDueAt());
					wordBookMapper.updateById(book);
				}
			}
			else if (ReviewConsts.MODULE_SENTENCE.equals(module)) {
				EnglishSentenceBook book = sentenceBookMapper.selectOne(Wrappers.<EnglishSentenceBook>lambdaQuery()
						.eq(EnglishSentenceBook::getUserId, userId)
						.eq(EnglishSentenceBook::getSentenceId, schedule.getKnowledgeId()).last("limit 1"));
				if (book != null) {
					book.setFamiliarity(familiarityOf(schedule.getStability()));
					book.setNextReviewTime(schedule.getDueAt());
					sentenceBookMapper.updateById(book);
				}
			}
			else if (ReviewConsts.MODULE_GRAMMAR.equals(module)) {
				EnglishGrammarBook book = grammarBookMapper.selectOne(Wrappers.<EnglishGrammarBook>lambdaQuery()
						.eq(EnglishGrammarBook::getUserId, userId)
						.eq(EnglishGrammarBook::getGrammarId, schedule.getKnowledgeId()).last("limit 1"));
				if (book != null) {
					book.setFamiliarity(familiarityOf(schedule.getStability()));
					book.setNextReviewTime(schedule.getDueAt());
					grammarBookMapper.updateById(book);
				}
			}
			if (!ReviewConsts.MODULE_KP.equals(module)) {
				EnglishLearningState state = learningStateMapper.selectOne(Wrappers.<EnglishLearningState>lambdaQuery()
						.eq(EnglishLearningState::getUserId, userId)
						.eq(EnglishLearningState::getModule, module)
						.eq(EnglishLearningState::getKnowledgeId, schedule.getKnowledgeId()).last("limit 1"));
				if (state != null) {
					state.setWeakFlag(transition.weakFlag() ? 1 : 0);
					state.setWeakReason(transition.weakReason());
					learningStateMapper.updateById(state);
				}
			}
			else {
				syncKpLegacy(userId, schedule, transition, mastery, now);
			}
		}
		catch (Exception e) {
			log.warn("记忆调度回写旧表失败 user={} module={} kp={}", userId, module, schedule.getKnowledgeId(), e);
		}
	}

	/**
	 * 知识点级旧表回写：薄弱池与掌握度。
	 */
	private void syncKpLegacy(String userId, MemorySchedule schedule, WeakStateMachine.WeakTransition transition,
			int mastery, Date now) {
		UserWeakKnowledge weak = weakMapper.selectOne(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getKnowledgePointId, schedule.getKnowledgeId()).last("limit 1"));
		if (transition.weakFlag()) {
			if (weak == null) {
				weak = new UserWeakKnowledge();
				weak.setUserId(userId);
				weak.setSubjectId(schedule.getSubjectId());
				weak.setKnowledgePointId(schedule.getKnowledgeId());
				weak.setStatus("active");
				weak.setConquerTimes(0);
				weak.setFirstWeakAt(now);
				weakMapper.insert(weak);
			}
			else if (!"active".equals(weak.getStatus())) {
				weak.setStatus("active");
				weakMapper.updateById(weak);
			}
		}
		else if (transition.conquered() && weak != null && "active".equals(weak.getStatus())) {
			weak.setStatus("conquered");
			weak.setConquerTimes(nz(weak.getConquerTimes()) + 1);
			weak.setConqueredAt(now);
			weakMapper.updateById(weak);
		}
		UserKnowledgeProgress progress = progressMapper.selectOne(Wrappers.<UserKnowledgeProgress>lambdaQuery()
				.eq(UserKnowledgeProgress::getUserId, userId)
				.eq(UserKnowledgeProgress::getKnowledgePointId, schedule.getKnowledgeId()).last("limit 1"));
		if (progress != null) {
			progress.setMastery(mastery);
			progress.setLevel(levelOf(mastery));
			progressMapper.updateById(progress);
		}
	}

	/**
	 * 薄弱跃迁事件。
	 */
	private void recordWeakEvent(String userId, ReviewAnswerCommand command, MemorySchedule schedule,
			WeakStateMachine.WeakTransition transition, int mastery, boolean wasWeak) {
		if (!ReviewConsts.MODULE_KP.equals(schedule.getModule())) {
			return;
		}
		String eventType;
		if (transition.conquered()) {
			eventType = "conquered";
		}
		else if (transition.reopened()) {
			eventType = "reopened";
		}
		else if (transition.weakFlag() && !wasWeak) {
			eventType = "discovered";
		}
		else {
			return;
		}
		WeakPointEvent event = new WeakPointEvent();
		event.setStudentId(userId);
		event.setSubjectId(schedule.getSubjectId());
		event.setKnowledgePointId(schedule.getKnowledgeId());
		event.setEventType(eventType);
		event.setMastery(mastery);
		event.setSource("practice");
		event.setRefId("memory:" + schedule.getModule() + ":" + todayKey());
		event.setKpName(command.getKnowledgeName());
		event.setOccurredAt(new Date());
		weakPointEventRecorder.publish(event);
	}

	// ---------------- 查询辅助 ----------------

	private List<MemorySchedule> schedules(String userId, String module, String subjectId) {
		return scheduleMapper.selectList(Wrappers.<MemorySchedule>lambdaQuery()
				.eq(MemorySchedule::getUserId, userId)
				.eq(StringUtils.hasText(module), MemorySchedule::getModule, module)
				.eq(StringUtils.hasText(subjectId), MemorySchedule::getSubjectId, subjectId));
	}

	private List<MemorySchedule> filteredSchedules(String userId, ReviewQueueRequest request) {
		List<MemorySchedule> all = schedules(userId, request.getModule(), request.getSubjectId());
		boolean scoped = StringUtils.hasText(request.getVersion()) || StringUtils.hasText(request.getGrade())
				|| StringUtils.hasText(request.getTerm()) || StringUtils.hasText(request.getUnit());
		if (!scoped) {
			return all;
		}
		Map<String, Set<String>> allowed = new HashMap<>();
		List<MemorySchedule> result = new ArrayList<>();
		for (MemorySchedule schedule : all) {
			String module = schedule.getModule();
			if (ReviewConsts.MODULE_KP.equals(module)) {
				result.add(schedule);
				continue;
			}
			Set<String> ids = allowed.computeIfAbsent(module, key -> contentIds(key, request));
			if (ids.contains(schedule.getKnowledgeId())) {
				result.add(schedule);
			}
		}
		return result;
	}

	private Set<String> contentIds(String module, ReviewQueueRequest request) {
		if (ReviewConsts.MODULE_WORD.equals(module)) {
			return wordMapper.selectList(Wrappers.<EnglishWord>lambdaQuery()
							.eq(StringUtils.hasText(request.getVersion()), EnglishWord::getVersion, request.getVersion())
							.eq(StringUtils.hasText(request.getGrade()), EnglishWord::getGrade, request.getGrade())
							.eq(StringUtils.hasText(request.getTerm()), EnglishWord::getTerm, request.getTerm())
							.eq(StringUtils.hasText(request.getUnit()), EnglishWord::getUnit, request.getUnit()))
					.stream().map(EnglishWord::getId).collect(Collectors.toSet());
		}
		if (ReviewConsts.MODULE_SENTENCE.equals(module)) {
			return sentenceMapper.selectList(Wrappers.<EnglishSentence>lambdaQuery()
							.eq(StringUtils.hasText(request.getVersion()), EnglishSentence::getVersion, request.getVersion())
							.eq(StringUtils.hasText(request.getGrade()), EnglishSentence::getGrade, request.getGrade())
							.eq(StringUtils.hasText(request.getTerm()), EnglishSentence::getTerm, request.getTerm())
							.eq(StringUtils.hasText(request.getUnit()), EnglishSentence::getUnit, request.getUnit()))
					.stream().map(EnglishSentence::getId).collect(Collectors.toSet());
		}
		if (ReviewConsts.MODULE_GRAMMAR.equals(module)) {
			return grammarMapper.selectList(Wrappers.<EnglishGrammar>lambdaQuery()
							.eq(StringUtils.hasText(request.getVersion()), EnglishGrammar::getVersion, request.getVersion())
							.eq(StringUtils.hasText(request.getGrade()), EnglishGrammar::getGrade, request.getGrade())
							.eq(StringUtils.hasText(request.getTerm()), EnglishGrammar::getTerm, request.getTerm())
							.eq(StringUtils.hasText(request.getUnit()), EnglishGrammar::getUnit, request.getUnit()))
					.stream().map(EnglishGrammar::getId).collect(Collectors.toSet());
		}
		return Collections.emptySet();
	}

	private Map<String, String> names(Collection<MemorySchedule> schedules) {
		Map<String, Set<String>> idsByModule = new HashMap<>();
		for (MemorySchedule schedule : schedules) {
			idsByModule.computeIfAbsent(schedule.getModule(), key -> new HashSet<>())
					.add(schedule.getKnowledgeId());
		}
		Map<String, String> names = new HashMap<>();
		for (Map.Entry<String, Set<String>> entry : idsByModule.entrySet()) {
			String module = entry.getKey();
			Set<String> ids = entry.getValue();
			if (ids.isEmpty()) {
				continue;
			}
			if (ReviewConsts.MODULE_WORD.equals(module)) {
				wordMapper.selectBatchIds(ids).forEach(word -> names.put("word:" + word.getId(), word.getSpell()));
			}
			else if (ReviewConsts.MODULE_SENTENCE.equals(module)) {
				sentenceMapper.selectBatchIds(ids).forEach(sentence -> names.put("sentence:" + sentence.getId(),
						sentence.getEn()));
			}
			else if (ReviewConsts.MODULE_GRAMMAR.equals(module)) {
				grammarMapper.selectBatchIds(ids).forEach(grammar -> names.put("grammar:" + grammar.getId(),
						grammar.getTitle()));
			}
			else {
				knowledgePointMapper.selectBatchIds(ids).forEach(point -> names.put("kp:" + point.getId(),
						point.getName()));
			}
		}
		return names;
	}

	private List<ReviewQueueItemView> toQueueViews(List<MemorySchedule> schedules, Date now, boolean withSource) {
		Map<String, String> names = names(schedules);
		List<ReviewQueueItemView> result = new ArrayList<>();
		for (MemorySchedule schedule : schedules) {
			ReviewQueueItemView view = new ReviewQueueItemView();
			view.setModule(schedule.getModule());
			view.setSubjectId(schedule.getSubjectId());
			view.setKnowledgeId(schedule.getKnowledgeId());
			view.setName(names.get(schedule.getModule() + ":" + schedule.getKnowledgeId()));
			view.setAngle(suggestedAngle(schedule));
			view.setDemand(suggestedDemand(schedule));
			view.setRetrievability(round3(retrievability(schedule, now)));
			view.setMastery(mastery(schedule, now));
			Date due = schedule.getDueAt();
			view.setDueAt(due);
			view.setOverdueDays(due == null ? 0 : Math.max(0, round3(daysBetween(due, now))));
			view.setWeak(nz(schedule.getWeakFlag()) == 1);
			view.setWeakReason(schedule.getWeakReason());
			view.setLastGrade(schedule.getLastGrade());
			view.setStability(round3(nzDouble(schedule.getStability())));
			view.setState(schedule.getState());
			if (withSource) {
				if (nz(schedule.getReps()) == 0) {
					view.setSource(ReviewConsts.SOURCE_NEW);
					view.setSeverity(0.2);
				}
				else if (schedule.getDueAt() != null && !schedule.getDueAt().after(now)) {
					view.setSource(ReviewConsts.SOURCE_DUE);
					view.setSeverity(round3(severity(schedule, now)));
				}
				else {
					view.setSource(ReviewConsts.SOURCE_WEAK);
					view.setSeverity(round3(severity(schedule, now)));
				}
			}
			else {
				view.setSeverity(round3(severity(schedule, now)));
			}
			result.add(view);
		}
		return result;
	}

	private void fillBase(WeakBoardItemView view, MemorySchedule schedule, Date now, Map<String, String> names) {
		view.setModule(schedule.getModule());
		view.setSubjectId(schedule.getSubjectId());
		view.setKnowledgeId(schedule.getKnowledgeId());
		view.setName(names.get(schedule.getModule() + ":" + schedule.getKnowledgeId()));
		view.setWeakReason(schedule.getWeakReason());
		view.setLastGrade(schedule.getLastGrade());
		view.setLapses(nz(schedule.getLapses()));
		view.setDueAt(schedule.getDueAt());
	}

	// ---------------- 纯计算辅助 ----------------

	private double retrievability(MemorySchedule schedule, Date now) {
		if (nz(schedule.getReps()) == 0 || schedule.getLastReviewAt() == null) {
			return 0;
		}
		double elapsed = daysBetween(schedule.getLastReviewAt(), now);
		return Fsrs5.retrievability(elapsed, nzDouble(schedule.getStability()));
	}

	private double severity(MemorySchedule schedule, Date now) {
		double r = retrievability(schedule, now);
		double weight = ReviewDemand.weight(schedule.getLastDemand() == null ? ReviewConsts.DEMAND_CUED
				: schedule.getLastDemand());
		double value = (1 - r) * weight * (1 + 0.5 * nz(schedule.getLapses()));
		if (nz(schedule.getOutputWrong()) > 0 && !ReviewConsts.MODULE_KP.equals(schedule.getModule())) {
			value *= 1.3;
		}
		if (nz(schedule.getWeakFlag()) == 1) {
			value *= 1.5;
			// 刚复习完 R≈1 不应把薄弱项排到队尾，给薄弱项一个随失误次数抬升的严重度下限
			value = Math.max(value, 0.6 + 0.15 * Math.min(nz(schedule.getLapses()), 4));
		}
		return Math.max(0, Math.min(3, value));
	}

	private int mastery(MemorySchedule schedule, Date now) {
		int answers = nz(schedule.getAnswers());
		if (answers <= 0) {
			return 0;
		}
		double accuracy = nz(schedule.getCorrects()) / (double) answers;
		double r = retrievability(schedule, now);
		double blended = 0.45 * r + 0.55 * accuracy;
		double confidence = Math.min(1.0, 0.55 + 0.05 * answers);
		// 最近一次答错/带提示答对时压低掌握度，避免「刚做错仍显示 80 分」
		int lastGrade = nz(schedule.getLastGrade());
		double gradeFactor = lastGrade <= ReviewGrade.AGAIN ? 0.55
				: lastGrade == ReviewGrade.HARD ? 0.8 : 1.0;
		double value = 100 * blended * confidence * gradeFactor - 4.0 * nz(schedule.getConsecutiveWrong());
		return (int) Math.max(0, Math.min(100, Math.round(value)));
	}

	private Date forgettingAt(MemorySchedule schedule) {
		double stability = nzDouble(schedule.getStability());
		if (stability <= 0) {
			return null;
		}
		double days = stability / Fsrs5.FACTOR
				* (Math.pow(FORGETTING_RETRIEVABILITY, 1 / Fsrs5.DECAY) - 1);
		Date base = schedule.getLastReviewAt() == null ? new Date() : schedule.getLastReviewAt();
		return new Date(base.getTime() + Math.round(Math.max(0, days) * 24L * 60 * 60 * 1000));
	}

	private String suggestedAngle(MemorySchedule schedule) {
		List<String> pool = EnglishExamAngle.anglesOf(schedule.getModule());
		if (pool.isEmpty()) {
			return null;
		}
		for (String angle : pool) {
			if (!angle.equals(schedule.getLastAngle())) {
				return angle;
			}
		}
		return pool.get(0);
	}

	private String suggestedDemand(MemorySchedule schedule) {
		String angle = suggestedAngle(schedule);
		if (angle == null) {
			return ReviewConsts.DEMAND_CUED;
		}
		return ReviewDemand.of(angle, null);
	}

	private int outputCount(List<MemorySchedule> schedules) {
		int count = 0;
		for (MemorySchedule schedule : schedules) {
			if (ReviewDemand.isOutput(suggestedDemand(schedule))) {
				count++;
			}
		}
		return count;
	}

	private String stateOf(int grade, int oldReps) {
		if (grade <= ReviewGrade.AGAIN) {
			return oldReps == 0 ? ReviewConsts.STATE_LEARNING : ReviewConsts.STATE_RELEARNING;
		}
		return oldReps + 1 < 2 ? ReviewConsts.STATE_LEARNING : ReviewConsts.STATE_REVIEW;
	}

	private int familiarityOf(Double stability) {
		double value = nzDouble(stability);
		if (value < 1) {
			return 0;
		}
		if (value < 3) {
			return 1;
		}
		if (value < 7) {
			return 2;
		}
		if (value < 16) {
			return 3;
		}
		return 4;
	}

	private String levelOf(int mastery) {
		if (mastery >= 85) {
			return "精通";
		}
		if (mastery >= 70) {
			return "熟练";
		}
		if (mastery >= 55) {
			return "掌握";
		}
		if (mastery >= 40) {
			return "待巩固";
		}
		return "薄弱";
	}

	private int insertLegacy(String userId, String module, String subjectId, String knowledgeId,
			Integer familiarity, Date nextReviewTime, int reps, int lapses, Integer mastery,
			int answers, int corrects, boolean weak, String weakReason, Date firstWeakAt,
			Set<String> existing, Date now) {
		if (!StringUtils.hasText(knowledgeId) || existing.contains(module + ":" + knowledgeId)) {
			return 0;
		}
		MemorySchedule schedule = new MemorySchedule();
		schedule.setUserId(userId);
		schedule.setModule(module);
		schedule.setSubjectId(subjectId);
		schedule.setKnowledgeId(knowledgeId);
		schedule.setState(reps > 0 ? ReviewConsts.STATE_REVIEW : ReviewConsts.STATE_NEW);
		double stability;
		double difficulty;
		if (ReviewConsts.MODULE_KP.equals(module)) {
			int value = mastery == null ? 0 : Math.max(0, Math.min(100, mastery));
			double ratio = value / 100.0;
			stability = Math.max(0.5, 0.5 + 29.5 * Math.pow(ratio, 1.5));
			difficulty = 1 + 9 * (1 - ratio);
		}
		else {
			int value = familiarity == null ? 0 : Math.max(0, Math.min(4, familiarity));
			stability = new double[] { 0.4, 1.0, 3.0, 7.0, 16.0 }[value];
			difficulty = 5.0;
		}
		schedule.setStability(stability);
		schedule.setDifficulty(difficulty);
		schedule.setEaseFactor(Sm2ColdStart.DEFAULT_EASE);
		schedule.setReps(reps);
		schedule.setLapses(lapses);
		schedule.setConsecutiveWrong(0);
		schedule.setAnswers(Math.max(answers, reps));
		schedule.setCorrects(Math.max(0, Math.min(corrects, Math.max(answers, reps))));
		schedule.setOutputWrong(0);
		schedule.setWeakFlag(weak ? 1 : 0);
		schedule.setWeakReason(weak ? weakReason : null);
		schedule.setFirstWeakAt(weak ? firstWeakAt : null);
		schedule.setReclaimCount(0);
		schedule.setLastGrade(0);
		schedule.setScheduledDays(stability);
		schedule.setElapsedDays(0.0);
		schedule.setDueAt(nextReviewTime == null ? now : nextReviewTime);
		schedule.setLastReviewAt(nextReviewTime == null ? null
				: new Date(nextReviewTime.getTime() - Math.round(stability * 24L * 60 * 60 * 1000)));
		scheduleMapper.insert(schedule);
		existing.add(module + ":" + knowledgeId);
		return 1;
	}

	private static String key(MemorySchedule schedule) {
		return schedule.getModule() + ":" + schedule.getKnowledgeId();
	}

	private static double daysBetween(Date from, Date to) {
		if (from == null || to == null) {
			return 0;
		}
		return Math.max(0, (to.getTime() - from.getTime()) / (24.0 * 60 * 60 * 1000));
	}

	private static int nz(Integer value) {
		return value == null ? 0 : value;
	}

	private static double nzDouble(Double value) {
		return value == null ? 0 : value;
	}

	private static double nzDouble(Double value, double fallback) {
		return value == null || value <= 0 ? fallback : value;
	}

	private static double round3(double value) {
		return Math.round(value * 1000.0) / 1000.0;
	}

	private static String todayKey() {
		return new SimpleDateFormat("yyyy-MM-dd").format(new Date());
	}

}
