package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语知识点学习状态视图（shared 层传递用）。
 *
 * <p>由 {@code EnglishLearningRuleService} 返回，学员端与规则计算只读本视图，
 * 不直接接触 rdbms 实体，保持分层依赖方向。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
public class EnglishLearningStateView {

	/** 模块：word / sentence / grammar */
	private String module;

	/** 知识点 ID */
	private String knowledgeId;

	/** 掌握状态：familiar / ordinary / unfamiliar */
	private String state;

	/** 连续答对计数 */
	private Integer consecutiveCorrect;

	/** 连续答错计数 */
	private Integer consecutiveWrong;

	/** 输出类题型答对次数 */
	private Integer outputCorrectCount;

	/** 最近一次作答的考察角度 */
	private String lastAngle;

	/** 是否在薄弱池 */
	private Boolean weak;

	/** 薄弱原因编码 */
	private String weakReason;

	/** 记忆阶段答题总数 */
	private Integer memoryTotal;

	/** 记忆阶段答对总数 */
	private Integer memoryCorrect;

	/** 练习阶段答题总数 */
	private Integer practiceTotal;

	/** 练习阶段答对总数 */
	private Integer practiceCorrect;

	/** 运用阶段答题总数 */
	private Integer useTotal;

	/** 运用阶段答对总数 */
	private Integer useCorrect;
}
