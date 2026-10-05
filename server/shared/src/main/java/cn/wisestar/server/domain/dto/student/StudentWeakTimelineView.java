package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.Date;

/**
 * 薄弱点变化事件视图（精准破弱「留痕」时间线）。
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class StudentWeakTimelineView {

	/** 事件类型 discovered / conquered / reopened */
	private String eventType;

	/** 知识点ID */
	private String kpId;

	/** 知识点名称快照 */
	private String kpName;

	/** 小节名称快照 */
	private String sectionName;

	/** 章节名称快照 */
	private String chapterName;

	/** 事件发生时掌握度 */
	private Integer mastery;

	/** 来源 detect / practice / correction */
	private String source;

	/** 发生时间 */
	private Date occurredAt;

}
