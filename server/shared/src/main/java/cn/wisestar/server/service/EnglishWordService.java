package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.english.EnglishWordQuery;
import cn.wisestar.server.domain.dto.english.EnglishWordView;

/**
 * 英语单词学习服务。
 *
 * @author wisestar
 * @date 2026/8/30
 */
public interface EnglishWordService {

	/**
	 * 单词列表（按版本/年级/单元筛选）。
	 *
	 * @param query 查询条件
	 * @return 分页单词列表
	 */
	PaginationResponse<EnglishWordView> listWords(EnglishWordQuery query);

	/**
	 * 获取待学习/复习单词。
	 *
	 * @param userId 用户 ID
	 * @param limit 数量限制
	 * @return 待学习/复习单词列表
	 */
	java.util.List<EnglishWordView> getStudyWords(String userId, int limit);

	/**
	 * 记录学习结果（熟练度 + 下次复习时间）。
	 *
	 * @param userId 用户 ID
	 * @param wordId 单词 ID
	 * @param correct 是否正确
	 */
	void recordLearning(String userId, String wordId, boolean correct);

}
