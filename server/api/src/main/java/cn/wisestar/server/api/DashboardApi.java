package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.DashboardQuery;
import cn.wisestar.server.domain.dto.DashboardView;
import cn.wisestar.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author javahuang
 * @date 2022/1/28
 */
@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class DashboardApi {

	private final DashboardService dashboardService;

	@GetMapping("/list")
	public List<DashboardView> listDashboard(DashboardQuery query) {
		return dashboardService.listDashboard(query);
	}

}
