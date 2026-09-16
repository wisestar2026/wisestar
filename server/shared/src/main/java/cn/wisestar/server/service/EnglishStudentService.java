package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishSentenceView;
import cn.wisestar.server.domain.dto.english.EnglishUnitProgressView;
import cn.wisestar.server.domain.dto.english.ReviewSessionView;

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
	 * 记录一次学习会话（写学习日志）。
	 *
	 * @param userId 用户 ID
	 * @param type 类型 word / sentence
	 * @param durationSeconds 学习时长（秒）
	 * @param correctCount 正确数
	 */
	void recordSession(String userId, String type, int durationSeconds, int correctCount);

}
