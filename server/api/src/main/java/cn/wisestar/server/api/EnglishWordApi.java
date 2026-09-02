package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.service.EnglishWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 英语单词学习接口。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@RestController
@RequestMapping("${api.prefix}/english/word")
@RequiredArgsConstructor
public class EnglishWordApi {

	private final EnglishWordService englishWordService;

	/**
	 * 单词列表（按版本/年级/单元筛选）。
	 *
	 * @param query 查询条件
	 * @return 分页单词列表
	 */
	@GetMapping("/list")
	@PreAuthorize("isAuthenticated()")
	public cn.wisestar.server.core.common.PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query) {
		return englishWordService.listWords(query);
	}

	/**
	 * 获取待学习/复习单词。
	 *
	 * @param limit 数量限制
	 * @return 待学习/复习单词列表
	 */
	@GetMapping("/study")
	@PreAuthorize("isAuthenticated()")
	public java.util.List<EnglishWordView> studyWords(Integer limit) {
		String userId = SecurityContextUtils.getUserId();
		return englishWordService.getStudyWords(userId, limit == null ? 10 : limit);
	}

	/**
	 * 记录学习结果（熟练度 + 下次复习时间）。
	 *
	 * @param wordId 单词 ID
	 * @param correct 是否正确
	 */
	@PostMapping("/record")
	@PreAuthorize("isAuthenticated()")
	public void recordLearning(@RequestBody java.util.Map<String, Object> request) {
		String userId = SecurityContextUtils.getUserId();
		String wordId = String.valueOf(request.get("wordId"));
		Boolean correct = Boolean.valueOf(String.valueOf(request.get("correct")));
		englishWordService.recordLearning(userId, wordId, correct);
	}

}
