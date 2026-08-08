package cn.wisestar.server.domain.dto;

import lombok.Data;

/**
 * 知识点统计结果：答题次数、正确次数、正确率
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
@Data
public class KnowledgePointStat {

	/**
	 * 学科
	 */
	private String subject;

	/**
	 * 章节
	 */
	private String chapter;

	/**
	 * 知识点
	 */
	private String knowledgePoint;

	/**
	 * 答题次数
	 */
	private long attempts;

	/**
	 * 正确次数
	 */
	private long correctCount;

	/**
	 * 正确率（0-1）
	 */
	private double correctRate;

}
