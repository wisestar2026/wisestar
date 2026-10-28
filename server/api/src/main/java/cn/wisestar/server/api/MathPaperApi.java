package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.math.MathPaperMetaView;
import cn.wisestar.server.domain.dto.math.MathPaperRequest;
import cn.wisestar.server.domain.dto.math.MathPaperStudentView;
import cn.wisestar.server.domain.dto.math.MathPaperView;
import cn.wisestar.server.service.MathPaperService;
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
 * 数学组卷接口（后台管理端）。
 *
 * <p><b>功能</b>：按「章节/小节/知识点 + 难度 + 题型 + 题量」通用组卷，
 * 或按学员学号优先组该学员的数学错题卷；生成结果含标准答案与解析，供直接打印。</p>
 *
 * <p><b>权限</b>：复用 {@code template:list}（与题目管理、英语单元练习卷一致）。</p>
 *
 * @author wisestar
 * @date 2026/10/26
 */
@RestController
@RequestMapping("${api.prefix}/math/paper")
@RequiredArgsConstructor
public class MathPaperApi {

	private final MathPaperService mathPaperService;

	/**
	 * 组卷配置元数据（年级/册别/章节/小节/知识点树 + 题型、难度题量）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/math/paper/meta。</p>
	 */
	@GetMapping("/meta")
	@PreAuthorize("hasAuthority('template:list')")
	public MathPaperMetaView meta() {
		return mathPaperService.meta();
	}

	/**
	 * 按学号/姓名搜索学员（按学员组卷时选人用）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/math/paper/students?keyword=。</p>
	 */
	@GetMapping("/students")
	@PreAuthorize("hasAuthority('template:list')")
	public List<MathPaperStudentView> students(@RequestParam(required = false) String keyword) {
		return mathPaperService.searchStudents(keyword);
	}

	/**
	 * 生成试卷（含标准答案与解析）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/math/paper/generate。</p>
	 */
	@PostMapping("/generate")
	@PreAuthorize("hasAuthority('template:list')")
	public MathPaperView generate(@RequestBody MathPaperRequest request) {
		return mathPaperService.generate(request);
	}

}
