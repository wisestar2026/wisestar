package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * 学习轨迹事件视图（成长时间轴条目）。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class GrowthEventView {

	/** 事件ID */
	private String id;

	/** 事件类型 PRACTICE / DETECT / GRAMMAR / CLASS */
	private String eventType;

	/** 来源类型 practice / detect / grammar / archive_record */
	private String sourceType;

	/** 学科ID */
	private String subjectId;

	/** 学科名称快照 */
	private String subjectName;

	/** 发生日期 yyyy-MM-dd */
	private String eventDate;

	/** 发生时间 */
	private Date occurredAt;

	/** 章节/单元名称 */
	private String chapter;

	/** 知识点名称列表 */
	private List<String> knowledgePoints;

	/** 题量 */
	private Integer questionCount;

	/** 正确数 */
	private Integer correctCount;

	/** 正确率 0-100 */
	private Integer accuracy;

	/** 时长(ms) */
	private Long durationMs;

	/** 积分 */
	private Integer points;

	/** 学习币 */
	private Integer coins;

	/** 事件标题 */
	private String title;

	/** 备注 */
	private String remark;

}
