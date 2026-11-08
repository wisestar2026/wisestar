package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.AiPromptKeys;
import cn.wisestar.server.core.uitls.AiEndpointUtils;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudySession;
import cn.wisestar.server.domain.model.StudySummary;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.StudySessionMapper;
import cn.wisestar.server.mapper.StudySummaryMapper;
import cn.wisestar.server.mapper.UserKnowledgeProgressMapper;
import cn.wisestar.server.mapper.UserLearningRecordMapper;
import cn.wisestar.server.mapper.UserWeakKnowledgeMapper;
import cn.wisestar.server.service.StudySummaryService;
import cn.wisestar.server.service.SystemService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 当日学习总结服务实现。
 *
 * <p>聚合学员当日练习记录、答题明细、学习行为、掌握度与薄弱点数据，优先调用系统 AI 生成
 * 面向家长的总结文本；AI 未启用或调用失败时降级为规则模板，{@code model} 标记为 rule。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudySummaryServiceImpl implements StudySummaryService {

	/** 未配置模型时的兜底模型 */
	private static final String DEFAULT_MODEL = "deepseek-chat";

	private final StudySummaryMapper studySummaryMapper;
	private final StudySessionMapper studySessionMapper;
	private final PracticeRecordMapper practiceRecordMapper;
	private final PracticeDetailMapper practiceDetailMapper;
	private final UserLearningRecordMapper userLearningRecordMapper;
	private final UserKnowledgeProgressMapper userKnowledgeProgressMapper;
	private final UserWeakKnowledgeMapper userWeakKnowledgeMapper;
	private final KnowledgePointMapper knowledgePointMapper;
	private final StudentMapper studentMapper;
	private final SectionMapper sectionMapper;
	private final SystemService systemService;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RestTemplate restTemplate = buildRestTemplate();

	/**
	 * 构建带连接与读取超时配置的 RestTemplate。
	 *
	 * @return RestTemplate 实例
	 */
	private static RestTemplate buildRestTemplate() {
		SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
		factory.setConnectTimeout(15_000);
		factory.setReadTimeout(60_000);
		return new RestTemplate(factory);
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudySummaryView generate(String studentId, String summaryDate, String sessionId) {
		Aggregation agg = aggregate(studentId, summaryDate);

		String content;
		String model;
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		String aiContent = null;
		String aiModel = null;
		if (aiSetting != null && Boolean.TRUE.equals(aiSetting.getEnabled())
				&& aiSetting.getToken() != null && !aiSetting.getToken().trim().isEmpty()) {
			try {
				aiContent = callAi(aiSetting,
						AiPromptKeys.resolve(aiSetting, AiPromptKeys.STUDY_SUMMARY, null),
						buildPrompt(summaryDate, agg));
				aiModel = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
						? aiSetting.getModels().get(0) : DEFAULT_MODEL;
			} catch (Exception e) {
				log.warn("AI 生成学习总结失败，降级规则模板：{}", e.getMessage());
			}
		}
		if (aiContent != null && !aiContent.trim().isEmpty()) {
			content = aiContent.trim();
			model = aiModel;
		} else {
			content = buildRuleContent(summaryDate, agg);
			model = "rule";
		}

		// 按 学员+日期 唯一，存在则覆盖更新
		StudySummary existing = studySummaryMapper.selectOne(new LambdaQueryWrapper<StudySummary>()
				.eq(StudySummary::getStudentId, studentId)
				.eq(StudySummary::getSummaryDate, summaryDate)
				.last("limit 1"));
		StudySummary summary = existing == null ? new StudySummary() : existing;
		summary.setStudentId(studentId);
		summary.setSummaryDate(summaryDate);
		summary.setSessionId(sessionId);
		summary.setContent(content);
		summary.setModel(model);
		summary.setStatus("success");
		if (existing == null) {
			studySummaryMapper.insert(summary);
		} else {
			studySummaryMapper.updateById(summary);
		}
		return toView(summary, agg, studentId);
	}

	@Override
	public StudySummaryView getMySummary() {
		String studentId = SecurityContextUtils.getUserId();
		if (studentId == null || studentId.isEmpty()) {
			return null;
		}
		return getStudentSummary(studentId, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
	}

	@Override
	public StudySummaryView getStudentSummary(String studentId, String date) {
		if (studentId == null || studentId.isEmpty()) {
			return null;
		}
		String summaryDate = (date == null || date.isEmpty())
				? LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) : date;
		StudySummary summary = studySummaryMapper.selectOne(new LambdaQueryWrapper<StudySummary>()
				.eq(StudySummary::getStudentId, studentId)
				.eq(StudySummary::getSummaryDate, summaryDate)
				.last("limit 1"));
		if (summary == null) {
			return null;
		}
		return toView(summary, aggregate(studentId, summaryDate), studentId);
	}

	@Override
	public StudySummaryView preview(String studentId, String date) {
		if (studentId == null || studentId.isEmpty()) {
			return null;
		}
		String summaryDate = (date == null || date.isEmpty())
				? LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) : date;
		Aggregation agg = aggregate(studentId, summaryDate);
		StudySummary summary = studySummaryMapper.selectOne(new LambdaQueryWrapper<StudySummary>()
				.eq(StudySummary::getStudentId, studentId)
				.eq(StudySummary::getSummaryDate, summaryDate)
				.last("limit 1"));
		if (summary != null) {
			return toView(summary, agg, studentId);
		}
		StudySummary tmp = new StudySummary();
		tmp.setStudentId(studentId);
		tmp.setSummaryDate(summaryDate);
		tmp.setContent(buildRuleContent(summaryDate, agg));
		tmp.setModel("rule");
		tmp.setStatus("preview");
		return toView(tmp, agg, studentId);
	}

	@Override
	public String aiText(String systemPrompt, String userPrompt) {
		SystemInfo.AiSetting aiSetting = systemService.getSystemAiSetting();
		if (aiSetting == null || !Boolean.TRUE.equals(aiSetting.getEnabled())
				|| aiSetting.getToken() == null || aiSetting.getToken().trim().isEmpty()) {
			return null;
		}
		try {
			String content = callAi(aiSetting, systemPrompt, userPrompt);
			return (content != null && !content.trim().isEmpty()) ? content.trim() : null;
		} catch (Exception e) {
			log.warn("AI 文本生成失败：{}", e.getMessage());
			return null;
		}
	}

	// ------------------------------------------------------------------ 聚合

	/** 汇总当日学习数据 */
	private Aggregation aggregate(String studentId, String summaryDate) {
		Aggregation agg = new Aggregation();
		Date[] range = dayRange(summaryDate);
		Date start = range[0];
		Date end = range[1];

		List<PracticeRecord> records = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
				.eq(PracticeRecord::getUserId, studentId)
				.ge(PracticeRecord::getCreateAt, start)
				.lt(PracticeRecord::getCreateAt, end));
		agg.practiceCount = records.size();
		for (PracticeRecord r : records) {
			agg.questionCount += r.getTotalQuestions() == null ? 0 : r.getTotalQuestions();
			agg.correctCount += r.getCorrectCount() == null ? 0 : r.getCorrectCount();
			agg.practiceDurationMs += r.getDurationMs() == null ? 0L : r.getDurationMs();
		}
		if (!records.isEmpty()) {
			List<String> practiceIds = records.stream().map(PracticeRecord::getId).collect(Collectors.toList());
			Long wrong = practiceDetailMapper.selectCount(new LambdaQueryWrapper<PracticeDetail>()
					.in(PracticeDetail::getPracticeId, practiceIds)
					.eq(PracticeDetail::getIsCorrect, 0));
			agg.wrongCount = wrong == null ? 0L : wrong;
		}

		// 学习行为（积分/学币）
		List<UserLearningRecord> behaviors = userLearningRecordMapper.selectList(
				new LambdaQueryWrapper<UserLearningRecord>()
						.eq(UserLearningRecord::getUserId, studentId)
						.ge(UserLearningRecord::getLearnedAt, start)
						.lt(UserLearningRecord::getLearnedAt, end));
		for (UserLearningRecord b : behaviors) {
			agg.points += b.getPoints() == null ? 0 : b.getPoints();
			agg.coins += b.getCoins() == null ? 0 : b.getCoins();
		}

		// 掌握度
		List<UserKnowledgeProgress> progresses = userKnowledgeProgressMapper.selectList(
				new LambdaQueryWrapper<UserKnowledgeProgress>()
						.eq(UserKnowledgeProgress::getUserId, studentId)
						.ge(UserKnowledgeProgress::getLastPracticeAt, start)
						.lt(UserKnowledgeProgress::getLastPracticeAt, end));
		agg.knowledgeCount = progresses.size();
		if (!progresses.isEmpty()) {
			int sum = 0;
			for (UserKnowledgeProgress p : progresses) {
				sum += p.getMastery() == null ? 0 : p.getMastery();
			}
			agg.avgMastery = sum / progresses.size();
		}

		// 当日强化的知识点（当日有练习进度的知识点名称）
		agg.strengthenedNames = new ArrayList<>();
		if (!progresses.isEmpty()) {
			Set<String> progressKpIds = progresses.stream().map(UserKnowledgeProgress::getKnowledgePointId)
					.filter(id -> id != null && !id.isEmpty()).collect(Collectors.toSet());
			if (!progressKpIds.isEmpty()) {
				Map<String, String> progressNameMap = new HashMap<>();
				for (KnowledgePoint kp : knowledgePointMapper.selectBatchIds(progressKpIds)) {
					progressNameMap.put(kp.getId(), kp.getName());
				}
				for (UserKnowledgeProgress p : progresses) {
					String name = progressNameMap.get(p.getKnowledgePointId());
					if (name != null) {
						agg.strengthenedNames.add(shortName(name));
					}
				}
			}
		}

		// 薄弱知识点
		List<UserWeakKnowledge> weaks = userWeakKnowledgeMapper.selectList(
				new LambdaQueryWrapper<UserWeakKnowledge>()
						.eq(UserWeakKnowledge::getUserId, studentId)
						.eq(UserWeakKnowledge::getStatus, "active"));
		agg.weakNames = new ArrayList<>();
		if (!weaks.isEmpty()) {
			Set<String> kpIds = weaks.stream().map(UserWeakKnowledge::getKnowledgePointId)
					.filter(id -> id != null && !id.isEmpty()).collect(Collectors.toSet());
			if (!kpIds.isEmpty()) {
				Map<String, String> nameMap = new HashMap<>();
				for (KnowledgePoint kp : knowledgePointMapper.selectBatchIds(kpIds)) {
					nameMap.put(kp.getId(), kp.getName());
				}
				for (UserWeakKnowledge w : weaks) {
					String name = nameMap.get(w.getKnowledgePointId());
					if (name != null) {
						agg.weakNames.add(shortName(name));
					}
				}
			}
		}

		// 本次学习涉及的单元/小节（家长版叙述用，最多取 3 个）
		Set<String> sectionIds = records.stream().map(PracticeRecord::getSectionId)
				.filter(StringUtils::hasText).collect(Collectors.toCollection(java.util.LinkedHashSet::new));
		if (!sectionIds.isEmpty()) {
			List<Section> sections = sectionMapper.selectBatchIds(sectionIds);
			agg.topicLabel = sections.stream().map(Section::getName).filter(StringUtils::hasText)
					.map(StudySummaryServiceImpl::shortName).limit(3).collect(Collectors.joining("、"));
		}

		// 上一次学习日的正确率，用于「比上次提高 N 个百分点」
		List<PracticeRecord> history = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
				.eq(PracticeRecord::getUserId, studentId)
				.lt(PracticeRecord::getCreateAt, start)
				.orderByDesc(PracticeRecord::getCreateAt)
				.last("limit 100"));
		if (!history.isEmpty()) {
			ZoneId zone = ZoneId.systemDefault();
			LocalDate prevDate = history.get(0).getCreateAt().toInstant().atZone(zone).toLocalDate();
			Date[] prevRange = dayRange(prevDate.toString());
			int prevTotal = 0;
			int prevCorrect = 0;
			for (PracticeRecord r : history) {
				Date at = r.getCreateAt();
				if (at == null || at.before(prevRange[0]) || !at.before(prevRange[1])) {
					continue;
				}
				prevTotal += r.getTotalQuestions() == null ? 0 : r.getTotalQuestions();
				prevCorrect += r.getCorrectCount() == null ? 0 : r.getCorrectCount();
			}
			if (prevTotal > 0) {
				agg.lastStudyDate = prevDate.toString();
				agg.lastAccuracy = (int) Math.round(prevCorrect * 100.0 / prevTotal);
			}
		}
		if (agg.questionCount > 0) {
			int currentAccuracy = (int) Math.round(agg.correctCount * 100.0 / agg.questionCount);
			// 上次正确率低于 20% 时基数是偶然命中，不构成可比基准：不做「提高 N 个百分点」的结论
			agg.hasComparison = agg.lastStudyDate != null && agg.lastAccuracy >= 20;
			agg.accuracyDelta = agg.hasComparison ? currentAccuracy - agg.lastAccuracy : 0;
		}

		// 近 7 天（含当日）练习汇总
		Date weekStart = Date.from(LocalDate.parse(summaryDate, DateTimeFormatter.ISO_LOCAL_DATE)
				.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant());
		List<PracticeRecord> weekRecords = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
				.eq(PracticeRecord::getUserId, studentId)
				.ge(PracticeRecord::getCreateAt, weekStart)
				.lt(PracticeRecord::getCreateAt, end));
		agg.weekPracticeCount = weekRecords.size();
		for (PracticeRecord r : weekRecords) {
			agg.weekQuestionCount += r.getTotalQuestions() == null ? 0 : r.getTotalQuestions();
			agg.weekCorrectCount += r.getCorrectCount() == null ? 0 : r.getCorrectCount();
		}
		if (agg.weekQuestionCount > 0) {
			agg.weekAccuracy = (int) Math.round(agg.weekCorrectCount * 100.0 / agg.weekQuestionCount);
		}

		// 已攻克薄弱点（历史累计，名称最多回填 3 个）
		List<UserWeakKnowledge> conquered = userWeakKnowledgeMapper.selectList(
				new LambdaQueryWrapper<UserWeakKnowledge>()
						.eq(UserWeakKnowledge::getUserId, studentId)
						.eq(UserWeakKnowledge::getStatus, "conquered"));
		agg.conqueredCount = conquered.size();
		if (!conquered.isEmpty()) {
			Set<String> conqueredIds = conquered.stream().map(UserWeakKnowledge::getKnowledgePointId)
					.filter(StringUtils::hasText).collect(Collectors.toSet());
			if (!conqueredIds.isEmpty()) {
				List<String> names = new ArrayList<>();
				for (KnowledgePoint kp : knowledgePointMapper.selectBatchIds(conqueredIds)) {
					if (StringUtils.hasText(kp.getName())) {
						names.add(shortName(kp.getName()));
					}
				}
				agg.conqueredNames = names.stream().filter(StringUtils::hasText).distinct()
						.limit(3).collect(Collectors.toList());
			}
		}

		// 连续学习天数（以当日为终点往前数，任一天有练习记录即算）
		LocalDate cursor = LocalDate.parse(summaryDate, DateTimeFormatter.ISO_LOCAL_DATE);
		Set<LocalDate> studyDates = practiceRecordMapper.selectList(new LambdaQueryWrapper<PracticeRecord>()
						.eq(PracticeRecord::getUserId, studentId)
						.ge(PracticeRecord::getCreateAt, Date.from(cursor.minusDays(59).atStartOfDay(ZoneId.systemDefault()).toInstant()))
						.lt(PracticeRecord::getCreateAt, end)).stream()
				.map(r -> r.getCreateAt() == null ? null
						: r.getCreateAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
				.filter(java.util.Objects::nonNull).collect(Collectors.toSet());
		while (studyDates.contains(cursor)) {
			agg.streakDays++;
			cursor = cursor.minusDays(1);
		}

		// 当日累计在线时长（全部会话）
		List<StudySession> sessions = studySessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
				.eq(StudySession::getStudentId, studentId)
				.eq(StudySession::getSessionDate, summaryDate));
		long total = 0L;
		for (StudySession s : sessions) {
			total += s.getDurationMs() == null ? 0L : s.getDurationMs();
		}
		agg.onlineDurationMs = total;
		return agg;
	}

	// ------------------------------------------------------------------ 文本生成

	/**
	 * 依据当日学习数据聚合结果构建发送给大模型的提示词。
	 *
	 * @param summaryDate 总结日期
	 * @param agg 当日学习数据聚合结果
	 * @return 提示词文本
	 */
	private String buildPrompt(String summaryDate, Aggregation agg) {
		StringBuilder sb = new StringBuilder();
		sb.append("日期：").append(summaryDate).append("\n");
		sb.append("在线学习时长：").append(minutes(agg.onlineDurationMs)).append(" 分钟\n");
		sb.append("练习次数：").append(agg.practiceCount).append("，题量：").append(agg.questionCount)
				.append("，答对：").append(agg.correctCount).append("，错题：").append(agg.wrongCount).append("\n");
		sb.append("获得学海积分：").append(agg.points).append("，学习币：").append(agg.coins).append("\n");
		sb.append("涉及知识点：").append(agg.knowledgeCount).append(" 个，平均掌握度：").append(agg.avgMastery).append("%\n");
		if (StringUtils.hasText(agg.topicLabel)) {
			sb.append("本次学习单元/小节：").append(agg.topicLabel).append("\n");
		}
		if (agg.hasComparison) {
			sb.append("上一次学习日期：").append(agg.lastStudyDate).append("，当时正确率：")
					.append(agg.lastAccuracy).append("%，本次正确率比上次").append(agg.accuracyDelta >= 0 ? "提高" : "下降")
					.append(Math.abs(agg.accuracyDelta)).append(" 个百分点");
			if (agg.lastAccuracy < 20) {
				sb.append("（上次基数很低，不要写成大幅进步，可写「刚开始练、正在找到感觉」）");
			}
			sb.append("\n");
		} else {
			sb.append("本次是近期第一次练习，没有历史对比数据\n");
		}
		sb.append("近 7 天：练习 ").append(agg.weekPracticeCount).append(" 次，共 ").append(agg.weekQuestionCount)
				.append(" 题，正确率 ").append(agg.weekAccuracy).append("%\n");
		sb.append("连续学习天数：").append(agg.streakDays).append(" 天\n");
		if (!agg.weakNames.isEmpty()) {
			sb.append("薄弱知识点：").append(String.join("、", agg.weakNames)).append("\n");
		}
		sb.append("\n写作要求：\n")
				.append("1. 写三段话，每段一到两句，总字数控制在 100~180 字，用一句换行分隔。\n")
				.append("2. 第一段写「今天学了什么」，用单元/小节或口语化描述，不要堆知识点术语。\n")
				.append("3. 第二段写「进步在哪」，必须引用上面给出的前后对比数据"
						+ "（如正确率比上次提高几个百分点、攻克了哪个知识点），没有对比数据时用今天全对/掌握扎实的事实。\n")
				.append("4. 第三段写「下一步一个小目标」，给出具体可执行的动作，并带一句鼓励。\n")
				.append("5. 禁止出现：积分、学习币、错题率、薄弱知识点、掌握度百分比、答错几道。\n")
				.append("6. 不要标题、不要 Markdown、不要分点符号，直接输出三段正文。\n");
		if (!agg.weakNames.isEmpty()) {
			sb.append("可用作「下一步」的知识点（请改写成口语描述）：")
					.append(String.join("、", agg.weakNames.stream().limit(2).collect(Collectors.toList())))
					.append("\n");
		}
		if (agg.conqueredCount > 0) {
			sb.append("已攻克薄弱点：").append(agg.conqueredCount).append(" 个")
					.append(agg.conqueredNames.isEmpty() ? "" : "（" + String.join("、", agg.conqueredNames) + "）")
					.append("\n");
		}
		return sb.toString();
	}

	/**
	 * 依据当日学习数据聚合结果生成规则模板总结文案（AI 不可用时的降级内容）。
	 *
	 * @param summaryDate 总结日期
	 * @param agg 当日学习数据聚合结果
	 * @return 规则模板总结文本
	 */
	private String buildRuleContent(String summaryDate, Aggregation agg) {
		StringBuilder sb = new StringBuilder();

		// 零学习日：不罗列 0 值，改用周视角 + 鼓励，避免家长误解系统异常
		if (agg.questionCount == 0) {
			sb.append("今天孩子没有进行在线练习，我们把这一天记为休息日。");
			if (agg.weekQuestionCount > 0) {
				sb.append("回顾这一周，孩子完成了 ").append(agg.weekPracticeCount).append(" 次练习共 ")
						.append(agg.weekQuestionCount).append(" 题，正确率 ").append(agg.weekAccuracy).append("%");
				if (agg.conqueredCount > 0) {
					sb.append("，已经攻克了 ").append(agg.conqueredNames.isEmpty()
							? agg.conqueredCount + " 个薄弱知识点"
							: String.join("、", agg.conqueredNames) + " 等 " + agg.conqueredCount + " 个知识点");
				}
				sb.append("。休息是为了更好地出发。");
			} else {
				sb.append("明天我们从一个小目标开始，先做 5 道题找找手感就好。");
			}
			return sb.toString();
		}

		int accuracy = (int) Math.round(agg.correctCount * 100.0 / agg.questionCount);
		String topic = StringUtils.hasText(agg.topicLabel) ? agg.topicLabel : "今天的练习内容";

		// 第一段：学了什么
		sb.append("今天孩子完成了 ").append(topic);
		if (agg.questionCount > 0) {
			sb.append("，一共 ").append(agg.questionCount).append(" 道题，答对 ").append(agg.correctCount).append(" 道");
		}
		sb.append("。");

		// 第二段：进步在哪（优先用对比数据，其次用攻克/熟练证据）
		StringBuilder progress = new StringBuilder();
		// 只有上次已形成可比基准（≥20%）时才声明「提高 N 个百分点」，
		// 避免从 0% 起步时写出「提高 67 个百分点」这类失真表述。
		if (agg.hasComparison && agg.lastAccuracy >= 20 && agg.accuracyDelta >= 5) {
			progress.append("正确率 ").append(accuracy).append("%，比上一次提高了 ")
					.append(agg.accuracyDelta).append(" 个百分点");
		} else if (agg.hasComparison && agg.lastAccuracy >= 20 && agg.accuracyDelta <= -5) {
			progress.append("正确率 ").append(accuracy).append("%，这次略有起伏，属于正常波动");
		} else if (agg.hasComparison && agg.lastAccuracy >= 20) {
			progress.append("正确率 ").append(accuracy).append("%，和上一次基本持平");
		} else {
			progress.append("正确率 ").append(accuracy).append("%");
		}
		if (agg.wrongCount == 0) {
			progress.append("，这次全部答对");
		}
		if (!agg.conqueredNames.isEmpty()) {
			progress.append("；之前重点练的「").append(String.join("」「", agg.conqueredNames))
					.append("」已经拿下");
		}
		if (!agg.strengthenedNames.isEmpty()) {
			progress.append("；今天在「").append(String.join("」「", agg.strengthenedNames.stream().limit(2)
					.collect(Collectors.toList()))).append("」上练得比较扎实");
		}
		sb.append(progress).append("。");

		// 第三段：下一步 + 鼓励
		if (!agg.weakNames.isEmpty()) {
			sb.append("接下来重点攻克「").append(String.join("」「", agg.weakNames.stream().limit(2)
					.collect(Collectors.toList()))).append("」，每天几道题，慢慢就稳了。");
		} else {
			sb.append("目前没有明显吃力的地方，接下来保持现在的节奏，适当加一点难度。");
		}
		if (agg.streakDays >= 2) {
			sb.append("已经连续学习 ").append(agg.streakDays).append(" 天，这份坚持很值得肯定。");
		} else {
			sb.append("今天的状态不错，明天继续保持。");
		}
		return sb.toString();
	}

	/** 调用大模型生成总结文本；异常向上抛出由调用方降级。 */
	private String callAi(SystemInfo.AiSetting aiSetting, String systemPrompt, String userPrompt) throws Exception {
		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0) : DEFAULT_MODEL;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(aiSetting.getToken().trim());

		List<Map<String, String>> messages = new ArrayList<>();
		Map<String, String> system = new HashMap<>();
		system.put("role", "system");
		system.put("content", systemPrompt);
		messages.add(system);
		Map<String, String> user = new HashMap<>();
		user.put("role", "user");
		user.put("content", userPrompt);
		messages.add(user);

		Map<String, Object> body = new HashMap<>();
		body.put("model", model);
		body.put("messages", messages);
		body.put("temperature", 0.5);
		body.put("max_tokens", 800);
		body.put("stream", false);

		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		JsonNode resp = objectMapper.readTree(restTemplate.postForObject(
				AiEndpointUtils.resolveChatUrl(aiSetting.getBaseUrl()), request, String.class));
		JsonNode choices = resp.path("choices");
		if (choices.isArray() && choices.size() > 0) {
			String content = choices.get(0).path("message").path("content").asText("");
			if (!content.trim().isEmpty()) {
				return content;
			}
		}
		throw new IllegalStateException("AI 返回内容为空");
	}

	// ------------------------------------------------------------------ 工具

	/**
	 * 将学习总结实体与聚合数据组装为前端展示视图。
	 *
	 * @param summary 学习总结实体
	 * @param agg 当日学习数据聚合结果
	 * @param studentId 学员 ID
	 * @return 学习总结视图
	 */
	private StudySummaryView toView(StudySummary summary, Aggregation agg, String studentId) {
		StudySummaryView view = new StudySummaryView();
		view.setId(summary.getId());
		view.setStudentId(studentId);
		view.setSummaryDate(summary.getSummaryDate());
		view.setSessionId(summary.getSessionId());
		view.setContent(summary.getContent());
		// 家长版固定用规则模板重新生成，保证历史记录也按新口径叙述（不依赖旧 AI 文案）
		view.setParentContent(agg == null ? summary.getContent() : buildRuleContent(summary.getSummaryDate(), agg));
		view.setModel(summary.getModel());
		view.setStatus(summary.getStatus());
		if (agg != null) {
			view.setDurationMs(agg.onlineDurationMs);
			view.setPracticeCount(agg.practiceCount);
			view.setQuestionCount(agg.questionCount);
			view.setCorrectCount(agg.correctCount);
			view.setAccuracy(agg.questionCount == 0 ? 0
					: (int) Math.round(agg.correctCount * 100.0 / agg.questionCount));
			view.setWrongCount((int) agg.wrongCount);
			view.setPoints(agg.points);
			view.setCoins(agg.coins);
			view.setKnowledgeCount(agg.knowledgeCount);
			view.setAvgMastery(agg.avgMastery);
			view.setWeakNames(agg.weakNames);
			view.setStrengthenedNames(agg.strengthenedNames);
			view.setLastStudyDate(agg.lastStudyDate);
			view.setLastAccuracy(agg.lastAccuracy);
			view.setAccuracyDelta(agg.accuracyDelta);
			view.setHasComparison(agg.hasComparison);
			view.setWeekPracticeCount(agg.weekPracticeCount);
			view.setWeekQuestionCount(agg.weekQuestionCount);
			view.setWeekAccuracy(agg.weekAccuracy);
			view.setConqueredCount(agg.conqueredCount);
			view.setConqueredNames(agg.conqueredNames);
			view.setStreakDays(agg.streakDays);
			view.setTopicLabel(agg.topicLabel);
		}
		Student student = studentMapper.selectById(studentId);
		if (student != null) {
			view.setStudentName(student.getName());
		}
		if (summary.getCreateAt() != null) {
			view.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(summary.getCreateAt()));
		}
		return view;
	}

	/**
	 * 计算指定日期在系统时区下的起止时间区间。
	 *
	 * @param summaryDate 日期字符串（yyyy-MM-dd）
	 * @return 长度为 2 的数组，依次为当日 00:00:00 与次日 00:00:00
	 */
	private Date[] dayRange(String summaryDate) {
		LocalDate date = LocalDate.parse(summaryDate, DateTimeFormatter.ISO_LOCAL_DATE);
		ZoneId zone = ZoneId.systemDefault();
		Date start = Date.from(date.atStartOfDay(zone).toInstant());
		Date end = Date.from(date.plusDays(1).atStartOfDay(zone).toInstant());
		return new Date[] { start, end };
	}

	/**
	 * 知识点/单元名称过长时截断为口语化短名，避免家长版出现整段规则描述。
	 *
	 * @param name 原始名称
	 * @return 截断后的短名（最多 12 个字符）
	 */
	private static String shortName(String name) {
		if (!StringUtils.hasText(name)) {
			return "";
		}
		String cleaned = name.replaceAll("\\s+", " ").trim();
		int colon = cleaned.indexOf('：');
		if (colon > 0 && colon <= 12) {
			cleaned = cleaned.substring(0, colon);
		}
		return cleaned.length() <= 12 ? cleaned : cleaned.substring(0, 12);
	}

	/**
	 * 将毫秒数换算为分钟数（四舍五入）。
	 *
	 * @param ms 毫秒数
	 * @return 分钟数
	 */
	private long minutes(long ms) {
		return Math.round(ms / 60000.0);
	}

	/** 当日学习数据聚合结果 */
	private static class Aggregation {
		private int practiceCount;
		private int questionCount;
		private int correctCount;
		private long wrongCount;
		private long practiceDurationMs;
		private int points;
		private int coins;
		private int knowledgeCount;
		private int avgMastery;
		private long onlineDurationMs;
		private List<String> weakNames = Collections.emptyList();
		private List<String> strengthenedNames = Collections.emptyList();
		private String lastStudyDate;
		private int lastAccuracy;
		private int accuracyDelta;
		private boolean hasComparison;
		private int weekPracticeCount;
		private int weekQuestionCount;
		private int weekCorrectCount;
		private int weekAccuracy;
		private int conqueredCount;
		private List<String> conqueredNames = Collections.emptyList();
		private int streakDays;
		private String topicLabel;
	}

}
