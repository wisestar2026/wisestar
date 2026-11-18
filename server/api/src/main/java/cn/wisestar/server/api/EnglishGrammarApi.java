package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishGrammarQuery;
import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.service.EnglishAiPackService;
import cn.wisestar.server.service.EnglishGrammarService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 英语语法接口（后台管理端）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@RestController
@RequestMapping("${api.prefix}/english/grammar")
@RequiredArgsConstructor
public class EnglishGrammarApi {

	private final EnglishGrammarService englishGrammarService;

	private final EnglishAiPackService englishAiPackService;

	/**
	 * 语法分页列表。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:grammar:list')")
	public PaginationResponse<EnglishGrammarView> list(EnglishGrammarQuery query) {
		return englishGrammarService.list(query);
	}

	/**
	 * 新增/更新语法。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAnyAuthority('english:grammar:create','english:grammar:update')")
	public void save(@RequestBody EnglishGrammarView view) {
		englishGrammarService.saveOrUpdate(view);
	}

	/**
	 * AI 批量补齐单元语法点（仅预览，不落库；确认后调用 /save 入库）。
	 */
	@PostMapping("/ai-enrich")
	@PreAuthorize("hasAnyAuthority('english:grammar:create','english:grammar:update')")
	public List<EnglishGrammarView> aiEnrich(@RequestParam String version, @RequestParam String grade,
			@RequestParam(required = false) String term, @RequestParam String unit) {
		return englishAiPackService.generateGrammarPreview(version, grade, term, unit);
	}

	/**
	 * 删除语法。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:grammar:delete')")
	public void delete(@RequestParam String id) {
		englishGrammarService.delete(id);
	}

}
