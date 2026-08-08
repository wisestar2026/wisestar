package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.DashboardQuery;
import cn.wisestar.server.domain.dto.DashboardRequest;
import cn.wisestar.server.domain.dto.DashboardView;

import java.util.List;

/**
 * 仪表盘服务接口（DashboardService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供用户自定义仪表盘（首页/项目概要页布局配置）的查询与
 * 保存能力。仪表盘内容为前端可配置的卡片/模块布局。实现类位于 rdbms 模块
 * （DashboardServiceImpl）。</p>
 *
 * @author javahuang
 * @date 2022/1/28
 */
public interface DashboardService {

	/**
	 * 查询仪表盘列表。
	 *
	 * @param query 查询条件（如按用户、仪表盘类型过滤，见 {@link DashboardQuery}）
	 * @return 仪表盘视图列表
	 */
	List<DashboardView> listDashboard(DashboardQuery query);

	/**
	 * 批量保存仪表盘配置（整单覆盖式保存）。
	 *
	 * @param request 仪表盘配置请求列表（见 {@link DashboardRequest}）
	 */
	void saveDashboard(List<DashboardRequest> request);

}
