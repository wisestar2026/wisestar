package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.EvaluationService;
import cn.wisestar.server.service.RewardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学员学习评价实现。
 *
 * <p>掌握度 = 最近 5 次练习正确率的加权平均（越近权重越高 5,4,3,2,1）；
 * 薄弱 = 掌握度 &lt;55 或存在未订正错题；攻克 = 正确率 ≥80（显式）或 掌握度 ≥70 且本次正确率 ≥80（自动）。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

	/** 薄弱阈值 */
	private static final int WEAK_THRESHOLD = 55;

	/** 攻克所需掌握度 */
	private static final int CONQUER_MASTERY = 70;

	/** 攻克所需本次正确率 */
	private static final int CONQUER_RATE = 80;

	/** 掌握度窗口（最近 N 次练习） */
	private static final int WINDOW = 5;

	private final UserKnowledgeProgressMapper progressMapper;

	private final UserWeakKnowledgeMapper weakMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final SectionMapper sectionMapper;

	private final ChapterMapper chapterMapper;

	private final PracticeRecordMapper practiceRecordMapper;

	private final PracticeDetailMapper practiceDetailMapper;

	private final TemplateMapper templateMapper;

	private final RewardService rewardService;

	@Override
	public void recordPractice(PracticeEvaluationContext context) {
		if (context == null || !StringUtils.hasText(context.getUserId()) || context.getItems() == null
				|| context.getItems().isEmpty()) {
			return;
		}
		String userId = context.getUserId();
		// 会话级知识点上下文
		KpContext ctx = resolveContext(context.getKnowledgePointId(), context.getSectionId());
		if (ctx == null && !StringUtils.hasText(context.getKnowledgePointId())
				&& !StringUtils.hasText(context.getSectionId())) {
			return;
		}
		Map<String, int[]> agg = new LinkedHashMap<>();
		Map<String, String> nameCache = new HashMap<>();
		for (PracticeEvaluationContext.Item item : context.getItems()) {
			String kpId = resolveKpId(item.getKnowledgePointNames(), ctx, nameCache);
			if (!StringUtils.hasText(kpId)) {
				kpId = context.getKnowledgePointId();
			}
			if (!StringUtils.hasText(kpId)) {
				continue;
			}
			int[] slot = agg.computeIfAbsent(kpId, k -> new int[2]);
			slot[1]++;
			if (item.isCorrect()) {
				slot[0]++;
			}
		}
		for (Map.Entry<String, int[]> e : agg.entrySet()) {
			int total = e.getValue()[1];
			if (total <= 0) {
				continue;
			}
			int correct = e.getValue()[0];
			int rate = (int) Math.round(correct * 100.0 / total);
			updateProgress(userId, e.getKey(), rate, total, correct);
		}
		if (ctx != null && StringUtils.hasText(ctx.subjectId)) {
			checkChapterStage(userId, ctx.subjectId);
		}
	}

	@Override
	public boolean conquer(String userId, String knowledgePointId, int correctRate) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(knowledgePointId)
				|| correctRate < CONQUER_RATE) {
			return false;
		}
		KpContext ctx = resolveContext(knowledgePointId, null);
		String subjectId = ctx == null ? null : ctx.subjectId;
		recordConquerRate(userId, knowledgePointId, ctx, correctRate);
		markConquered(userId, subjectId, knowledgePointId);
		if (StringUtils.hasText(subjectId)) {
			checkChapterStage(userId, subjectId);
		}
		return true;
	}

	@Override
	public void refreshWeakAfterCorrection(String userId, String knowledgePointId) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(knowledgePointId)) {
			return;
		}
		KnowledgePoint kp = knowledgePointMapper.selectById(knowledgePointId);
		if (kp == null) {
			return;
		}
		KpContext ctx = resolveContext(knowledgePointId, null);
		UserKnowledgeProgress p = findProgress(userId, ctx, knowledgePointId);
		int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
		UserWeakKnowledge w = findWeak(userId, ctx == null ? null : ctx.subjectId, knowledgePointId);
		if (w == null) {
			return;
		}
		boolean hasWrong = hasUncorrectedWrong(userId, kp.getName());
		if (!hasWrong && mastery >= WEAK_THRESHOLD) {
			markConquered(userId, ctx == null ? null : ctx.subjectId, knowledgePointId);
		}
	}

	// ---------------- 内部方法 ----------------

	/**
	 * 记录显式攻克复测的正确率，刷新掌握度。
	 *
	 * <p>攻克复测为达标演示（正确率 ≥80），按最近窗口加权计算掌握度，并保证结果不低于薄弱阈值，
	 * 使「薄弱已消除」与「掌握度 ≥55」的口径保持一致。</p>
	 */
	private void recordConquerRate(String userId, String kpId, KpContext ctx, int rate) {
		UserKnowledgeProgress p = findProgress(userId, ctx, kpId);
		if (p == null) {
			p = new UserKnowledgeProgress();
			p.setUserId(userId);
			p.setSubjectId(ctx == null ? null : ctx.subjectId);
			p.setVersionId(ctx == null ? null : ctx.versionId);
			p.setChapterId(ctx == null ? null : ctx.chapterId);
			p.setKnowledgePointId(kpId);
			p.setMastery(rate);
			p.setLevel(level(rate));
			p.setTimes(1);
			p.setTotalCount(0);
			p.setCorrectCount(0);
			p.setLastCorrectRate(rate);
			p.setRecentRates(String.valueOf(rate));
			p.setLastPracticeAt(new Date());
			progressMapper.insert(p);
			return;
		}
		List<Integer> rates = parseRates(p.getRecentRates());
		rates.add(0, rate);
		if (rates.size() > WINDOW) {
			rates = new ArrayList<>(rates.subList(0, WINDOW));
		}
		int mastery = Math.max(weighted(rates), WEAK_THRESHOLD);
		p.setRecentRates(rates.stream().map(String::valueOf).collect(Collectors.joining(",")));
		p.setMastery(mastery);
		p.setLevel(level(mastery));
		p.setTimes((p.getTimes() == null ? 0 : p.getTimes()) + 1);
		p.setLastCorrectRate(rate);
		p.setLastPracticeAt(new Date());
		progressMapper.updateById(p);
	}

	private void updateProgress(String userId, String kpId, int rate, int total, int correct) {
		KpContext ctx = resolveContext(kpId, null);
		UserKnowledgeProgress p = findProgress(userId, ctx, kpId);
		if (p == null) {
			p = new UserKnowledgeProgress();
			p.setUserId(userId);
			p.setSubjectId(ctx == null ? null : ctx.subjectId);
			p.setVersionId(ctx == null ? null : ctx.versionId);
			p.setChapterId(ctx == null ? null : ctx.chapterId);
			p.setKnowledgePointId(kpId);
			p.setMastery(rate);
			p.setLevel(level(rate));
			p.setTimes(1);
			p.setTotalCount(total);
			p.setCorrectCount(correct);
			p.setLastCorrectRate(rate);
			p.setRecentRates(String.valueOf(rate));
			p.setLastPracticeAt(new Date());
			progressMapper.insert(p);
		}
		else {
			List<Integer> rates = parseRates(p.getRecentRates());
			rates.add(0, rate);
			if (rates.size() > WINDOW) {
				rates = new ArrayList<>(rates.subList(0, WINDOW));
			}
			int mastery = weighted(rates);
			p.setRecentRates(rates.stream().map(String::valueOf).collect(Collectors.joining(",")));
			p.setMastery(mastery);
			p.setLevel(level(mastery));
			p.setTimes((p.getTimes() == null ? 0 : p.getTimes()) + 1);
			p.setTotalCount((p.getTotalCount() == null ? 0 : p.getTotalCount()) + total);
			p.setCorrectCount((p.getCorrectCount() == null ? 0 : p.getCorrectCount()) + correct);
			p.setLastCorrectRate(rate);
			p.setLastPracticeAt(new Date());
			progressMapper.updateById(p);
		}
		// 薄弱研判
		int mastery = p.getMastery() == null ? rate : p.getMastery();
		String subjectId = ctx == null ? null : ctx.subjectId;
		UserWeakKnowledge w = findWeak(userId, subjectId, kpId);
		if (mastery < WEAK_THRESHOLD) {
			if (w == null) {
				w = new UserWeakKnowledge();
				w.setUserId(userId);
				w.setSubjectId(subjectId);
				w.setKnowledgePointId(kpId);
				w.setStatus("active");
				w.setConquerTimes(0);
				w.setFirstWeakAt(new Date());
				weakMapper.insert(w);
			}
			else if (!"active".equals(w.getStatus())) {
				w.setStatus("active");
				weakMapper.updateById(w);
			}
		}
		else if (mastery >= CONQUER_MASTERY && rate >= CONQUER_RATE) {
			if (w != null && "active".equals(w.getStatus())) {
				markConquered(userId, subjectId, kpId);
			}
		}
	}

	private void markConquered(String userId, String subjectId, String kpId) {
		UserWeakKnowledge w = findWeak(userId, subjectId, kpId);
		boolean firstConquer = false;
		if (w == null) {
			w = new UserWeakKnowledge();
			w.setUserId(userId);
			w.setSubjectId(subjectId);
			w.setKnowledgePointId(kpId);
			w.setStatus("conquered");
			w.setConquerTimes(1);
			w.setConqueredAt(new Date());
			weakMapper.insert(w);
			firstConquer = true;
		}
		else {
			boolean wasActive = "active".equals(w.getStatus());
			w.setStatus("conquered");
			w.setConquerTimes((w.getConquerTimes() == null ? 0 : w.getConquerTimes()) + 1);
			w.setConqueredAt(new Date());
			weakMapper.updateById(w);
			firstConquer = wasActive;
		}
		if (firstConquer) {
			try {
				RewardContext rc = new RewardContext();
				rc.setUserId(userId);
				rc.setSubjectId(subjectId);
				rc.setKnowledgePointId(kpId);
				rc.setActionType(StudentRewardConstants.ACTION_WEAK_CONQUER);
				rc.setRefId(kpId);
				rewardService.settle(rc);
			}
			catch (Exception e) {
				log.warn("weak conquer reward failed: user={}, kp={}", userId, kpId, e);
			}
		}
	}

	/**
	 * 章节阶段里程碑：按学科累计「达标章节数」（掌握度 ≥55 的不同章节）结算 1..6 阶段奖励。
	 *
	 * <p>以 {@code ref_id={subjectId}:{stage}} 终身幂等，补齐所有已达成阶段，教学内容调整不重复发放也不追回。</p>
	 */
	private void checkChapterStage(String userId, String subjectId) {
		if (!StringUtils.hasText(subjectId)) {
			return;
		}
		List<UserKnowledgeProgress> progresses = progressMapper
				.selectList(Wrappers.<UserKnowledgeProgress>lambdaQuery()
						.eq(UserKnowledgeProgress::getUserId, userId)
						.eq(UserKnowledgeProgress::getSubjectId, subjectId)
						.isNotNull(UserKnowledgeProgress::getChapterId)
						.ge(UserKnowledgeProgress::getMastery, WEAK_THRESHOLD));
		long chapters = progresses.stream().map(UserKnowledgeProgress::getChapterId).distinct().count();
		int reached = (int) Math.min(chapters, 6);
		for (int stage = 1; stage <= reached; stage++) {
			try {
				RewardContext rc = new RewardContext();
				rc.setUserId(userId);
				rc.setSubjectId(subjectId);
				rc.setActionType(StudentRewardConstants.ACTION_CHAPTER_STAGE);
				rc.setStage(stage);
				rc.setRefId(subjectId + ":" + stage);
				rewardService.settle(rc);
			}
			catch (Exception e) {
				log.warn("chapter stage reward failed: user={}, subject={}, stage={}", userId, subjectId, stage, e);
			}
		}
	}

	private boolean hasUncorrectedWrong(String userId, String kpName) {
		if (!StringUtils.hasText(kpName)) {
			return false;
		}
		List<PracticeRecord> records = practiceRecordMapper
				.selectList(Wrappers.<PracticeRecord>lambdaQuery().eq(PracticeRecord::getUserId, userId));
		List<String> practiceIds = records.stream().map(PracticeRecord::getId).collect(Collectors.toList());
		if (practiceIds.isEmpty()) {
			return false;
		}
		List<PracticeDetail> wrongs = practiceDetailMapper.selectList(Wrappers.<PracticeDetail>lambdaQuery()
				.in(PracticeDetail::getPracticeId, practiceIds).eq(PracticeDetail::getIsCorrect, 0)
				.eq(PracticeDetail::getCorrected, false));
		for (PracticeDetail d : wrongs) {
			Template t = templateMapper.selectById(d.getQuestionId());
			if (t == null || t.getKnowledgePoint() == null) {
				continue;
			}
			for (String name : t.getKnowledgePoint()) {
				if (name != null && name.trim().equals(kpName)) {
					return true;
				}
			}
		}
		return false;
	}

	private String resolveKpId(List<String> knowledgePointNames, KpContext sessionCtx, Map<String, String> cache) {
		if (knowledgePointNames == null || knowledgePointNames.isEmpty()) {
			return null;
		}
		for (String name : knowledgePointNames) {
			if (!StringUtils.hasText(name)) {
				continue;
			}
			String key = name.trim();
			if (cache.containsKey(key)) {
				String cached = cache.get(key);
				if (StringUtils.hasText(cached)) {
					return cached;
				}
				continue;
			}
			List<KnowledgePoint> list = knowledgePointMapper
					.selectList(Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getName, key).last("limit 1"));
			String id = list.isEmpty() ? null : list.get(0).getId();
			cache.put(key, id == null ? "" : id);
			if (StringUtils.hasText(id)) {
				return id;
			}
		}
		return null;
	}

	private KpContext resolveContext(String kpId, String sectionId) {
		if (StringUtils.hasText(kpId)) {
			KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
			if (kp != null) {
				return buildContext(kp.getSectionId());
			}
		}
		if (StringUtils.hasText(sectionId)) {
			return buildContext(sectionId);
		}
		return null;
	}

	private KpContext buildContext(String sectionId) {
		if (!StringUtils.hasText(sectionId)) {
			return null;
		}
		Section section = sectionMapper.selectById(sectionId);
		if (section == null) {
			return null;
		}
		KpContext ctx = new KpContext();
		ctx.chapterId = section.getChapterId();
		if (StringUtils.hasText(section.getChapterId())) {
			Chapter chapter = chapterMapper.selectById(section.getChapterId());
			if (chapter != null) {
				ctx.subjectId = chapter.getSubjectId();
				ctx.versionId = chapter.getVersion();
			}
		}
		return ctx;
	}

	private UserKnowledgeProgress findProgress(String userId, KpContext ctx, String kpId) {
		if (ctx == null) {
			return null;
		}
		return progressMapper.selectOne(Wrappers.<UserKnowledgeProgress>lambdaQuery()
				.eq(UserKnowledgeProgress::getUserId, userId).eq(UserKnowledgeProgress::getSubjectId, ctx.subjectId)
				.eq(UserKnowledgeProgress::getVersionId, ctx.versionId)
				.eq(UserKnowledgeProgress::getKnowledgePointId, kpId).last("limit 1"));
	}

	private UserWeakKnowledge findWeak(String userId, String subjectId, String kpId) {
		return weakMapper.selectOne(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId).eq(UserWeakKnowledge::getKnowledgePointId, kpId)
				.eq(subjectId != null, UserWeakKnowledge::getSubjectId, subjectId).last("limit 1"));
	}

	private List<Integer> parseRates(String raw) {
		List<Integer> list = new ArrayList<>();
		if (StringUtils.hasText(raw)) {
			for (String s : raw.split(",")) {
				try {
					list.add(Integer.parseInt(s.trim()));
				}
				catch (NumberFormatException ignored) {
					// skip
				}
			}
		}
		return list;
	}

	/** 越近权重越高：第 1 个权重 5，依次 4,3,2,1。 */
	private int weighted(List<Integer> rates) {
		if (rates.isEmpty()) {
			return 0;
		}
		int num = 0;
		int den = 0;
		for (int i = 0; i < rates.size(); i++) {
			int w = WINDOW - i;
			num += w * rates.get(i);
			den += w;
		}
		return den == 0 ? 0 : (int) Math.round(num * 1.0 / den);
	}

	private String level(int mastery) {
		if (mastery >= 85) {
			return "精通";
		}
		if (mastery >= 70) {
			return "熟练";
		}
		if (mastery >= 55) {
			return "夯实";
		}
		if (mastery >= 40) {
			return "待巩固";
		}
		return "待攻克";
	}

	private static class KpContext {

		String subjectId;

		String versionId;

		String chapterId;

	}

}
