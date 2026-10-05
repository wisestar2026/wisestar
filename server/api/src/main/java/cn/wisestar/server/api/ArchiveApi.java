package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.archive.ArchiveRecordDraftView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveOverviewView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveSaveRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveView;
import cn.wisestar.server.domain.dto.detect.DetectRecordView;
import cn.wisestar.server.domain.dto.growth.GrowthCompareView;
import cn.wisestar.server.domain.dto.growth.GrowthEventView;
import cn.wisestar.server.domain.dto.growth.GrowthReportRequest;
import cn.wisestar.server.domain.dto.growth.GrowthReportView;
import cn.wisestar.server.service.DetectionService;
import cn.wisestar.server.service.GrowthArchiveService;
import cn.wisestar.server.service.StudentArchiveService;
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
 * 学员档案接口。
 *
 * <p><b>功能</b>：管理端（老师）维护学员档案——目标规划表、承诺书、上课记录/学习日志与学期报告，
 * 整体可打印交付家长；学员端只读查看本人档案。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@RestController
@RequestMapping("${api.prefix}/student/archive")
@RequiredArgsConstructor
public class ArchiveApi {

	private final StudentArchiveService archiveService;

	private final DetectionService detectionService;

	private final GrowthArchiveService growthArchiveService;

	/**
	 * 学员档案详情（含初始快照、薄弱点、上课记录、当日情况）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/detail?studentId=&amp;semester=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('student:archive')")
	public StudentArchiveView detail(@RequestParam String studentId,
			@RequestParam(required = false) String semester) {
		return archiveService.getArchive(studentId, semester);
	}

	/**
	 * 档案概览（学员列表入口角标：是否建档、薄弱点/记录数、报告状态）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/overview?studentId=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/overview")
	@PreAuthorize("hasAuthority('student:archive')")
	public StudentArchiveOverviewView overview(@RequestParam String studentId) {
		return archiveService.overview(studentId);
	}

	/**
	 * 保存档案（目标规划表/承诺书/学期报告/状态）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/archive/save。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public StudentArchiveView save(@RequestBody StudentArchiveSaveRequest request) {
		return archiveService.save(request);
	}

	/**
	 * 生成上课记录草稿（拉取当日学习数据/薄弱/强化知识点）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/draft?studentId=&amp;date=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@GetMapping("/draft")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public ArchiveRecordDraftView draft(@RequestParam String studentId,
			@RequestParam(required = false) String date) {
		return archiveService.draft(studentId, date);
	}

	/**
	 * 保存上课记录（新增/更新，档案不存在时自动建档）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/archive/record/save。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@PostMapping("/record/save")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public StudentArchiveRecordView saveRecord(@RequestBody StudentArchiveRecordRequest request) {
		return archiveService.saveRecord(request);
	}

	/**
	 * 删除上课记录。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/archive/record/delete?id=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@PostMapping("/record/delete")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public void deleteRecord(@RequestParam String id) {
		archiveService.deleteRecord(id);
	}

	/**
	 * 生成学期报告（AI 可用时润色，否则规则模板）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/archive/report/generate?studentId=&amp;semester=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@PostMapping("/report/generate")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public StudentArchiveView generateReport(@RequestParam String studentId,
			@RequestParam(required = false) String semester) {
		return archiveService.generateReport(studentId, semester);
	}

	/**
	 * 我的档案（学员端只读）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/my。</p>
	 */
	@GetMapping("/my")
	@PreAuthorize("isAuthenticated()")
	public StudentArchiveView myArchive() {
		return archiveService.myArchive();
	}

	/**
	 * 指定学员检测历史（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/detect-history?studentId=&amp;subjectId=&amp;semester=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/detect-history")
	@PreAuthorize("hasAuthority('student:archive')")
	public List<DetectRecordView> detectHistory(@RequestParam String studentId,
			@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String semester) {
		return detectionService.history(studentId, subjectId, semester);
	}

	/**
	 * 指定学员学习轨迹时间轴（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/timeline?studentId=&amp;subjectId=&amp;from=&amp;to=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/timeline")
	@PreAuthorize("hasAuthority('student:archive')")
	public List<GrowthEventView> timeline(@RequestParam String studentId,
			@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		return growthArchiveService.timeline(studentId, subjectId, from, to);
	}

	/**
	 * 指定学员成长对比（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/compare?studentId=&amp;subjectId=&amp;semester=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/compare")
	@PreAuthorize("hasAuthority('student:archive')")
	public GrowthCompareView compare(@RequestParam String studentId,
			@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String semester) {
		return growthArchiveService.compare(studentId, subjectId, semester);
	}

	/**
	 * 生成指定学员成长报告（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/archive/growth/report/generate。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive:edit')。</p>
	 */
	@PostMapping("/growth/report/generate")
	@PreAuthorize("hasAuthority('student:archive:edit')")
	public GrowthReportView generateGrowthReport(@RequestBody GrowthReportRequest request) {
		return growthArchiveService.generate(request);
	}

	/**
	 * 读取指定学员成长报告（管理端）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/archive/growth/report?studentId=&amp;subjectId=&amp;semester=。</p>
	 * <p><b>权限</b>：hasAuthority('student:archive')。</p>
	 */
	@GetMapping("/growth/report")
	@PreAuthorize("hasAuthority('student:archive')")
	public GrowthReportView growthReport(@RequestParam String studentId,
			@RequestParam(required = false) String subjectId,
			@RequestParam(required = false) String semester) {
		return growthArchiveService.report(studentId, subjectId, semester);
	}

}
