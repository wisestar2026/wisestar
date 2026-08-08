package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.ReportData;
import cn.wisestar.server.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 问卷报表接口（ReportApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供问卷统计分析报表的数据查询接口——当前仅包含按短链接
 * （shortId）获取报表数据。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/report}（api.prefix 通常为 /api），
 * 当前方法路径为 ${api.prefix}/report/{shortId}。</p>
 * <p><b>被谁调用</b>：前端管理后台"问卷报表"页面（需要 project:report 权限）。</p>
 * <p><b>依赖的服务</b>：注入 {@link ReportService}（shared 模块接口，rdbms 模块实现）——
 * 负责从项目/答卷数据聚合生成报表统计结果。</p>
 *
 * <p><b>数据流</b>：前端 GET /api/report/{shortId} → 本类 getData(shortId) →
 * ReportService#getData → rdbms 实现 → 答卷/项目 Mapper 聚合统计 → ReportData → JSON。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
@RestController
@RequestMapping("${api.prefix}/report")
@RequiredArgsConstructor
public class ReportApi {

	/**
	 * 报表服务（业务层入口，构造器注入）。
	 */
	private final ReportService reportService;

	/**
	 * 获取问卷报表数据。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/report/{shortId}
	 * （如 /api/report/abc123）。</p>
	 *
	 * <p><b>功能</b>：按项目的短链接标识（shortId）获取该问卷的统计分析报表数据
	 * （答卷总量、回收率、各题选项统计等），供报表页面渲染。</p>
	 *
	 * <p><b>请求参数</b>：shortId（@PathVariable 路径变量，项目短链接标识）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ReportData}（报表统计视图：基础统计 + 各题明细统计）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('project:report')")——需要该权限点。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link ReportService#getData(String)}。</p>
	 *
	 * @param shortId 项目短链接标识（路径变量）
	 * @return 报表统计数据
	 */
	@GetMapping("/{shortId}")
	@PreAuthorize("hasAuthority('project:report')")
	public ReportData getData(@PathVariable String shortId) {
		return reportService.getData(shortId);
	}

}
