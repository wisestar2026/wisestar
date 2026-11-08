package cn.wisestar.server.core.uitls;

/**
 * 英语分阶段通关阈值常量。
 *
 * <p>单词模块的三阶阈值、掌握三态分界与单元通关门槛统一维护在
 * {@link cn.wisestar.server.impl.EnglishMastery}（与既有掌握度算法同处一地）。
 * 本类只承载句式与语法模块的阈值，避免同一批数字散落在服务实现与前端页面中。</p>
 *
 * <p>统计粒度：句式按单元重点句式集合，语法按语法知识点。前端不得硬编码同类数字，
 * 一律由后端接口返回的进度与达标状态驱动展示。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
public final class EnglishPassRule {

	private EnglishPassRule() {
	}

	/** 句式学习：听音组句正确率（百分比） */
	public static final int SENTENCE_LISTEN_BUILD_RATE = 85;

	/** 句式学习：连词成句正确率（百分比） */
	public static final int SENTENCE_BUILD_RATE = 85;

	/** 句式学习：词汇填空正确率（百分比，输出题） */
	public static final int SENTENCE_FILL_RATE = 75;

	/** 句式练习：三个选择题型各自正确率（百分比） */
	public static final int SENTENCE_CHOICE_RATE = 80;

	/** 句式练习：句子默写正确率（百分比，输出题） */
	public static final int SENTENCE_DICTATION_RATE = 70;

	/** 情景对话：正确率（百分比） */
	public static final int SENTENCE_SCENARIO_RATE = 75;

	/** 语法记忆 / 练习：识别类正确率（百分比） */
	public static final int GRAMMAR_RECOGNITION_RATE = 80;

	/** 语法运用：输出类正确率（百分比） */
	public static final int GRAMMAR_OUTPUT_RATE = 70;

	/** 薄弱判定：正确率口径的最小样本量，样本不足时只看连续答错 */
	public static final int WEAK_MIN_SAMPLE = 3;

	/** 薄弱判定：知识点累计正确率下限（百分比），低于该值判为薄弱 */
	public static final int WEAK_ACCURACY_RATE = 60;

	/** 薄弱判定：连续答错达到该次数即判为薄弱 */
	public static final int WEAK_CONSECUTIVE_WRONG = 2;

	/** 攻克判定：需要连续答对的不同派生题数量 */
	public static final int BREAKTHROUGH_CONSECUTIVE_CORRECT = 2;
}
