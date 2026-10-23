package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 英语单元练习交卷请求（内容包阶梯卷）。
 *
 * <p>题目以内容键（contentKey）标识，后端按内容键回读句子包/情景包复核答案，
 * 不信任前端判分结果。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
public class EnglishUnitPaperSubmitRequest {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别 */
	private String term;

	/** 单元 */
	private String unit;

	/** 章节 ID */
	private String chapterId;

	/** 练习模块：unit 单元练习 / grammar 语法练习（用于练习记录与错题本归类） */
	private String module;

	/** 作答列表 */
	private List<Item> items = new ArrayList<>();

	@Data
	public static class Item {

		/** 内容键（与标签表 contentKey 一致） */
		private String contentKey;

		/** 学生答案：{type:'option',optionId} / {type:'text',text} */
		private Map<String, Object> answer;

		/** 选项改选次数（>0 表示改选后作答，用于「不稳定」判定） */
		private Integer changes;

		/** 是否使用过提示（用于「不稳定」判定） */
		private Boolean hintUsed;

	}

}
