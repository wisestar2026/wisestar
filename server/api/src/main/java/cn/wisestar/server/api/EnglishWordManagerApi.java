package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;
import cn.wisestar.server.service.EnglishWordManagerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 英语单词管理接口（后台管理端）。
 *
 * @author wisestar
 * @date 2026/8/30
 */
@RestController
@RequestMapping("${api.prefix}/english/word-manager")
@RequiredArgsConstructor
public class EnglishWordManagerApi {

	private final EnglishWordManagerService wordManagerService;

	/**
	 * 单词列表（按版本/年级/单元筛选）。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:word:list')")
	public cn.wisestar.server.core.common.PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query) {
		return wordManagerService.listWords(query);
	}

	/**
	 * 单词详情。
	 */
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('english:word:list')")
	public EnglishWordView detail(@RequestParam String id) {
		return wordManagerService.getDetail(id);
	}

	/**
	 * 新增单词。
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('english:word:create')")
	public void createWord(@RequestBody EnglishWordView word) {
		wordManagerService.createWord(word);
	}

	/**
	 * 编辑单词。
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('english:word:update')")
	public void updateWord(@RequestBody EnglishWordView word) {
		wordManagerService.updateWord(word);
	}

	/**
	 * 删除单词。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:word:delete')")
	public void deleteWord(@RequestParam String id) {
		wordManagerService.deleteWord(id);
	}

	/**
	 * 批量导入单词（Excel）。
	 */
	@PostMapping("/import")
	@PreAuthorize("hasAuthority('english:word:import')")
	public cn.wisestar.server.domain.dto.english.ImportResult importWords(@RequestParam MultipartFile file) {
		return wordManagerService.importWords(file);
	}

}
