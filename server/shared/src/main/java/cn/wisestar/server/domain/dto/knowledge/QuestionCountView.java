package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.Map;

/**
 * 学科维度的「归属题目数量」视图（教研平台知识树名称后展示）。
 *
 * <p>按题目的章节/小节/知识点标签文本精确匹配（忽略大小写与首尾空格）统计，
 * 三张映射的键为知识树节点名称、值为题库中命中该名称的题目数。名称在学科内重名时
 * 共用同一计数。</p>
 *
 * @author wisestar
 * @date 2026/10/07
 */
@Data
public class QuestionCountView {

	/**
	 * 章节名 → 归属题目数（题目顶层 chapter 标签或 template JSON 快照命中）。
	 */
	private Map<String, Long> chapter;

	/**
	 * 小节名 → 归属题目数。
	 */
	private Map<String, Long> section;

	/**
	 * 知识点名 → 归属题目数。
	 */
	private Map<String, Long> knowledgePoint;

}
