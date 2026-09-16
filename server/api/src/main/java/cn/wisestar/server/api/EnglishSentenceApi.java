package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSentenceQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.ImportResult;
import cn.wisestar.server.service.EnglishSentenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 英语句库接口（后台管理端）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@RestController
@RequestMapping("${api.prefix}/english/sentence")
@RequiredArgsConstructor
public class EnglishSentenceApi {

	private final EnglishSentenceService englishSentenceService;

	/**
	 * 句子分页列表。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:sentence:list')")
	public PaginationResponse<EnglishSentenceView> list(EnglishSentenceQuery query) {
		return englishSentenceService.list(query);
	}

	/**
	 * 新增/更新句子。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAnyAuthority('english:sentence:create','english:sentence:update')")
	public void save(@RequestBody EnglishSentenceView view) {
		englishSentenceService.saveOrUpdate(view);
	}

	/**
	 * 删除句子。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:sentence:delete')")
	public void delete(@RequestParam String id) {
		englishSentenceService.delete(id);
	}

	/**
	 * 批量导入句子（Excel）。
	 */
	@PostMapping("/import")
	@PreAuthorize("hasAuthority('english:sentence:import')")
	public ImportResult importSentences(@RequestParam MultipartFile file) {
		return englishSentenceService.importSentences(file);
	}

}
