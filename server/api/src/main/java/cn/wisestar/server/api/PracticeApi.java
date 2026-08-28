package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PracticeResultView;
import cn.wisestar.server.domain.dto.WrongReasonRequest;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;
import cn.wisestar.server.service.PracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 练习接口（学员端练习交卷落库）。
 *
 * <p><b>定位</b>：AI 自习室学员端练习闭环的数据落点——交卷后调用本接口，
 * 后端复核判分并写入 t_practice_record / t_practice_detail（错题标记）。</p>
 */
@RestController
@RequestMapping("${api.prefix}/practice")
@RequiredArgsConstructor
public class PracticeApi {

	/**
	 * 练习服务（业务层入口，构造器注入）。
	 */
	private final PracticeService practiceService;

	/**
	 * 提交一次练习（交卷落库 + 错题标记）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/practice/submit
	 * （如 /api/practice/submit）。</p>
	 *
	 * <p><b>功能</b>：接收前端逐题作答结果，后端按题目 id 回源并复核判分，
	 * 写入练习会话记录与逐题明细；is_correct=0 的明细即错题，供错题本查询。</p>
	 *
	 * <p><b>请求参数</b>：{@link PracticeSubmitRequest}（@RequestBody JSON：
	 * mode/repoId/durationMs/items[{questionId, answer}]）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")（学员端登录即可提交）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PracticeService#submitPractice(PracticeSubmitRequest)}。</p>
	 *
	 * @param request 练习交卷请求
	 */
	@PostMapping("/submit")
	@PreAuthorize("isAuthenticated()")
	public PracticeResultView submitPractice(@RequestBody PracticeSubmitRequest request) {
		return practiceService.submitPractice(request);
	}

	/**
	 * 分页查询错题库（题目 × 学员聚合）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/practice/wrong-list
	 * （如 /api/practice/wrong-list）。</p>
	 *
	 * <p><b>功能</b>：管理端「错题库管理」页面数据源——从 t_practice_detail（is_correct=0）
	 * 按题目 + 学员聚合错题，返回题目信息、学员、累计错误次数、最近做错时间与最近答案。</p>
	 *
	 * <p><b>请求参数</b>：{@link WrongQuestionQuery}（Query 参数：
	 * repoId/questionType/keyword/startTime/endTime/current/pageSize）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}（total + list，
	 * 元素为 {@link WrongQuestionView}）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")（登录用户可查看错题库）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link PracticeService#listWrongQuestions(WrongQuestionQuery)}。</p>
	 *
	 * @param query 错题筛选条件
	 * @return 聚合错题分页结果
	 */
	@GetMapping("/wrong-list")
	@PreAuthorize("isAuthenticated()")
	public PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query) {
		return practiceService.listWrongQuestions(query);
	}

	/**
	 * 保存错题错误归因（学员标注）。
	 *
	 * @param request 归因请求（detailId + reason）
	 */
	@PostMapping("/wrongReason")
	@PreAuthorize("isAuthenticated()")
	public void saveWrongReason(@RequestBody WrongReasonRequest request) {
		practiceService.saveWrongReason(request);
	}
}
