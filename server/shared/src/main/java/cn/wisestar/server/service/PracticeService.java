package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.PracticeMasteryView;
import cn.wisestar.server.domain.dto.PracticeResultView;
import cn.wisestar.server.domain.dto.WrongReasonBatchRequest;
import cn.wisestar.server.domain.dto.WrongReasonRequest;
import cn.wisestar.server.domain.dto.PracticeSubmitRequest;
import cn.wisestar.server.domain.dto.WrongQuestionQuery;
import cn.wisestar.server.domain.dto.WrongQuestionView;

import java.util.List;

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
	PracticeResultView submitPractice(PracticeSubmitRequest request);

	/**
	 * 保存错题错误归因（学员标注：大意/计算错误/知识点不熟/题型不会等）。
	 *
	 * @param request 归因请求（detailId + reason）
	 */
	void saveWrongReason(WrongReasonRequest request);

	/**
	 * 分页查询错题库（题目 × 学员聚合）。
	 *
	 * @param query 筛选条件（题库/题型/关键词/做错时间范围 + 分页）
	 * @return 聚合错题分页结果（wrongCount 累计错误次数）
	 */
	PaginationResponse<WrongQuestionView> listWrongQuestions(WrongQuestionQuery query);

	/**
	 * 保存错题错误归因（批量：交卷后强制逐题归因一次提交）。
	 *
	 * <p>逐条复用 {@link #saveWrongReason(WrongReasonRequest)} 的归属校验；
	 * 任一明细不属于当前学员即整批失败（保证交卷归因原子性）。</p>
	 *
	 * @param request 批量归因请求（items 非空）
	 */
	void saveWrongReasons(WrongReasonBatchRequest request);

	/**
	 * 学员练习掌握度汇总（学习页上次掌握卡 / 交卷结果页掌握变化的数据源）。
	 *
	 * <p>按范围二选一：sectionId（小节）或 knowledgePointId（知识点）。
	 * 掌握度基于该范围学员全部练习历史统计（答对题次 ÷ 判分题次）；
	 * lastRecord 为该范围最近一次练习记录（上次练习结果卡）。</p>
	 *
	 * @param sectionId        小节ID（与 knowledgePointId 二选一）
	 * @param knowledgePointId 知识点ID（与 sectionId 二选一）
	 * @return 掌握度汇总视图
	 */
	PracticeMasteryView mastery(String sectionId, String knowledgePointId);

	/**
	 * 范围内近期练习记录列表（交卷结果页「掌握变化」对比数据源）。
	 *
	 * <p>与 {@link #mastery(String, String)} 相同的范围锚定（record.section_id /
	 * record.knowledge_point_id），返回最近至多 20 条记录摘要（倒序）：
	 * 第 0 条为本次/最近一次，第 1 条起为历史，用于计算掌握度变化。</p>
	 *
	 * @param sectionId        小节ID（与 knowledgePointId 二选一）
	 * @param knowledgePointId 知识点ID（与 sectionId 二选一）
	 * @return 近期练习记录摘要列表（无记录返回空列表）
	 */
	List<PracticeMasteryView.LastRecord> history(String sectionId, String knowledgePointId);
}
