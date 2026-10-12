package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语情景对话视图 DTO（学员端「场景对话练习」单个场景）。
 *
 * <p>由 AI 依据指定单元的单词实时生成，不落库；每个对话围绕一个生活场景，两名角色
 * 交替对话，并把该单元的若干目标单词从英文台词中挖空，供学员填空作答。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Data
public class EnglishScenarioView {

	/** 情景标题（中文） */
	private String title;

	/** 本对话考查的单词（小写） */
	private List<String> words = new ArrayList<>();

	/** 对话内容（按顺序的台词） */
	private List<EnglishScenarioLine> lines = new ArrayList<>();

}
