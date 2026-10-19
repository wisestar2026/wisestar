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

}
