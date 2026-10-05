package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 薄弱点变化事件实体（对应 t_weak_point_event）。
 *
 * <p>精准破弱模型（WPB）的「留痕」载体：记录薄弱知识点被发现（discovered）、
 * 被攻克（conquered）、复现（reopened）的跃迁；仅在状态跃迁时产生，
 * 事件幂等键为「学员 + 知识点 + 事件类型 + 业务来源ID」。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
@TableName("t_weak_point_event")
@EqualsAndHashCode(callSuper = false)
public class WeakPointEvent extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 学科ID */
	private String subjectId;

	/** 知识点ID */
	private String knowledgePointId;

	/** 小节ID */
	private String sectionId;

	/** 章节ID */
	private String chapterId;

	/** 事件类型 discovered / conquered / reopened */
	private String eventType;

	/** 事件发生时该知识点掌握度 0-100 */
	private Integer mastery;

	/** 来源 detect / practice / correction */
	private String source;

	/** 业务来源ID（幂等键，如 detect:xxx / practice:xxx / correct:xxx） */
	private String refId;

	/** 知识点名称快照 */
	private String kpName;

	/** 小节名称快照 */
	private String sectionName;

	/** 章节名称快照 */
	private String chapterName;

	/** 发生时间 */
	private Date occurredAt;

}
