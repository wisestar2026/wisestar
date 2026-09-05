package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.service.EnglishWordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 英语单词列表接口（学生端学习接口见 EnglishWordStudentApi）。
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

}
