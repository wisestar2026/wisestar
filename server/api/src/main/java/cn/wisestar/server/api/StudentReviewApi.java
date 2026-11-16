package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.review.ReviewAnswerCommand;
import cn.wisestar.server.domain.dto.review.ReviewAnswerResult;
import cn.wisestar.server.domain.dto.review.ReviewForecastView;
import cn.wisestar.server.domain.dto.review.ReviewPlanView;
import cn.wisestar.server.domain.dto.review.ReviewQueueItemView;
import cn.wisestar.server.domain.dto.review.ReviewQueueRequest;
import cn.wisestar.server.domain.dto.review.WeakBoardItemView;
import cn.wisestar.server.domain.dto.review.WeakSessionRequest;
import cn.wisestar.server.service.MemoryReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 记忆调度与薄弱攻克接口（学员端）。
 *
 * <p>提供今日复习计划、复习队列、作答入账、到期预测、薄弱看板、攻克训练与幂等回填。
 * 学员只能访问本人数据：userId 由服务端从登录上下文取，接口不接收学员标识。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@RestController
@RequestMapping("${api.prefix}/student")
@RequiredArgsConstructor
public class StudentReviewApi {

	private final MemoryReviewService memoryReviewService;

	/**
	 * 今日复习计划。
	 *
	 * @param subjectId 学科ID，可空
	 * @return 计划视图
	 */
	@GetMapping("/review/plan")
	@PreAuthorize("isAuthenticated()")
	public ReviewPlanView plan(@RequestParam(required = false) String subjectId) {
		return memoryReviewService.plan(SecurityContextUtils.getUserId(), subjectId);
	}

	/**
	 * 复习队列。
	 *
	 * @param request 查询条件
	 * @return 队列条目
	 */
	@PostMapping("/review/queue")
	@PreAuthorize("isAuthenticated()")
	public List<ReviewQueueItemView> queue(@RequestBody(required = false) ReviewQueueRequest request) {
		return memoryReviewService.queue(SecurityContextUtils.getUserId(),
				request == null ? new ReviewQueueRequest() : request);
	}

	/**
	 * 作答入账（新题型可直接传用时与作答方式）。
	 *
	 * @param command 作答入参
	 * @return 调度结果
	 */
	@PostMapping("/review/answer")
	@PreAuthorize("isAuthenticated()")
	public ReviewAnswerResult answer(@RequestBody ReviewAnswerCommand command) {
		return memoryReviewService.recordAnswer(SecurityContextUtils.getUserId(), command);
	}

	/**
	 * 未来到期预测。
	 *
	 * @param subjectId 学科ID，可空
	 * @param days 预测天数
	 * @return 预测视图
	 */
	@GetMapping("/review/forecast")
	@PreAuthorize("isAuthenticated()")
	public ReviewForecastView forecast(@RequestParam(required = false) String subjectId,
			@RequestParam(defaultValue = "30") Integer days) {
		return memoryReviewService.forecast(SecurityContextUtils.getUserId(), subjectId, days);
	}

	/**
	 * 薄弱看板。
	 *
	 * @param subjectId 学科ID，可空
	 * @param limit 条数上限
	 * @return 看板条目
	 */
	@GetMapping("/weak/board")
	@PreAuthorize("isAuthenticated()")
	public List<WeakBoardItemView> weakBoard(@RequestParam(required = false) String subjectId,
			@RequestParam(defaultValue = "50") Integer limit) {
		return memoryReviewService.weakBoard(SecurityContextUtils.getUserId(), subjectId, limit);
	}

	/**
	 * 攻克训练组卷。
	 *
	 * @param request 组卷条件
	 * @return 训练条目
	 */
	@PostMapping("/weak/session")
	@PreAuthorize("isAuthenticated()")
	public List<ReviewQueueItemView> weakSession(@RequestBody(required = false) WeakSessionRequest request) {
		return memoryReviewService.weakSession(SecurityContextUtils.getUserId(),
				request == null ? new WeakSessionRequest() : request);
	}

	/**
	 * 幂等回填既有学习记录到调度表。
	 *
	 * @return 新增条数
	 */
	@PostMapping("/review/backfill")
	@PreAuthorize("isAuthenticated()")
	public int backfill() {
		return memoryReviewService.backfill(SecurityContextUtils.getUserId());
	}

}
