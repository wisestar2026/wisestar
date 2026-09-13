package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.service.EnglishWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 英语单词学习接口（学生端）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@RestController
@RequestMapping("${api.prefix}/english/word")
@RequiredArgsConstructor
public class EnglishWordStudentApi {

	private final EnglishWordService englishWordService;

	/**
	 * 学生端：获取待学习/复习单词列表。
	 *
	 * @param limit 数量限制
	 * @return 单词列表
	 */
	@GetMapping("/study")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishWordView> studyWords(@RequestParam(defaultValue = "10") Integer limit) {
		String userId = SecurityContextUtils.getUserId();
		return englishWordService.getStudyWords(userId, limit);
	}

	/**
	 * 学生端：记录学习结果（熟练度 + 下次复习时间）。
	 *
	 * @param request {wordId, correct}
	 */
	@PostMapping("/record")
	@PreAuthorize("isAuthenticated()")
	public void recordLearning(@RequestBody java.util.Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		String wordId = String.valueOf(request.get("wordId"));
		Boolean correct = Boolean.valueOf(String.valueOf(request.get("correct")));
		englishWordService.recordLearning(userId, wordId, correct);
	}

	/**
	 * 学生端：按条件筛选单词本。
	 *
	 * @param version 版本
	 * @param grade 年级
	 * @param unit 单元
	 * @param query 查询条件（版本/年级/册别/单元 + 分页）
	 * @return 分页单词本
	 */
	@GetMapping("/word-book")
	@PreAuthorize("isAuthenticated()")
	public PaginationResponse<EnglishWordView> getWordBook(EnglishWordQuery query) {
		String userId = SecurityContextUtils.getUserId();
		return englishWordService.wordBook(userId, query);
	}

}
