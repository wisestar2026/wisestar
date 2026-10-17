package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.StudentRewardConstants;
import cn.wisestar.server.domain.dto.student.PracticeEvaluationContext;
import cn.wisestar.server.domain.dto.student.RewardContext;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.domain.model.WeakPointEvent;
import cn.wisestar.server.event.WeakPointEventRecorder;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.SectionMapper;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学员学习评价实现。
 *
 * <p>掌握度 = 最近 5 次练习正确率的加权平均（越近权重越高 5,4,3,2,1）；
 * 薄弱 = 掌握度 &lt;55，且仅对已产生学习记录（进度）的知识点认定，未学习实体不参与；攻克 = 正确率 ≥80（显式）。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EvaluationServiceImpl implements EvaluationService {

	/** 薄弱阈值（报告三态的「不稳定」得分需高于此值） */
	public static final int WEAK_THRESHOLD = 55;

	/** 攻克所需本次正确率 */
	private static final int CONQUER_RATE = 80;

	/** 掌握度窗口（最近 N 次练习） */
	private static final int WINDOW = 5;

	private final UserKnowledgeProgressMapper progressMapper;

	private final UserWeakKnowledgeMapper weakMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final SectionMapper sectionMapper;

	private final ChapterMapper chapterMapper;

	private final RewardService rewardService;

	private final WeakPointEventRecorder weakPointEventRecorder;

	@Override
	public void recordPractice(PracticeEvaluationContext context) {
		if (context == null || !StringUtils.hasText(context.getUserId()) || context.getItems() == null
				|| context.getItems().isEmpty()) {
			return;
		}
		String userId = context.getUserId();
		// 同名知识点跨册/单元复用，优先按本次会话章节收敛
		Map<String, String> chapterKpIndex = chapterKnowledgePointIndex(context.getChapterId());
		// 会话级知识点上下文
		KpContext ctx = resolveContext(context.getKnowledgePointId(), context.getSectionId(),
				context.getChapterId());
		if (ctx == null) {
			// 仅带逐题知识点名称时（检测/英语练习），用首个可解析知识点补全学科与版本上下文
			String firstKpId = null;
			for (PracticeEvaluationContext.Item item : context.getItems()) {
				firstKpId = resolveKpId(item.getKnowledgePointNames(), chapterKpIndex, new HashMap<>());
				if (StringUtils.hasText(firstKpId)) {
					break;
				}
			}
			if (StringUtils.hasText(firstKpId)) {
				ctx = resolveContext(firstKpId, null, null);
			}
		}
		if (ctx == null && !StringUtils.hasText(context.getKnowledgePointId())
				&& !StringUtils.hasText(context.getSectionId()) && !StringUtils.hasText(context.getChapterId())) {
			return;
		}
		Map<String, int[]> agg = new LinkedHashMap<>();
		Map<String, String> nameCache = new HashMap<>();
		// 事件来源基键：同一练习会话内的跃迁做幂等；无会话ID（错题订正链路）按日期兜底
		String refBase = StringUtils.hasText(context.getPracticeId()) ? "practice:" + context.getPracticeId()
				: "practice:" + todayKey();
		for (PracticeEvaluationContext.Item item : context.getItems()) {
			String kpId = resolveKpId(item.getKnowledgePointNames(), chapterKpIndex, nameCache);
			if (!StringUtils.hasText(kpId)) {
				kpId = context.getKnowledgePointId();
			}
			if (!StringUtils.hasText(kpId)) {
				continue;
			}
			int[] slot = agg.computeIfAbsent(kpId, k -> new int[3]);
			slot[1]++;
			if (item.isCorrect()) {
				slot[0]++;
			}
			// 三态得分优先（掌握 100 / 不稳定 60 / 漏洞 0）；未提供时按对错折算
			slot[2] += item.getScore() == null ? (item.isCorrect() ? 100 : 0)
					: Math.max(0, Math.min(item.getScore(), 100));
		}
		for (Map.Entry<String, int[]> e : agg.entrySet()) {
			int total = e.getValue()[1];
			if (total <= 0) {
				continue;
			}
			int correct = e.getValue()[0];
			int rate = (int) Math.round(e.getValue()[2] / (double) total);
			updateProgress(userId, e.getKey(), rate, total, correct, refBase);
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
		settleKpMaster(userId, ctx, knowledgePointId);
		markConquered(userId, subjectId, knowledgePointId, "conquer:" + todayKey(), correctRate);
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
		KpContext ctx = resolveContext(knowledgePointId, null);
		UserWeakKnowledge w = findWeak(userId, ctx == null ? null : ctx.subjectId, knowledgePointId);
		if (w == null) {
			return;
		}
		// 薄弱以掌握度阈值为唯一依据：订正后掌握度回升到阈值即移出薄弱
		UserKnowledgeProgress p = findProgress(userId, ctx, knowledgePointId);
		int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
		if (mastery >= WEAK_THRESHOLD) {
			markConquered(userId, ctx == null ? null : ctx.subjectId, knowledgePointId,
					"correction:" + todayKey(), mastery);
		}
	}

	@Override
	public void seedWeakFromBaseline(String userId, String subjectId, String refKey,
			Map<String, Integer> kpMasteryByName) {
		if (!StringUtils.hasText(userId) || !StringUtils.hasText(subjectId)
				|| kpMasteryByName == null || kpMasteryByName.isEmpty()) {
			return;
		}
		String ref = StringUtils.hasText(refKey) ? refKey : "detect";
		Map<String, KnowledgePoint> byName = knowledgePointsBySubject(subjectId);
		for (Map.Entry<String, Integer> entry : kpMasteryByName.entrySet()) {
			String name = entry.getKey();
			if (!StringUtils.hasText(name)) {
				continue;
			}
			KnowledgePoint kp = byName.get(name);
			if (kp == null) {
				continue;
			}
			int mastery = entry.getValue() == null ? 0 : entry.getValue();
			if (mastery >= WEAK_THRESHOLD) {
				continue;
			}
			if (findWeak(userId, subjectId, kp.getId()) != null) {
				continue;
			}
			UserWeakKnowledge w = new UserWeakKnowledge();
			w.setUserId(userId);
			w.setSubjectId(subjectId);
			w.setKnowledgePointId(kp.getId());
			w.setStatus("active");
			w.setConquerTimes(0);
			w.setFirstWeakAt(new Date());
			weakMapper.insert(w);
			weakPointEventRecorder.publish(buildEvent(userId, subjectId, kp.getId(), "discovered",
					mastery, ref + ":" + kp.getId() + ":discovered"));
		}
	}

	/**
	 * 学科下知识点按名称索引（经 章节 → 小节 → 知识点 定位，名称重复取首个）。
	 */
	private Map<String, KnowledgePoint> knowledgePointsBySubject(String subjectId) {
		List<Chapter> chapters = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
				.eq(Chapter::getSubjectId, subjectId));
		if (chapters.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toSet());
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.in(Section::getChapterId, chapterIds));
		if (sections.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toSet());
		return knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.in(KnowledgePoint::getSectionId, sectionIds))
				.stream().filter(k -> StringUtils.hasText(k.getName()))
				.collect(Collectors.toMap(KnowledgePoint::getName, k -> k, (a, b) -> a));
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

	/**
	 * 更新用户对知识点的掌握进度，并据此维护薄弱知识点状态与相关奖励结算。
	 *
	 * @param userId 用户 ID
	 * @param kpId 知识点 ID
	 * @param rate 本次正确率
	 * @param total 本次题目总数
	 * @param correct 本次答对题数
	 * @param refBase 事件幂等引用前缀
	 */
	private void updateProgress(String userId, String kpId, int rate, int total, int correct, String refBase) {
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
		// 薄弱研判：以掌握度阈值为唯一依据；本次已产生学习记录（进度）的知识点才参与认定
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
				weakPointEventRecorder.publish(buildEvent(userId, subjectId, kpId, "discovered",
						mastery, refBase + ":" + kpId + ":discovered"));
			}
			else if (!"active".equals(w.getStatus())) {
				w.setStatus("active");
				weakMapper.updateById(w);
				weakPointEventRecorder.publish(buildEvent(userId, subjectId, kpId, "reopened",
						mastery, refBase + ":" + kpId + ":reopened"));
			}
		}
		else if (w != null && "active".equals(w.getStatus())) {
			// 掌握度回到阈值以上：移出薄弱
			markConquered(userId, subjectId, kpId, refBase, mastery);
		}
		// 掌握度达精通即「消灭知识点」，每学期每知识点结算一次
		settleKpMaster(userId, ctx, kpId);
	}

	/**
	 * 掌握度达精通（≥85）时结算「消灭知识点」奖励，按 {@code {semester}:master:{kpId}} 幂等。
	 */
	private void settleKpMaster(String userId, KpContext ctx, String kpId) {
		if (ctx == null || !StringUtils.hasText(ctx.subjectId)) {
			return;
		}
		UserKnowledgeProgress p = findProgress(userId, ctx, kpId);
		int mastery = p == null || p.getMastery() == null ? 0 : p.getMastery();
		if (mastery < StudentRewardConstants.MASTER_THRESHOLD) {
			return;
		}
		try {
			RewardContext rc = new RewardContext();
			rc.setUserId(userId);
			rc.setSubjectId(ctx.subjectId);
			rc.setKnowledgePointId(kpId);
			rc.setActionType(StudentRewardConstants.ACTION_KP_MASTER);
			rc.setRefId("master:" + kpId);
			rewardService.settle(rc);
		}
		catch (Exception e) {
			log.warn("kp master reward failed: user={}, kp={}", userId, kpId, e);
		}
	}

	/**
	 * 将薄弱知识点标记为已攻克；首次攻克（原先处于 active 或不存在）时发放奖励并记录事件。
	 *
	 * @param userId 用户 ID
	 * @param subjectId 学科 ID
	 * @param kpId 知识点 ID
	 * @param refBase 事件幂等引用前缀
	 * @param mastery 当前掌握度
	 */
	private void markConquered(String userId, String subjectId, String kpId, String refBase, int mastery) {
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
				rc.setRefId("weak:" + kpId);
				rewardService.settle(rc);
			}
			catch (Exception e) {
				log.warn("weak conquer reward failed: user={}, kp={}", userId, kpId, e);
			}
			weakPointEventRecorder.publish(buildEvent(userId, subjectId, kpId, "conquered",
					mastery, refBase + ":" + kpId + ":conquered"));
			settleWeakSection(userId, subjectId, kpId);
		}
	}

	/**
	 * 薄弱小节攻克：该小节不再有 active 薄弱知识点时，终身一次发放小节攻克奖励。
	 */
	private void settleWeakSection(String userId, String subjectId, String kpId) {
		KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
		String sectionId = kp == null ? null : kp.getSectionId();
		if (!StringUtils.hasText(sectionId)) {
			return;
		}
		List<String> sectionKpIds = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.eq(KnowledgePoint::getSectionId, sectionId))
				.stream().map(KnowledgePoint::getId).collect(Collectors.toList());
		if (sectionKpIds.isEmpty()) {
			return;
		}
		Long activeCount = weakMapper.selectCount(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId)
				.eq(UserWeakKnowledge::getStatus, "active")
				.in(UserWeakKnowledge::getKnowledgePointId, sectionKpIds));
		if (activeCount != null && activeCount > 0) {
			return;
		}
		try {
			RewardContext rc = new RewardContext();
			rc.setUserId(userId);
			rc.setSubjectId(subjectId);
			rc.setSectionId(sectionId);
			rc.setActionType(StudentRewardConstants.ACTION_WEAK_SECTION_CONQUER);
			rc.setRefId("weak_section:" + sectionId);
			rewardService.settle(rc);
		}
		catch (Exception e) {
			log.warn("weak section conquer reward failed: user={}, section={}", userId, sectionId, e);
		}
	}

	/**
	 * 构造薄弱点跃迁事件（含知识点/小节/章节名称快照，便于时间线展示）。
	 */
	private WeakPointEvent buildEvent(String userId, String subjectId, String kpId,
			String eventType, int mastery, String refId) {
		WeakPointEvent event = new WeakPointEvent();
		event.setStudentId(userId);
		event.setSubjectId(subjectId);
		event.setKnowledgePointId(kpId);
		event.setEventType(eventType);
		event.setMastery(mastery);
		event.setRefId(refId);
		event.setSource(refId.startsWith("correction:") ? "correction" : "practice");
		event.setOccurredAt(new Date());
		KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
		if (kp != null) {
			event.setKpName(kp.getName());
			event.setSectionId(kp.getSectionId());
			Section section = StringUtils.hasText(kp.getSectionId())
					? sectionMapper.selectById(kp.getSectionId()) : null;
			if (section != null) {
				event.setSectionName(section.getName());
				event.setChapterId(section.getChapterId());
				Chapter chapter = StringUtils.hasText(section.getChapterId())
						? chapterMapper.selectById(section.getChapterId()) : null;
				if (chapter != null) {
					event.setChapterName(chapter.getName());
				}
			}
		}
		return event;
	}

	/** 事件幂等用的日期键 yyyyMMdd。 */
	private String todayKey() {
		return new SimpleDateFormat("yyyyMMdd").format(new Date());
	}

	/**
	 * 章节阶段里程碑：按学科累计「达标章节数」（掌握度 ≥55 的不同章节）结算 1..6 阶段奖励。
	 *
	 * <p>以 {@code {semester}:chapter_stage:{subjectId}:{stage}} 学期幂等，补齐所有已达成阶段，教学内容调整不重复发放也不追回。</p>
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
				rc.setRefId("chapter_stage:" + subjectId + ":" + stage);
				rewardService.settle(rc);
			}
			catch (Exception e) {
				log.warn("chapter stage reward failed: user={}, subject={}, stage={}", userId, subjectId, stage, e);
			}
		}
	}

	/**
	 * 依据知识点名称列表解析出知识点 ID，解析结果通过缓存复用（未命中以空串占位）。
	 *
	 * @param knowledgePointNames 知识点名称列表
	 * @param sessionCtx 会话上下文
	 * @param cache 知识点名称到 ID 的缓存
	 * @return 命中的知识点 ID，未命中返回 null
	 */
	private String resolveKpId(List<String> knowledgePointNames, Map<String, String> chapterKpIndex,
			Map<String, String> cache) {
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
			// 先按会话章节收敛（同名知识点跨年级/册别复用），未命中再回退全局
			String id = chapterKpIndex == null ? null : chapterKpIndex.get(key);
			if (!StringUtils.hasText(id)) {
				List<KnowledgePoint> list = knowledgePointMapper.selectList(
						Wrappers.<KnowledgePoint>lambdaQuery().eq(KnowledgePoint::getName, key).last("limit 1"));
				id = list.isEmpty() ? null : list.get(0).getId();
			}
			cache.put(key, id == null ? "" : id);
			if (StringUtils.hasText(id)) {
				return id;
			}
		}
		return null;
	}

	/**
	 * 构建章节内知识点名称索引（章节 → 小节 → 知识点），用于同名知识点跨册收敛。
	 *
	 * @param chapterId 章节 ID
	 * @return 知识点名称到 ID 的索引，章节为空或无知识点时返回空 Map
	 */
	private Map<String, String> chapterKnowledgePointIndex(String chapterId) {
		if (!StringUtils.hasText(chapterId)) {
			return Collections.emptyMap();
		}
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.eq(Section::getChapterId, chapterId));
		if (sections.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toSet());
		List<KnowledgePoint> list = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
				.in(KnowledgePoint::getSectionId, sectionIds));
		Map<String, String> index = new HashMap<>();
		for (KnowledgePoint kp : list) {
			if (StringUtils.hasText(kp.getName())) {
				index.putIfAbsent(kp.getName().trim(), kp.getId());
			}
		}
		return index;
	}

	/**
	 * 依据知识点 ID 或小节 ID 解析出知识点上下文（章节、学科、版本等）。
	 *
	 * @param kpId 知识点 ID
	 * @param sectionId 小节 ID
	 * @return 解析出的知识点上下文，均无法解析时返回 null
	 */
	private KpContext resolveContext(String kpId, String sectionId) {
		return resolveContext(kpId, sectionId, null);
	}

	/**
	 * 依据知识点 ID、小节 ID 或章节 ID 解析出知识点上下文（章节、学科、版本等）。
	 *
	 * @param kpId 知识点 ID
	 * @param sectionId 小节 ID
	 * @param chapterId 章节 ID
	 * @return 解析出的知识点上下文，均无法解析时返回 null
	 */
	private KpContext resolveContext(String kpId, String sectionId, String chapterId) {
		if (StringUtils.hasText(kpId)) {
			KnowledgePoint kp = knowledgePointMapper.selectById(kpId);
			if (kp != null) {
				return buildContext(kp.getSectionId());
			}
		}
		if (StringUtils.hasText(sectionId)) {
			KpContext bySection = buildContext(sectionId);
			if (bySection != null) {
				return bySection;
			}
		}
		if (StringUtils.hasText(chapterId)) {
			return buildChapterContext(chapterId);
		}
		return null;
	}

	/**
	 * 依据章节 ID 构建知识点上下文（仅到章节层级，不含小节）。
	 *
	 * @param chapterId 章节 ID
	 * @return 知识点上下文，章节不存在时返回 null
	 */
	private KpContext buildChapterContext(String chapterId) {
		Chapter chapter = chapterMapper.selectById(chapterId);
		if (chapter == null) {
			return null;
		}
		KpContext ctx = new KpContext();
		ctx.chapterId = chapterId;
		ctx.subjectId = chapter.getSubjectId();
		ctx.versionId = chapter.getVersion();
		return ctx;
	}

	/**
	 * 依据小节 ID 构建知识点上下文，逐级回溯小节与章节以取得学科和版本。
	 *
	 * @param sectionId 小节 ID
	 * @return 知识点上下文，小节 ID 为空或小节不存在时返回 null
	 */
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

	/**
	 * 查询用户在某知识点上的掌握进度记录。
	 *
	 * @param userId 用户 ID
	 * @param ctx 知识点上下文
	 * @param kpId 知识点 ID
	 * @return 进度记录，上下文为空或查询不到时返回 null
	 */
	private UserKnowledgeProgress findProgress(String userId, KpContext ctx, String kpId) {
		if (ctx == null) {
			return null;
		}
		return progressMapper.selectOne(Wrappers.<UserKnowledgeProgress>lambdaQuery()
				.eq(UserKnowledgeProgress::getUserId, userId).eq(UserKnowledgeProgress::getSubjectId, ctx.subjectId)
				.eq(UserKnowledgeProgress::getVersionId, ctx.versionId)
				.eq(UserKnowledgeProgress::getKnowledgePointId, kpId).last("limit 1"));
	}

	/**
	 * 查询用户指定知识点的薄弱记录。
	 *
	 * @param userId 用户 ID
	 * @param subjectId 学科 ID
	 * @param kpId 知识点 ID
	 * @return 薄弱记录，不存在时返回 null
	 */
	private UserWeakKnowledge findWeak(String userId, String subjectId, String kpId) {
		return weakMapper.selectOne(Wrappers.<UserWeakKnowledge>lambdaQuery()
				.eq(UserWeakKnowledge::getUserId, userId).eq(UserWeakKnowledge::getKnowledgePointId, kpId)
				.eq(subjectId != null, UserWeakKnowledge::getSubjectId, subjectId).last("limit 1"));
	}

	/**
	 * 将逗号分隔的正确率字符串解析为整数列表，忽略无法解析的项。
	 *
	 * @param raw 原始正确率字符串
	 * @return 正确率列表
	 */
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

	/**
	 * 依据掌握度返回对应的掌握等级名称。
	 *
	 * @param mastery 掌握度
	 * @return 掌握等级名称
	 */
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
