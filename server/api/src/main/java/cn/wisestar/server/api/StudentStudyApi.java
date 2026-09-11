package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.student.StudyHeartbeatRequest;
import cn.wisestar.server.domain.dto.student.StudyHeartbeatView;
import cn.wisestar.server.domain.dto.student.StudySummaryView;
import cn.wisestar.server.service.StudySessionService;
import cn.wisestar.server.service.StudySummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 学员学习会话与总结接口。
 *
 * <p>学员端周期性上报心跳以累计学习时长；会话累计满 60 分钟自动生成当日学习总结。
 * 学员可查看本人总结，教师/管理员可查看指定学员总结。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@RestController
@RequestMapping("${api.prefix}/student/study")
@RequiredArgsConstructor
public class StudentStudyApi {

	private final StudySessionService studySessionService;
	private final StudySummaryService studySummaryService;

	/**
	 * 学员学习心跳（每 5 分钟及页面可见性变化时上报）。
	 *
	 * @param request 心跳请求（可携带当前学科ID）
	 * @return 当前会话累计时长与本次是否生成总结
	 */
	@PostMapping("/heartbeat")
	@PreAuthorize("isAuthenticated()")
	public StudyHeartbeatView heartbeat(@RequestBody(required = false) StudyHeartbeatRequest request) {
		return studySessionService.heartbeat(request);
	}

	/**
	 * 学员查看本人当日学习总结。
	 *
	 * @return 当日总结，尚未生成时返回 null
	 */
	@GetMapping("/summary")
	@PreAuthorize("isAuthenticated()")
	public StudySummaryView getMySummary() {
		return studySummaryService.getMySummary();
	}

	/**
	 * 教师/管理员查看指定学员某日学习总结。
	 *
	 * @param studentId 学员ID
	 * @param date      日期 yyyy-MM-dd（默认当天）
	 * @return 该学员总结，尚未生成时返回 null
	 */
	@GetMapping("/summary/student")
	@PreAuthorize("hasAuthority('student:supervision')")
	public StudySummaryView getStudentSummary(@RequestParam String studentId,
			@RequestParam(required = false) String date) {
		return studySummaryService.getStudentSummary(studentId, date);
	}

}
