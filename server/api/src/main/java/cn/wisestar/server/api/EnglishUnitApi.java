package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishUnitQuery;
import cn.wisestar.server.domain.dto.english.EnglishUnitView;
import cn.wisestar.server.service.EnglishUnitService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
	 * 单元目录（按版本/年级/册别取，含单词数与句子数）。
	 *
	 * <p>供教研平台「英语」学科构建「册别 → 单元 → 单词/重点句子」只读知识树；
	 * 学员端与后台管理端之外的登录用户均可读取，故仅要求登录。</p>
	 *
	 * @param version 教材版本（可选）
	 * @param grade   年级（可选）
	 * @param term    册别，上册/下册（可选）
	 * @return 单元视图列表（sort 升序）
	 */
	@GetMapping("/books")
	@PreAuthorize("isAuthenticated()")
	public List<EnglishUnitView> books(@RequestParam(required = false) String version,
			@RequestParam(required = false) String grade,
			@RequestParam(required = false) String term) {
		return englishUnitService.listByBook(version, grade, term);
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
