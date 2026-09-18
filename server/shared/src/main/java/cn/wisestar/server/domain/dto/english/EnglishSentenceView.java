package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语句子视图 DTO。
 *
 * @author wisestar
 * @date 2026/9/15
 */
@Data
public class EnglishSentenceView {

	private String id;

	/** 英文句子 */
	private String en;

	/** 中文释义 */
	private String zh;

	/** 音频 URL */
	private String audioUrl;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 小节 */
	private String section;

	/** 单元内排序 */
	private Integer sort;

	/** 熟练度（用户句子本，未学习为 0） */
	private Integer familiarity;

	/** 累计答对次数 */
	private Integer correctCount;

	/** 累计答错次数 */
	private Integer wrongCount;

}
