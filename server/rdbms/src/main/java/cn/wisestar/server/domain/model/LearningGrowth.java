package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学习轨迹事件实体（对应 t_learning_growth）。
 *
 * <p>学员成长档案的「学习轨迹」载体：每次已落库的学习行为（在线练习、单元检测、
 * 英语语法练习、上课记录）追加一条事件；写入独立于交卷主事务，幂等键为
 * 「学员 + 来源类型 + 业务对象ID」，语法类按「grammarId + 日期」累加。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
@TableName("t_learning_growth")
@EqualsAndHashCode(callSuper = false)
public class LearningGrowth extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 学科ID */
	private String subjectId;

	/** 学科名称快照 */
	private String subjectName;

	/** 事件类型 PRACTICE / DETECT / GRAMMAR / CLASS */
	private String eventType;

	/** 来源类型 practice / detect / grammar / archive_record */
	private String sourceType;

	/** 业务对象ID（幂等键） */
	private String sourceId;

	/** 发生日期 yyyy-MM-dd */
	private String eventDate;

	/** 发生时间 */
	private Date occurredAt;

	/** 章节/单元ID */
	private String chapterId;

	/** 章节/单元名称 */
	private String chapter;

	/** 知识点名称 JSON 数组 */
	private String knowledgePoints;

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
