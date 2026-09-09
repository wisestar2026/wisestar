package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishAiPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishAiPackView;
import cn.wisestar.server.domain.dto.english.PackSyncResult;
import cn.wisestar.server.service.EnglishAiPackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * AI 单元内容包接口（后台管理端：AI 生成整套单元学习内容）。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@RestController
@RequestMapping("${api.prefix}/english/ai-pack")
@RequiredArgsConstructor
public class EnglishAiPackApi {

	private final EnglishAiPackService aiPackService;

	/**
	 * 内容包分页列表（不含 content 正文）。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public PaginationResponse<EnglishAiPackView> list(EnglishAiPackQuery query) {
		return aiPackService.listPacks(query);
	}

	/**
	 * 内容包详情（含 content 正文）。
	 */
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishAiPackView detail(@RequestParam String id) {
		return aiPackService.getPack(id);
	}

	/**
	 * 调用大模型生成整套单元内容（预览，不落库）。
	 */
	@PostMapping("/generate")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishAiPackView generate(
			@RequestParam String version,
			@RequestParam String grade,
			@RequestParam String unit,
			@RequestParam(required = false) String topic) {
		return aiPackService.generateUnit(version, grade, unit, topic);
	}

	/**
	 * 保存内容包（同 版本+年级+单元 覆盖更新）。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishAiPackView save(@RequestBody EnglishAiPackView pack) {
		return aiPackService.savePack(pack);
	}

	/**
	 * 将内容包同步到词库与语法库。
	 */
	@PostMapping("/sync")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public PackSyncResult sync(@RequestParam String id) {
		return aiPackService.syncToBank(id);
	}

	/**
	 * 删除内容包。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public void delete(@RequestParam String id) {
		aiPackService.deletePack(id);
	}

}
