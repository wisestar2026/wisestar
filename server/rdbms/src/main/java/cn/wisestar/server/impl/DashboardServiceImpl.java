package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.DashboardQuery;
import cn.wisestar.server.domain.dto.DashboardRequest;
import cn.wisestar.server.domain.dto.DashboardView;
import cn.wisestar.server.domain.mapper.DashboardViewMapper;
import cn.wisestar.server.domain.model.Dashboard;
import cn.wisestar.server.mapper.DashboardMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.DashboardService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 *
 */
@Service
@Transactional(rollbackFor = Exception.class)
@RequiredArgsConstructor
public class DashboardServiceImpl extends BaseService<DashboardMapper, Dashboard> implements DashboardService {

	private DashboardViewMapper dashboardViewMapper;

	@Override
	public List<DashboardView> listDashboard(DashboardQuery query) {
		List<Dashboard> dashboardList = list(Wrappers.<Dashboard>lambdaQuery().eq(Dashboard::getType, query.getType())
				.eq(query.getProjectId() != null, Dashboard::getProjectId, query.getProjectId()));
		return dashboardViewMapper.toView(dashboardList);
	}

	@Override
	public void saveDashboard(List<DashboardRequest> request) {

	}

}
