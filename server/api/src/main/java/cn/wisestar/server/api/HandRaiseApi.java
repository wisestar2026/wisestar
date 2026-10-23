package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.handraise.HandRaiseHandleRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseQuery;
import cn.wisestar.server.domain.dto.handraise.HandRaiseRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseView;
import cn.wisestar.server.service.HandRaiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学员举手反馈接口。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层）。</p>
 * <p><b>类职责</b>：学员题目页「举手」提交；教师端举手提醒列表、待处理数量与处理操作。</p>
 * <p><b>请求路径前缀</b>：${api.prefix}/hand-raise（api.prefix 通常为 /api）。</p>
 * <p><b>数据流</b>：前端 → 本类（权限注解）→ {@link HandRaiseService} →
 * rdbms 实现 → t_hand_raise。</p>
 *
 * @author wisestar
 * @date 2026/10/22
 */
@RestController
@RequestMapping("${api.prefix}/hand-raise")
@RequiredArgsConstructor
public class HandRaiseApi {

	private final HandRaiseService handRaiseService;

	/**
	 * 学员提交举手反馈。
	 *
	 * <p>HTTP：POST ${api.prefix}/hand-raise/create。</p>
	 * <p>请求参数：{@link HandRaiseRequest}（反馈类型、题目ID、学员作答与补充说明）。</p>
	 * <p>权限：登录即可（服务层校验当前账号为学员）。</p>
	 *
	 * @param request 举手反馈内容
	 */
	@PostMapping("/create")
	@PreAuthorize("isAuthenticated()")
	public void create(@RequestBody HandRaiseRequest request) {
		handRaiseService.createByStudent(request);
	}

	/**
	 * 举手反馈分页列表（教师端提醒 / 题目管理端待修任务）。
	 *
	 * <p>HTTP：GET ${api.prefix}/hand-raise/list。</p>
	 * <p>请求参数：{@link HandRaiseQuery}（类型/状态/学科/关键词/学员）。</p>
	 * <p>权限：handraise:list。</p>
	 *
	 * @param query 查询条件
	 * @return 分页反馈列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('handraise:list')")
	public PaginationResponse<HandRaiseView> list(HandRaiseQuery query) {
		return handRaiseService.listHandRaises(query);
	}

	/**
	 * 处理举手反馈（标记已处理/已忽略）。
	 *
	 * <p>HTTP：POST ${api.prefix}/hand-raise/handle。</p>
	 * <p>请求参数：{@link HandRaiseHandleRequest}（反馈ID、目标状态、处理备注）。</p>
	 * <p>权限：handraise:handle。</p>
	 *
	 * @param request 处理请求
	 */
	@PostMapping("/handle")
	@PreAuthorize("hasAuthority('handraise:handle')")
	public void handle(@RequestBody HandRaiseHandleRequest request) {
		handRaiseService.handle(request);
	}

	/**
	 * 待处理数量统计（教师端菜单角标）。
	 *
	 * <p>HTTP：GET ${api.prefix}/hand-raise/pending-count。</p>
	 * <p>权限：handraise:list。</p>
	 *
	 * @return explain 待讲解数 / error 待修题数 / total 合计
	 */
	@GetMapping("/pending-count")
	@PreAuthorize("hasAuthority('handraise:list')")
	public Map<String, Long> pendingCount() {
		return handRaiseService.pendingCount();
	}

}
