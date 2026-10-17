package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishGrammarView;
import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.EnglishWrongQuestionView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;
import cn.wisestar.server.domain.dto.student.StudentPreviewCompleteView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoRequest;
import cn.wisestar.server.domain.dto.student.StudentWrongRedoView;

import java.util.List;

/**
 * 英语学员学习服务（学员端学习中心）。
 *
 * @author wisestar
 * @date 2026/9/15
 */
public interface EnglishStudentService {

	/**
	 * 单元列表 + 学习进度。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @return 单元进度列表
	 */
	List<EnglishUnitProgressView> unitProgress(String userId, String version, String grade, String term);

	/**
	 * 单元句子列表（含熟练度）。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @param unit 单元
	 * @return 句子列表
	 */
	List<EnglishSentenceView> sentences(String userId, String version, String grade, String term, String unit);

	/**
	 * 单元重点语法列表（含掌握度）。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @param unit 单元
	 * @return 语法列表（单元内 sort 升序）
	 */
	List<EnglishGrammarView> grammars(String userId, String version, String grade, String term, String unit);

	/**
	 * 记录语法作答结果（熟练度 + 复习时间 + 学习日志）。
	 *
	 * @param userId 用户 ID
	 * @param grammarId 语法 ID
	 * @param correct 是否正确
	 */
	void recordGrammar(String userId, String grammarId, boolean correct);

	/**
	 * 待学习/复习句子。
	 *
	 * @param userId 用户 ID
	 * @param limit 数量限制
	 * @return 句子列表
	 */
	List<EnglishSentenceView> studySentences(String userId, int limit);

	/**
	 * 记录句子作答结果（熟练度 + 复习时间 + 学习日志）。
	 *
	 * @param userId 用户 ID
	 * @param sentenceId 句子 ID
	 * @param correct 是否正确
	 */
	void recordSentence(String userId, String sentenceId, boolean correct);

	/**
	 * 智能复习会话（单词 + 句子混合，按到期时间升序）。
	 *
	 * @param userId 用户 ID
	 * @param limit 数量限制
	 * @return 复习队列
	 */
	List<ReviewSessionView> reviewSession(String userId, int limit);

	/**
	 * 记录一次情景练习结果（单词情景 / 句式情景），纳入「单元·模块」掌握度。
	 *
	 * @param userId 用户 ID
	 * @param version 版本
	 * @param grade 年级
	 * @param term 学期/册别
	 * @param unit 单元
	 * @param module 模块（单词/句式）
	 * @param correct 答对题数
	 * @param total 参与判定的题数
	 */
	void recordScenarioPractice(String userId, String version, String grade, String term, String unit,
			String module, int correct, int total);

	/**
	 * 记录一次学习会话（写学习日志 + 结算学习币/积分奖励）。
	 *
	 * @param userId 用户 ID
	 * @param type 类型 word / sentence / word-quiz / review / drill
	 * @param durationSeconds 学习时长（秒）
	 * @param correctCount 正确数
	 * @return 奖励结算结果
	 */
	StudentPreviewCompleteView recordSession(String userId, String type, int durationSeconds, int correctCount);

	/**
	 * 英语复习错题（从学员检测记录聚合英语学科做错的题目）。
	 *
	 * <p>只取「有提交记录」的错题：遍历该学员在本学科下的检测记录，
	 * 提取逐题明细中 {@code correct=0} 的题目，按题目去重并累计做错次数，
	 * 返回的题目快照已剥离标准答案与解析。</p>
	 *
	 * @param userId 用户 ID
	 * @param limit  数量上限（为空或非正数时不限制）
	 * @return 错题列表（最近做错的在前）
	 */
	List<EnglishWrongQuestionView> wrongQuestions(String userId, Integer limit);

	/**
	 * 取单道英语错题的重做用题目（含标准答案，仅当确为本人英语错题时返回）。
	 *
	 * @param userId     用户 ID
	 * @param questionId 题目 ID
	 * @return 含标准答案的题目视图；非本人错题或题目不存在时返回 {@code null}
	 */
	StudentQuestionView wrongQuestion(String userId, String questionId);

	/**
	 * 英语错题重做：判分正确则把该题标记为「已消灭」（落库，幂等），
	 * 之后聚合英语错题时不再返回该题。
	 *
	 * @param userId  用户 ID
	 * @param request 重做请求（题目 ID + 学员作答）
	 * @return 重做结果（ok / 是否已消灭 removed）
	 */
	StudentWrongRedoView redoWrong(String userId, StudentWrongRedoRequest request);

}
