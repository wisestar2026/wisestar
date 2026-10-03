package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.archive.ArchiveRecordDraftView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveOverviewView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveSaveRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveView;
import cn.wisestar.server.service.StudentArchiveService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

}
