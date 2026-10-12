package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语重点句式情景练习单轮 DTO。
 *
 * <p>一轮有两种形态：</p>
 * <ul>
 *   <li>已给出轮次：{@link #en}/{@link #zh} 非空，供学员理解上下文；</li>
 *   <li>作答轮次：{@link #en} 为空，由 {@link #cue} 给出中文情景提示，学员从 {@link #options}
 *       中选出正确句式，{@link #answerIndex} 为正确项下标。</li>
 * </ul>
 *
 * @author wisestar
 * @date 2026/10/12
 */
@Data
public class EnglishPatternTurn {

	/** 说话人（A/B） */
	private String speaker;

	/** 已给出的英文台词（作答轮次为空） */
	private String en;

	/** 中文（已给出轮次的翻译 / 作答轮次的情景译文） */
	private String zh;

	/** 作答轮次的中文情景提示（已给出轮次为空） */
	private String cue;

	/** 候选英文句式（作答轮次；已给出轮次为空） */
	private List<String> options = new ArrayList<>();

	/** 正确选项下标；-1 表示该轮无需作答 */
	private Integer answerIndex = -1;

	/** 解析（作答轮次，简短中文说明为何选它） */
	private String explanation;

}
