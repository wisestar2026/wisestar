package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;

/**
 * 练习服务（练习会话落库 / 错题标记 / 错题库查询）。
 *
 * <p><b>功能</b>：学员端交卷后，按提交的逐题作答结果回源题目并复核判分，
 * 汇总写入 t_practice_record（练习会话）与 t_practice_detail（逐题明细，is_correct=0 即错题）；
 * 管理端「错题库管理」按题目 × 学员聚合查询错题记录。</p>
 */
public interface PracticeService {

	/**
	 * 提交一次练习（交卷落库 + 错题标记）。
	 *
	 * @param request 练习交卷请求（模式/题库/用时/逐题作答）
	 */
	void submitPractice(PracticeSubmitRequest request);

	/**
	 * 分页查询错题库（题目 × 学员聚合）。
	 *
	 * @param query 筛选条件（题库/题型/关键词/做错时间范围 + 分页）
	 * @return 聚合错题分页结果（wrongCount 累计错误次数）
	 */
	PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query);
}
