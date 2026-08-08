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
 * 数据看板接口（DashboardApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供数据看板/统计图表的数据查询接口——当前仅包含看板列表查询
 * （按条件返回一组统计卡片/图表数据，如答卷量趋势、题目完成度等）。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/dashboard}（api.prefix 通常为 /api），
 * 当前方法路径为 ${api.prefix}/dashboard/list。</p>
 * <p><b>被谁调用</b>：前端管理后台"数据看板"页面（需登录，是否要求权限点由全局安全规则控制）。</p>
 * <p><b>依赖的服务</b>：注入 {@link DashboardService}（shared 模块接口，rdbms 模块实现）——
 * 负责从答卷/项目/题目等表聚合统计看板数据。</p>
 *
 * <p><b>数据流</b>：前端 GET /api/dashboard/list?projectId=xxx → 本类
 * listDashboard(DashboardQuery) → DashboardService#listDashboard → rdbms 实现
 * → 多个 Mapper 聚合统计 → List&lt;DashboardView&gt; → JSON。</p>
 *
 * @author javahuang
 * @date 2022/1/28
 */
@RestController
@RequestMapping("${api.prefix}/dashboard")
@RequiredArgsConstructor
public class DashboardApi {

	/**
	 * 看板服务（业务层入口，构造器注入）。
	 */
	private final DashboardService dashboardService;

	/**
	 * 获取数据看板列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/dashboard/list（如 /api/dashboard/list）。</p>
	 *
	 * <p><b>功能</b>：按查询条件返回一组看板统计项（每项为一个统计视图：指标名称、
	 * 数值、趋势数据等），供前端看板页面渲染图表。</p>
	 *
	 * <p><b>请求参数</b>：{@link DashboardQuery}（GET 查询参数）——统计范围条件
	 * （如项目 id、时间范围、统计类型等）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<DashboardView>}（看板统计视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link DashboardService#listDashboard(DashboardQuery)}。</p>
	 *
	 * @param query 看板查询参数
	 * @return 看板统计视图列表
	 */
	@GetMapping("/list")
	public List<DashboardView> listDashboard(DashboardQuery query) {
		return dashboardService.listDashboard(query);
	}

}
