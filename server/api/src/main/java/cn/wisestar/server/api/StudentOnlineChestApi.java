package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudentOnlineChestClaimRequest;
import cn.wisestar.server.domain.dto.student.StudentOnlineChestClaimView;
import cn.wisestar.server.service.OnlineChestService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端在线时长宝箱接口。
 *
 * <p>宝箱状态随 {@code POST /student/study/heartbeat} 返回；本接口用于领取已达档宝箱，
 * 按「学员 + 档位 + 日期」幂等，复用统一奖励账本。</p>
 *
 * @author wisestar
 * @date 2026/9/12
 */
@RestController
@RequestMapping("${api.prefix}/student/online/chest")
@RequiredArgsConstructor
public class StudentOnlineChestApi {

	private final OnlineChestService onlineChestService;

	/**
	 * 领取在线时长宝箱。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/online/chest/claim（如 /api/student/online/chest/claim）。</p>
	 *
	 * <p><b>请求参数</b>：{@link StudentOnlineChestClaimRequest}（tier 档位分钟数 30/60/120；subjectId 可空）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link StudentOnlineChestClaimView}（本次到账学习币 + 最新宝箱状态）。</p>
	 *
	 * @param request 领取请求
	 * @return 领取结果（含最新宝箱状态）
	 */
	@PostMapping("/claim")
	@PreAuthorize("isAuthenticated()")
	public StudentOnlineChestClaimView claim(@RequestBody StudentOnlineChestClaimRequest request) {
		return onlineChestService.claim(SecurityContextUtils.getUserId(),
				request == null ? null : request.getTier(),
				request == null ? null : request.getSubjectId());
	}

}
