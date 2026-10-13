package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 单词情景分组输入 DTO（分组子集内的单个单词）。
 *
 * <p>内容包服务把单元单词集合分组后，以本 DTO 传递分组子集给情景生成服务，
 * 避免 shared 模块直接依赖 rdbms 实体 {@code EnglishWord}。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Data
public class EnglishScenarioWord {

	/** 单词拼写 */
	private String spell;

	/** 释义 */
	private String meaning;

	public EnglishScenarioWord() {
	}

	public EnglishScenarioWord(String spell, String meaning) {
		this.spell = spell;
		this.meaning = meaning;
	}

}
