package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 词典查询结果 DTO（音标 / 释义 / 例句）。
 *
 * @author wisestar
 * @date 2026/9/18
 */
@Data
public class DictionaryEntryView {

	/** 查询的单词 */
	private String word;

	/** 音标（不含斜杠，如 ˈæp(ə)l） */
	private String phonetic;

	/** 中文释义（如 n. 苹果；苹果树） */
	private String meaning;

	/** 英文例句 */
	private String exampleSentence;

}
