package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 知识点统计查询条件
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
@Data
public class KnowledgePointQuery {

	/**
	 * 学生 id（为空时统计当前登录用户）
	 */
	private String studentId;

	/**
	 * 学科
	 */
	private String subject;

	/**
	 * 章节
	 */
	private String chapter;

	/**
	 * 知识点（精确匹配）
	 */
	private String knowledgePoint;

}
