package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishReadingView;

/**
 * 英语单元阅读理解服务（按单元生成并缓存短文与题目）。
 *
 * @author wisestar
 * @date 2026/10/18
 */
public interface EnglishReadingService {

	/**
	 * 读取单元阅读理解；首次访问时生成并缓存到单元情景内容包。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @return 阅读理解内容
	 */
	EnglishReadingView getReading(String version, String grade, String term, String unit);

}
