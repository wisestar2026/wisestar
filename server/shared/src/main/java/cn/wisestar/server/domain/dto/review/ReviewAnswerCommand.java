package cn.wisestar.server.domain.dto.review;

import lombok.Data;

/**
 * 记忆调度作答入参。
 *
 * <p>{@code module} 取 {@code word/sentence/grammar/kp}；{@code knowledgeId} 为单词、句子、
 * 语法点或知识点 ID。{@code correctRate} 仅在知识点级按会话上报时使用，单题作答留空。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
public class ReviewAnswerCommand {

	/** 模块：word/sentence/grammar/kp */
	private String module;

	/** 学科ID，知识点级必填 */
	private String subjectId;

	/** 知识点ID */
	private String knowledgeId;

	/** 知识点名称快照，可为空 */
	private String knowledgeName;

	/** 考察角度编码，可为空 */
	private String angle;

	/** 作答方式：recognition/cued/application/full，为空时按角度推断 */
	private String demand;

	/** 是否正确 */
	private Boolean correct;

	/** 作答耗时（毫秒），可为空 */
	private Long latencyMs;

	/** 是否使用了提示 */
	private Boolean hintUsed;

	/** 是否经过重做后答对 */
	private Boolean retried;

	/** 是否本轮首次作答 */
	private Boolean firstAttempt;

	/** 会话ID，用于日志归集 */
	private String sessionId;

	/** 会话正确率 0-100，知识点级上报使用 */
	private Integer correctRate;

	/** 会话题量，知识点级上报使用 */
	private Integer total;

}
