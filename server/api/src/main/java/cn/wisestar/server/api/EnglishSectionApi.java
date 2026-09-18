package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishSectionQuery;
import cn.wisestar.server.domain.dto.english.EnglishSectionView;
import cn.wisestar.server.service.EnglishSectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 英语小节目录接口（后台管理端）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@RestController
@RequestMapping("${api.prefix}/english/section")
@RequiredArgsConstructor
public class EnglishSectionApi {

	private final EnglishSectionService englishSectionService;

	/**
	 * 小节分页列表。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:section:list')")
	public PaginationResponse<EnglishSectionView> list(EnglishSectionQuery query) {
		return englishSectionService.list(query);
	}

	/**
	 * 新增/更新小节。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAnyAuthority('english:section:create','english:section:update')")
	public void save(@RequestBody EnglishSectionView view) {
		englishSectionService.saveOrUpdate(view);
	}

	/**
	 * 删除小节。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:section:delete')")
	public void delete(@RequestParam String id) {
		englishSectionService.delete(id);
	}

}
