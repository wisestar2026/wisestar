package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackQuery;
import cn.wisestar.server.domain.dto.english.EnglishSentencePackView;
import cn.wisestar.server.service.EnglishSentencePackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 英语单元句子同步练习内容包接口（后台管理端：预生成多题型练习并固化）。
 *
 * <p>复用既有权限点 {@code english:word:ai}，与 AI 内容生成同属内容准备能力。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@RestController
@RequestMapping("${api.prefix}/english/sentence-pack")
@RequiredArgsConstructor
public class EnglishSentencePackApi {

	private final EnglishSentencePackService sentencePackService;

	/**
	 * 按单元生成句子练习内容包预览（分组调用 AI，不落库）。
	 */
	@PostMapping("/generate")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishSentencePackView generate(
			@RequestParam String version,
			@RequestParam String grade,
			@RequestParam String term,
			@RequestParam String unit) {
		return sentencePackService.generate(version, grade, term, unit);
	}

	/**
	 * 保存句子练习内容包（同 版本+年级+册别+单元 覆盖更新）。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishSentencePackView save(@RequestBody EnglishSentencePackView pack) {
		return sentencePackService.savePack(pack);
	}

	/**
	 * 内容包分页列表（不含正文）。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public PaginationResponse<EnglishSentencePackView> list(EnglishSentencePackQuery query) {
		return sentencePackService.listPacks(query);
	}

	/**
	 * 内容包详情（含正文）。
	 */
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishSentencePackView detail(@RequestParam String id) {
		return sentencePackService.getPack(id);
	}

	/**
	 * 删除内容包。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public void delete(@RequestParam String id) {
		sentencePackService.deletePack(id);
	}

	/**
	 * 该册下存在重点句子的单元列表（用于批量生成）。
	 */
	@GetMapping("/units")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public List<String> units(@RequestParam String version,
			@RequestParam String grade,
			@RequestParam String term) {
		return sentencePackService.listUnits(version, grade, term);
	}

}
