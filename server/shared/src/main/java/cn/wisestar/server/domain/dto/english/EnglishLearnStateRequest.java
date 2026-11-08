package cn.wisestar.server.domain.dto.english;

import lombok.Data;

/**
 * 英语作答状态上报请求。
 *
 * <p>用于前端在逐题作答后同步知识点状态；服务端按模块与角度累计三态、薄弱与攻克计数。
 * 统一入口的出发点是让「同一个知识点换角度作答」也能被规则服务感知，
 * 而不必每个题型各写一套统计代码。</p>
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
public class EnglishLearnStateRequest {

	/** 模块：word / sentence / grammar */
	private String module;

	/** 知识点 ID：单词 word_id、句子 sentence_id、语法 grammar_point_id */
	private String knowledgeId;

	/** 考察角度，为空时由服务端按模块取默认角度 */
	private String angle;

	/** 是否答对 */
	private Boolean correct;

	/** 阶段：memory / practice / use */
	private String stage;

	/** 是否首次作答，false 表示重做（不计入阶段统计） */
	private Boolean firstAttempt;
}
