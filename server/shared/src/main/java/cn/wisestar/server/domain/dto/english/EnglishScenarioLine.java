package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语情景对话单行（一句台词）DTO。
 *
 * <p>英文文本 {@link #en} 中用空位标记 {@code ___} 表示被挖空、待学员填写的目标单词；
 * {@link #answers} 按空位出现顺序给出每空的标准单词（小写）。无空的台词 answers 为空数组。</p>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Data
public class EnglishScenarioLine {

	/** 说话人（A / B 等，默认 A） */
	private String speaker;

	/** 英文台词（可能含若干 {@code ___} 空位） */
	private String en;

	/** 中文翻译 */
	private String zh;

	/** 各空位标准答案（小写，按出现顺序）；无空时为空数组 */
	private List<String> answers = new ArrayList<>();

}
