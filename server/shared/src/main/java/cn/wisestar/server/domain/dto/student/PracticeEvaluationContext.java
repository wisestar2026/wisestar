package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 一次练习的学情评价上下文（跨模块传递，避免 shared 依赖 rdbms 实体）。
 *
 * <p>由练习交卷方（rdbms）从逐题明细与题目 schema 组装后交给评价服务。</p>
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class PracticeEvaluationContext {

	/** 学员ID */
	private String userId;

	/** 练习会话ID */
	private String practiceId;

	/** 会话级知识点ID（可空） */
	private String knowledgePointId;

	/** 会话级小节ID（可空） */
	private String sectionId;

	/** 逐题结果 */
	private List<Item> items = new ArrayList<>();

	@Data
	public static class Item {

		/** 题目ID */
		private String questionId;

		/** 是否答对 */
		private boolean correct;

		/** 题目绑定的知识点名称（可为空） */
		private List<String> knowledgePointNames;

	}

}
