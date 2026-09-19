package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.english.DictionaryEntryView;

import java.util.List;

/**
 * 英语词典查询服务（免费公开词典接口）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
public interface EnglishDictionaryService {

	/**
	 * 查询单词的音标、释义与例句。
	 *
	 * @param word 要查询的英文单词
	 * @return 词典条目；无结果时返回 {@code null}
	 */
	DictionaryEntryView lookup(String word);

	/**
	 * 查询单词的配图候选地址。
	 *
	 * @param word 要查询的英文单词
	 * @return 候选图片地址列表；无结果时返回空列表
	 */
	List<String> lookupImages(String word);

}
