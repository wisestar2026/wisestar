package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 句式情景分组输入 DTO（分组子集内的单条重点句式）。
 *
 * <p>内容包服务把单元句式集合分组后，以本 DTO 传递分组子集给句式情景生成服务，
 * 避免 shared 模块直接依赖 rdbms 实体 {@code EnglishSentence}。</p>
 *
 * @author wisestar
 * @date 2026/10/13
 */
@Data
public class EnglishPatternSentence {

	/** 英文句式 */
	private String en;

	/** 中文释义 */
	private String zh;

	public EnglishPatternSentence() {
	}

	public EnglishPatternSentence(String en, String zh) {
		this.en = en;
		this.zh = zh;
	}

}
