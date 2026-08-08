package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.ExerciseView;
import cn.wisestar.server.domain.dto.HistoryExerciseQuery;
import cn.wisestar.server.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 练习接口（ExerciseApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供"练习"（exercise）场景的查询接口——目前仅包含历史练习列表查询。
 * 练习即用户对问卷/题库进行答题练习，历史练习记录从答卷数据（answer）中派生。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/exercise}（api.prefix 通常为 /api），
 * 当前方法路径为 ${api.prefix}/exercise/list。</p>
 * <p><b>被谁调用</b>：前端"练习中心/历史练习"页面（需要登录且具备 exercise:list 权限）。</p>
 * <p><b>依赖的服务</b>：注入 {@link AnswerService}（shared 模块接口，rdbms 模块实现）——
 * 注意本类将练习查询复用答卷服务的能力（历史练习本质上是答卷记录的筛选视图）。</p>
 *
 * <p><b>数据流</b>：前端 GET /api/exercise/list?pageNo=&amp;pageSize= → 本类
 * historyExercise(HistoryExerciseQuery) → AnswerService#historyExercise → rdbms 实现
 * → 答卷 Mapper 按当前用户 + 练习类型筛选 → PaginationResponse&lt;ExerciseView&gt; → JSON。</p>
 */
@RestController
@RequestMapping("${api.prefix}/exercise")
@RequiredArgsConstructor
public class ExerciseApi {

	/**
	 * 答卷服务（业务层入口，Lombok @RequiredArgsConstructor 构造器注入）。
	 * 练习数据由答卷记录派生，因此本类直接复用 AnswerService。
	 */
	private final AnswerService answerService;

	/**
	 * 历史练习列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/exercise/list（如 /api/exercise/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询当前登录用户的"历史练习"记录（练习过的问卷/题库、
	 * 练习时间、成绩等），供练习中心历史列表页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link HistoryExerciseQuery}（GET 查询参数，@Valid 校验）——
	 * 分页参数（pageNo/pageSize）及练习筛选条件（如练习类型、状态、时间范围）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;ExerciseView&gt;（分页包装：
	 * total + 当前页练习记录列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('exercise:list')")——需要该权限点。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#historyExercise(HistoryExerciseQuery)}。</p>
	 *
	 * @param query 查询参数（GET 绑定）
	 * @return 历史练习分页列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('exercise:list')")
	public PaginationResponse<ExerciseView> historyExercise(@Valid HistoryExerciseQuery query) {
		return answerService.historyExercise(query);
	}

}
