package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.SystemInfo;
import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.PracticeDetail;
import cn.wisestar.server.domain.model.PracticeRecord;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudySession;
import cn.wisestar.server.domain.model.StudySummary;
import cn.wisestar.server.domain.model.UserKnowledgeProgress;
import cn.wisestar.server.domain.model.UserLearningRecord;
import cn.wisestar.server.domain.model.UserWeakKnowledge;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.PracticeDetailMapper;
import cn.wisestar.server.mapper.PracticeRecordMapper;
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

	/** SiliconFlow 对话补全接口（与 AI 问答同一套系统设置） */
	private static final String CHAT_URL = "https://api.siliconflow.cn/v1/chat/completions";

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
	private final SystemService systemService;

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final RestTemplate restTemplate = buildRestTemplate();

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
				aiContent = callAi(aiSetting, buildPrompt(summaryDate, agg));
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
						agg.weakNames.add(name);
					}
				}
			}
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

	private String buildPrompt(String summaryDate, Aggregation agg) {
		StringBuilder sb = new StringBuilder();
		sb.append("日期：").append(summaryDate).append("\n");
		sb.append("在线学习时长：").append(minutes(agg.onlineDurationMs)).append(" 分钟\n");
		sb.append("练习次数：").append(agg.practiceCount).append("，题量：").append(agg.questionCount)
				.append("，答对：").append(agg.correctCount).append("，错题：").append(agg.wrongCount).append("\n");
		sb.append("获得学海积分：").append(agg.points).append("，学习币：").append(agg.coins).append("\n");
		sb.append("涉及知识点：").append(agg.knowledgeCount).append(" 个，平均掌握度：").append(agg.avgMastery).append("%\n");
		if (!agg.weakNames.isEmpty()) {
			sb.append("薄弱知识点：").append(String.join("、", agg.weakNames)).append("\n");
		}
		sb.append("\n请用 200 字以内的中文，面向家长口吻，总结该学员今日学习表现，"
				+ "指出进步与薄弱点，并给出一条具体可执行的改进建议。直接输出总结正文，不要标题与 Markdown。");
		return sb.toString();
	}

	private String buildRuleContent(String summaryDate, Aggregation agg) {
		int accuracy = agg.questionCount == 0 ? 0 : (int) Math.round(agg.correctCount * 100.0 / agg.questionCount);
		StringBuilder sb = new StringBuilder();
		sb.append("【").append(summaryDate).append(" 学习总结】");
		sb.append("今日在线学习约 ").append(minutes(agg.onlineDurationMs)).append(" 分钟，");
		sb.append("完成练习 ").append(agg.practiceCount).append(" 次，共 ").append(agg.questionCount)
				.append(" 题，答对 ").append(agg.correctCount).append(" 题，正确率 ").append(accuracy).append("%。");
		if (agg.wrongCount > 0) {
			sb.append("存在错题 ").append(agg.wrongCount).append(" 道，建议及时订正。");
		}
		sb.append("今日获得学海积分 ").append(agg.points).append("、学习币 ").append(agg.coins).append("。");
		if (agg.knowledgeCount > 0) {
			sb.append("覆盖知识点 ").append(agg.knowledgeCount).append(" 个，平均掌握度 ")
					.append(agg.avgMastery).append("%。");
		}
		if (!agg.weakNames.isEmpty()) {
			sb.append("当前薄弱知识点：").append(String.join("、", agg.weakNames)).append("，建议针对性巩固。");
		} else {
			sb.append("暂无明显薄弱知识点，保持当前节奏继续努力。");
		}
		return sb.toString();
	}

	/** 调用大模型生成总结文本；异常向上抛出由调用方降级。 */
	private String callAi(SystemInfo.AiSetting aiSetting, String userPrompt) throws Exception {
		String model = (aiSetting.getModels() != null && !aiSetting.getModels().isEmpty())
				? aiSetting.getModels().get(0) : DEFAULT_MODEL;
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(aiSetting.getToken().trim());

		List<Map<String, String>> messages = new ArrayList<>();
		Map<String, String> system = new HashMap<>();
		system.put("role", "system");
		system.put("content", "你是一位耐心细致的学习教练，擅长用简洁、鼓励的语言向家长反馈孩子的学习情况。");
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
		JsonNode resp = objectMapper.readTree(restTemplate.postForObject(CHAT_URL, request, String.class));
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

	private StudySummaryView toView(StudySummary summary, Aggregation agg, String studentId) {
		StudySummaryView view = new StudySummaryView();
		view.setId(summary.getId());
		view.setStudentId(studentId);
		view.setSummaryDate(summary.getSummaryDate());
		view.setSessionId(summary.getSessionId());
		view.setContent(summary.getContent());
		view.setModel(summary.getModel());
		view.setStatus(summary.getStatus());
		view.setDurationMs(agg == null ? null : agg.onlineDurationMs);
		Student student = studentMapper.selectById(studentId);
		if (student != null) {
			view.setStudentName(student.getName());
		}
		if (summary.getCreateAt() != null) {
			view.setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm").format(summary.getCreateAt()));
		}
		return view;
	}

	private Date[] dayRange(String summaryDate) {
		LocalDate date = LocalDate.parse(summaryDate, DateTimeFormatter.ISO_LOCAL_DATE);
		ZoneId zone = ZoneId.systemDefault();
		Date start = Date.from(date.atStartOfDay(zone).toInstant());
		Date end = Date.from(date.plusDays(1).atStartOfDay(zone).toInstant());
		return new Date[] { start, end };
	}

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
	}

}
