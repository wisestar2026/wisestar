package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishUnitQuery;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;
import cn.wisestar.server.service.EnglishUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 英语单元目录接口（后台管理端）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@RestController
@RequestMapping("${api.prefix}/english/unit")
@RequiredArgsConstructor
public class EnglishUnitApi {

	private final EnglishUnitService englishUnitService;

	/**
	 * 单元分页列表。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('english:unit:list')")
	public PaginationResponse<EnglishUnitView> list(EnglishUnitQuery query) {
		return englishUnitService.list(query);
	}

	/**
	 * 新增/更新单元。
	 */
	@PostMapping("/save")
	@PreAuthorize("hasAnyAuthority('english:unit:create','english:unit:update')")
	public void save(@RequestBody EnglishUnitView view) {
		englishUnitService.saveOrUpdate(view);
	}

	/**
	 * 删除单元。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('english:unit:delete')")
	public void delete(@RequestParam String id) {
		englishUnitService.delete(id);
	}

}
