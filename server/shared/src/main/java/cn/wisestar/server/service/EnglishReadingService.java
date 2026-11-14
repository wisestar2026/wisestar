package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishReadingView;

import java.util.List;

/**
 * 英语单元阅读理解服务（按单元读取多篇短文与题目，缺失时生成并缓存）。
 *
 * @author wisestar
 * @date 2026/10/18
 */
public interface EnglishReadingService {

	/**
	 * 读取单元阅读理解文章列表。
	 *
	 * <p>优先读取已固化的多篇阅读内容；单元无内容时生成 1 篇并缓存。</p>
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @return 按文章序号升序的阅读文章
	 */
	List<EnglishReadingView> getReadings(String version, String grade, String term, String unit);

	/**
	 * 批量生成并固化单元阅读理解文章（后台内容准备）。
	 *
	 * <p>逐篇调用 AI 生成，每篇保留前 4 道选择题，结果整体写入该单元的
	 * {@code reading_content} 覆盖旧内容。</p>
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @param count   生成篇数（1~5）
	 * @return 生成并按文章序号排序的文章列表
	 */
	List<EnglishReadingView> generateArticles(String version, String grade, String term, String unit, int count);

}
