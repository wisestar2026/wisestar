package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishLearningStateView;
import cn.wisestar.server.domain.dto.english.EnglishReviewCandidateView;
import cn.wisestar.server.domain.dto.english.EnglishSectionProgressView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;

import java.util.List;

/**
 * 英语学习规则服务：把「分阶段通关 / 薄弱攻克 / 复习派生」的可判定口径收敛到一处。
 *
 * <p>学员端页面只消费本服务的结果，不再各自计算阈值与状态，避免同一条规则在
 * 多个页面写出不同数字。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
public interface EnglishLearningRuleService {

	/**
	 * 批次加载三个模块的学习状态，供单元进度与复习候选复用，避免逐知识点查库。
	 *
	 * @param userId 用户 ID
	 * @param modules 模块列表
	 * @return 学习状态，键为 module + "|" + knowledgeId
	 */
	java.util.Map<String, EnglishLearningStateView> loadStates(String userId, List<String> modules);

	/**
	 * 记录一次作答并更新学习状态（三态、连续正确、输出题达标、阶段计数）。
	 *
	 * @param userId 用户 ID
	 * @param module 模块：word / sentence / grammar
	 * @param knowledgeId 知识点 ID
	 * @param angle 考察角度
	 * @param correct 是否答对
	 * @param stage 阶段：memory / practice / use
	 * @param firstAttempt 是否首次作答（重做不计入状态统计）
	 */
	void recordAnswer(String userId, String module, String knowledgeId, String angle, boolean correct,
			String stage, boolean firstAttempt);

	/**
	 * 批量加载已使用考察角度（供列表页回填「已考过哪些角度」，避免逐条查库）。
	 *
	 * @param userId 用户 ID
	 * @param module 模块：word / sentence / grammar
	 * @param knowledgeIds 知识点 ID 列表
	 * @return 键为知识点 ID，值为按使用时间升序的角度列表
	 */
	java.util.Map<String, List<String>> angleUsed(String userId, String module, List<String> knowledgeIds);

	/**
	 * 某单元的小节级三阶进度。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 册别
	 * @param unit 单元
	 * @return 按小节聚合的进度，未分节固定置末
	 */
	List<EnglishSectionProgressView> sectionProgress(String userId, String version, String grade, String term,
			String unit);

	/**
	 * 某单元的复习候选知识点（限定本单元，不跨单元）。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 册别
	 * @param unit 单元
	 * @param module 模块：word / sentence / grammar；为空返回全部
	 * @return 候选列表，按薄弱优先、到期次之排序
	 */
	List<EnglishReviewCandidateView> reviewCandidates(String userId, String version, String grade, String term,
			String unit, String module);

	/**
	 * 批量计算整册各单元的单词通关情况，供单元列表页一次取回，避免逐单元查库。
	 *
	 * <p>返回的视图只填充单元级汇总字段（wordPass / wordPassRate / sections），
	 * 其余字段保持空值，由调用方按需合并。</p>
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 册别
	 * @return 键为单元名的通关汇总
	 */
	java.util.Map<String, EnglishUnitProgressView> wordPassSummary(String userId, String version, String grade,
			String term);

	/**
	 * 单元通关判定（单词按小节、句式与语法按单元）。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 册别
	 * @param unit 单元
	 * @return 单元进度视图，含通关状态与待复习数
	 */
	EnglishUnitProgressView unitPass(String userId, String version, String grade, String term, String unit);
}
