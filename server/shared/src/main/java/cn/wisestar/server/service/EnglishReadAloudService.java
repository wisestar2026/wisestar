package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.EnglishReadAloudView;

import java.util.List;

/**
 * 英语课文跟读服务（按单元读取课本篇章与逐句译文）。
 *
 * @author wisestar
 * @date 2026/10/19
 */
public interface EnglishReadAloudService {

	/**
	 * 读取单元课文跟读篇章。
	 *
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @return 按 sort 升序的篇章列表
	 */
	List<EnglishReadAloudView> getPassages(String version, String grade, String term, String unit);

}
