package cn.wisestar.server.core.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 记忆调度常量（模块与作答方式）。
 *
 * <p>模块回答「这是哪类知识点」：英语单词、句式、语法，或其他学科的章节知识点。
 * 作答方式回答「这道题要求学员回忆还是产出」，用于评分与严重度加权：</p>
 *
 * <ul>
 *   <li>{@link #DEMAND_RECOGNITION}：选择、判断、听选，只要求再认；</li>
 *   <li>{@link #DEMAND_CUED}：带中文提示或词库的填空；</li>
 *   <li>{@link #DEMAND_APPLICATION}：连词成句、改写，需要组织语言；</li>
 *   <li>{@link #DEMAND_FULL}：默写或无提示产出。</li>
 * </ul>
 *
 * @author wisestar
 * @date 2026/11/16
 */
public final class ReviewConsts {

	private ReviewConsts() {
	}

	/** 模块：英语单词 */
	public static final String MODULE_WORD = "word";

	/** 模块：英语句式 */
	public static final String MODULE_SENTENCE = "sentence";

	/** 模块：英语语法 */
	public static final String MODULE_GRAMMAR = "grammar";

	/** 模块：学科知识点 */
	public static final String MODULE_KP = "kp";

	/** 全部模块 */
	public static final List<String> MODULES = Collections.unmodifiableList(
			Arrays.asList(MODULE_WORD, MODULE_SENTENCE, MODULE_GRAMMAR, MODULE_KP));

	/** 作答方式：再认 */
	public static final String DEMAND_RECOGNITION = "recognition";

	/** 作答方式：带提示填空 */
	public static final String DEMAND_CUED = "cued";

	/** 作答方式：组织与改写 */
	public static final String DEMAND_APPLICATION = "application";

	/** 作答方式：无提示产出 */
	public static final String DEMAND_FULL = "full";

	/** 调度状态：新项 */
	public static final String STATE_NEW = "new";

	/** 调度状态：学习中 */
	public static final String STATE_LEARNING = "learning";

	/** 调度状态：复习中 */
	public static final String STATE_REVIEW = "review";

	/** 调度状态：重新学习 */
	public static final String STATE_RELEARNING = "relearning";

	/** 薄弱原因：可提取度不足 */
	public static final String REASON_LOW_RETRIEVABILITY = "low_retrievability";

	/** 薄弱原因：连续答错 */
	public static final String REASON_CONSECUTIVE_WRONG = "consecutive_wrong";

	/** 薄弱原因：输出题错误 */
	public static final String REASON_OUTPUT_ERROR = "output_error";

	/** 薄弱原因：攻克后复发 */
	public static final String REASON_LAPSE = "lapse";

	/** 薄弱原因：答对但不稳（靠提示、超时或刚过线） */
	public static final String REASON_FRAGILE = "fragile";

	/** 薄弱原因：累计正确率过低 */
	public static final String REASON_LOW_ACCURACY = "low_accuracy";

	/** 队列来源：到期 */
	public static final String SOURCE_DUE = "due";

	/** 队列来源：薄弱 */
	public static final String SOURCE_WEAK = "weak";

	/** 队列来源：新项 */
	public static final String SOURCE_NEW = "new";

}
