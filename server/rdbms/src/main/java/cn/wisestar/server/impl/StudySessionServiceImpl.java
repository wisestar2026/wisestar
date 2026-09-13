package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudyHeartbeatRequest;
import cn.wisestar.server.domain.dto.student.StudyHeartbeatView;
import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.domain.model.StudySession;
import cn.wisestar.server.mapper.StudySessionMapper;
import cn.wisestar.server.service.OnlineChestService;
import cn.wisestar.server.service.StudySessionService;
import cn.wisestar.server.service.StudySummaryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 学习会话服务实现。
 *
 * <p>心跳策略：取当日最近一条会话，若距上次心跳不超过 30 分钟则续用并累加时长，
 * 否则新建会话；会话累计时长达到 60 分钟时生成或刷新当日学习总结。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudySessionServiceImpl implements StudySessionService {

	/** 同一会话允许的最大心跳间隔（毫秒） */
	private static final long SESSION_GAP_MS = 30 * 60 * 1000L;

	/** 触发学习总结的累计时长阈值（毫秒） */
	private static final long SUMMARY_THRESHOLD_MS = 60 * 60 * 1000L;

	private final StudySessionMapper studySessionMapper;
	private final StudySummaryService studySummaryService;
	private final OnlineChestService onlineChestService;

	@Override
	@Transactional(rollbackFor = Exception.class)
	public StudyHeartbeatView heartbeat(StudyHeartbeatRequest request) {
		String studentId = SecurityContextUtils.getUserId();
		StudyHeartbeatView view = new StudyHeartbeatView();
		if (studentId == null || studentId.isEmpty()) {
			view.setDurationMs(0L);
			view.setGenerated(false);
			view.setOnlineChest(onlineChestService.view(null));
			return view;
		}

		Date now = new Date();
		String today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
		String subjectId = request == null ? null : request.getSubjectId();

		// 取当日最近一条会话
		List<StudySession> recent = studySessionMapper.selectList(new LambdaQueryWrapper<StudySession>()
				.eq(StudySession::getStudentId, studentId)
				.eq(StudySession::getSessionDate, today)
				.orderByDesc(StudySession::getLastHeartbeatAt)
				.last("limit 1"));
		StudySession session = recent.isEmpty() ? null : recent.get(0);

		long previousDuration = session == null || session.getDurationMs() == null ? 0L : session.getDurationMs();

		boolean sameSession = session != null && session.getLastHeartbeatAt() != null
				&& now.getTime() - session.getLastHeartbeatAt().getTime() <= SESSION_GAP_MS;

		if (sameSession) {
			session.setLastHeartbeatAt(now);
			if (subjectId != null && !subjectId.isEmpty()) {
				session.setSubjectId(subjectId);
			}
			session.setDurationMs(now.getTime() - session.getStartAt().getTime());
			session.setStatus("active");
			studySessionMapper.updateById(session);
		} else {
			session = new StudySession();
			session.setStudentId(studentId);
			session.setSubjectId(subjectId);
			session.setSessionDate(today);
			session.setStartAt(now);
			session.setLastHeartbeatAt(now);
			session.setDurationMs(0L);
			session.setStatus("active");
			studySessionMapper.insert(session);
		}

		long durationMs = session.getDurationMs() == null ? 0L : session.getDurationMs();
		view.setDurationMs(durationMs);
		view.setGenerated(false);

		// 累计达到阈值：首次跨过阈值或当日尚无总结时生成/刷新
		if (durationMs >= SUMMARY_THRESHOLD_MS) {
			boolean crossed = previousDuration < SUMMARY_THRESHOLD_MS;
			StudySummaryView existing = studySummaryService.getStudentSummary(studentId, today);
			if (crossed || existing == null) {
				try {
					studySummaryService.generate(studentId, today, session.getId());
					view.setGenerated(true);
				} catch (Exception e) {
					log.warn("学习总结生成失败：studentId={}, date={}, err={}", studentId, today, e.getMessage());
				}
			}
		}
		view.setOnlineChest(onlineChestService.view(studentId));
		return view;
	}

}
