package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.detect.DetectGenerateRequest;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.detect.DetectSubmitRequest;
import cn.wisestar.server.domain.dto.detect.DetectUnitView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.service.DetectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学员端知识点检测接口。
 *
 * <p><b>功能</b>：学员勾选单元/题量/难度自动组卷，交卷后生成薄弱知识点诊断报告。
 * 检测为诊断性质，不发放学习币/积分，也不写入练习记录。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
@RestController
@RequestMapping("${api.prefix}/student/detect")
@RequiredArgsConstructor
public class DetectionApi {

	private final DetectionService detectionService;

	/**
	 * 可选单元列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/student/detect/units?subjectId=&amp;grade=&amp;term=。</p>
	 */
	@GetMapping("/units")
	@PreAuthorize("isAuthenticated()")
	public List<DetectUnitView> units(@RequestParam String subjectId,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term) {
		return detectionService.units(subjectId, grade, term);
	}

	/**
	 * 自动组卷（剥离答案与解析）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/detect/generate。</p>
	 */
	@PostMapping("/generate")
	@PreAuthorize("isAuthenticated()")
	public List<StudentQuestionView> generate(@RequestBody DetectGenerateRequest request) {
		return detectionService.generate(request);
	}

	/**
	 * 交卷并生成诊断报告。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/student/detect/submit。</p>
	 */
	@PostMapping("/submit")
	@PreAuthorize("isAuthenticated()")
	public DetectReportView submit(@RequestBody DetectSubmitRequest request) {
		return detectionService.submit(request);
	}

}
