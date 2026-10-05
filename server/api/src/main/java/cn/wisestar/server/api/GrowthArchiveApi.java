package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.growth.GrowthCompareView;
import cn.wisestar.server.domain.dto.growth.GrowthEventView;
import cn.wisestar.server.domain.dto.growth.GrowthReportRequest;
import cn.wisestar.server.domain.dto.growth.GrowthReportView;
import cn.wisestar.server.service.GrowthArchiveService;
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
 * 学员本人成长档案接口（学习轨迹 + 成长对比 + 成长报告）。
 *
 * <p><b>功能</b>：学员查看本人学习轨迹时间轴、以学前检测基线为参照的成长对比，
 * 以及可打印的成长报告；报告可手动触发重新生成。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@RestController
@RequestMapping("${api.prefix}/student/growth")
@RequiredArgsConstructor
public class GrowthArchiveApi {

	private final GrowthArchiveService growthArchiveService;

	/**
	 * 学习轨迹时间轴（按发生时间倒序）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/growth/timeline?subjectId=&amp;from=&amp;to=。</p>
	 * <p><b>权限</b>：isAuthenticated()。</p>
	 */
	@GetMapping("/timeline")
	@PreAuthorize("isAuthenticated()")
	public List<GrowthEventView> timeline(@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		return growthArchiveService.timeline(null, subjectId, from, to);
	}

	/**
	 * 成长对比（基线 vs 当前）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/growth/compare?subjectId=&amp;semester=。</p>
	 * <p><b>权限</b>：isAuthenticated()。</p>
	 */
	@GetMapping("/compare")
	@PreAuthorize("isAuthenticated()")
	public GrowthCompareView compare(@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String semester) {
		return growthArchiveService.compare(null, subjectId, semester);
	}

	/**
	 * 读取本人成长报告。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/growth/report?subjectId=&amp;semester=。</p>
	 * <p><b>权限</b>：isAuthenticated()。</p>
	 */
	@GetMapping("/report")
	@PreAuthorize("isAuthenticated()")
	public GrowthReportView report(@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String semester) {
		return growthArchiveService.report(null, subjectId, semester);
	}

	/**
	 * 生成/重新生成本人成长报告。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/growth/report/generate。</p>
	 * <p><b>权限</b>：isAuthenticated()。</p>
	 */
	@PostMapping("/report/generate")
	@PreAuthorize("isAuthenticated()")
	public GrowthReportView generate(@RequestBody GrowthReportRequest request) {
		return growthArchiveService.generate(request);
	}

}
