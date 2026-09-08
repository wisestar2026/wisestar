package cn.wisestar.server.api;

import cn.wisestar.server.core.constant.PermissionConsts;
import cn.wisestar.server.domain.dto.CampusRequest;
import cn.wisestar.server.domain.dto.CampusView;
import cn.wisestar.server.service.CampusService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 校区管理接口（行政管理-校区管理）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层）。</p>
 * <p><b>路径前缀</b>：{@code ${api.prefix}/system/campus}（api.prefix 通常为 /api）。</p>
 * <p><b>被谁调用</b>：校区管理页 /admin/campus、学员管理页校区下拉、系统用户管理页校区多选。</p>
 * <p><b>数据权限</b>：列表与下拉按当前账号校区数据权限范围返回（校长/教务/学管师仅见自己
 * 绑定校区）；写操作（create/update/delete）当前仅管理员内置角色拥有。</p>
 *
 * @author wisestar
 * @date 2026/9/8
 */
@RestController
@RequestMapping("${api.prefix}/system/campus")
@RequiredArgsConstructor
public class CampusApi {

	/**
	 * 校区下拉可选权限集（凡能触达学员校区字段的账号均可读取下拉）。
	 */
	private static final String OPTIONS_AUTHORITIES = "hasAnyAuthority('campus:list','student:list',"
			+ "'student:create','student:update','order:list','student:supervision','system:user:list')";

	private final CampusService campusService;

	/**
	 * 校区管理列表（含引用学员数/绑定角色员工数统计，受当前账号校区数据权限约束）。
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('" + PermissionConsts.CAMPUS_LIST + "')")
	public List<CampusView> listCampuses() {
		return campusService.listCampuses();
	}

	/**
	 * 校区下拉数据源（供学员表单/用户管理校区字段使用）。
	 *
	 * @param includeDisabled 是否包含停用校区（默认 false：仅启用校区）
	 */
	@GetMapping("/options")
	@PreAuthorize(OPTIONS_AUTHORITIES)
	public List<CampusView> campusOptions(@RequestParam(defaultValue = "false") boolean includeDisabled) {
		return campusService.campusOptions(includeDisabled);
	}

	/**
	 * 新增校区。
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('" + PermissionConsts.CAMPUS_CREATE + "')")
	public void createCampus(@RequestBody CampusRequest request) {
		campusService.createCampus(request);
	}

	/**
	 * 更新校区（改名同事务同步学员引用）。
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('" + PermissionConsts.CAMPUS_UPDATE + "')")
	public void updateCampus(@RequestBody CampusRequest request) {
		campusService.updateCampus(request);
	}

	/**
	 * 删除校区（仅零引用可删）。
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('" + PermissionConsts.CAMPUS_DELETE + "')")
	public void deleteCampus(@RequestBody CampusRequest request) {
		campusService.deleteCampus(request.getId());
	}

}
