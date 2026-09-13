package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudentCheckinView;
import cn.wisestar.server.service.CheckinService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员每日签到接口。
 *
 * @author wisestar
 * @date 2026/9/12
 */
@RestController
@RequestMapping("${api.prefix}/student/checkin")
@RequiredArgsConstructor
public class StudentCheckinApi {

	private final CheckinService checkinService;

	/**
	 * 查询当日签到状态。
	 *
	 * @return 签到状态
	 */
	@GetMapping
	@PreAuthorize("isAuthenticated()")
	public StudentCheckinView view() {
		return checkinService.view(SecurityContextUtils.getUserId());
	}

	/**
	 * 领取当日签到奖励。
	 *
	 * @return 领取结果
	 */
	@PostMapping
	@PreAuthorize("isAuthenticated()")
	public StudentCheckinView checkin() {
		return checkinService.checkin(SecurityContextUtils.getUserId());
	}

}
