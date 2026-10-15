package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.List;

/**
 * 英语·单元句子同步练习题目（多题型）。
 *
 * <p>由 AI 依据单元重点句子生成、经管理端固化落库后供学员端只读使用。</p>
 *
 * @author wisestar
 * @date 2026/10/15
 */
@Data
public class EnglishSentenceExercise {

	/**
	 * 题型：
	 * <ul>
	 *   <li>{@code build} 连词成句（给中文与正确英文，学员拖词块拼句）</li>
	 *   <li>{@code dictation} 句子默写（给中文，学员写出英文）</li>
	 *   <li>{@code choice} 句子选择（给中文题干与 4 个英文选项，选正确的）</li>
	 * </ul>
	 */
	private String kind;

	/** 中文提示 / 题干 */
	private String zh;

	/** 正确英文句子（build/dictation 的答案，choice 亦为正确项） */
	private String en;

	/** 选择题干扰与正确项（仅 kind=choice，恰好 4 个） */
	private List<String> options;

	/** 选择题正确选项文本（仅 kind=choice，等于 en） */
	private String answer;

	/** 可选提示（如默写题首字母提示） */
	private String hint;

	/** 可选中文解析 */
	private String explanation;

	/** 来源句子 ID（便于溯源，可为空） */
	private String sentenceId;

}
