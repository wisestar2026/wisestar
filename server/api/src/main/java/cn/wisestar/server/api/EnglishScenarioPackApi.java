package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackQuery;
import cn.wisestar.server.domain.dto.english.EnglishScenarioPackView;
import cn.wisestar.server.service.EnglishScenarioPackService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 英语单元情景练习内容包接口（后台管理端：预生成情景练习并固化）。
 *
 * <p>复用既有权限点 {@code english:word:ai}，与 AI 内容生成同属内容准备能力。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@RestController
@RequestMapping("${api.prefix}/english/scenario-pack")
@RequiredArgsConstructor
public class EnglishScenarioPackApi {

	private final EnglishScenarioPackService scenarioPackService;

	/**
	 * 按单元生成情景内容包预览（分组调用 AI，不落库）。
	 */
	@PostMapping("/generate")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishScenarioPackView generate(
			@RequestParam String version,
			@RequestParam String grade,
			@RequestParam String term,
			@RequestParam String unit) {
		return scenarioPackService.generate(version, grade, term, unit);
	}

	/**
	 * 保存情景内容包（同 版本+年级+册别+单元 覆盖更新）。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishScenarioPackView save(@RequestBody EnglishScenarioPackView pack) {
		return scenarioPackService.savePack(pack);
	}

	/**
	 * 情景内容包分页列表（不含正文）。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public PaginationResponse<EnglishScenarioPackView> list(EnglishScenarioPackQuery query) {
		return scenarioPackService.listPacks(query);
	}

	/**
	 * 情景内容包详情（含正文）。
	 */
	@GetMapping("/detail")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public EnglishScenarioPackView detail(@RequestParam String id) {
		return scenarioPackService.getPack(id);
	}

	/**
	 * 删除情景内容包。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public void delete(@RequestParam String id) {
		scenarioPackService.deletePack(id);
	}

	/**
	 * 该册下存在单词或句式的单元列表（用于批量生成）。
	 */
	@GetMapping("/units")
	@PreAuthorize("hasAuthority('english:word:ai')")
	public List<String> units(@RequestParam String version,
			@RequestParam String grade,
			@RequestParam String term) {
		return scenarioPackService.listUnits(version, grade, term);
	}

}
