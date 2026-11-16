package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 记忆复习日志实体（对应 t_memory_review_log）。
 *
 * <p>每次作答一条，保存评分、作答方式、用时与复习前后的难度/稳定度/可提取度，
 * 供薄弱归因、调度解释与后续 FSRS 参数拟合使用。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
@TableName("t_memory_review_log")
@EqualsAndHashCode(callSuper = false)
public class MemoryReviewLog extends BaseModel {

	/** 用户ID */
	private String userId;

	/** 模块 */
	private String module;

	/** 学科ID */
	private String subjectId;

	/** 知识点ID */
	private String knowledgeId;

	/** 评分 1~4 */
	private Integer grade;

	/** 是否正确 */
	private Integer correct;

	/** 作答耗时（毫秒） */
	private Integer latencyMs;

	/** 考察角度 */
	private String angle;

	/** 作答方式 */
	private String demand;

	/** 复习前稳定度 */
	private Double stabilityBefore;

	/** 复习后稳定度 */
	private Double stabilityAfter;

	/** 复习前难度 */
	private Double difficultyBefore;

	/** 复习后难度 */
	private Double difficultyAfter;

	/** 复习前可提取度 */
	private Double retrievabilityBefore;

	/** 距上次复习天数 */
	private Double elapsedDays;

	/** 本次安排间隔（天） */
	private Double scheduledDays;

	/** 目标保持率 */
	private Double targetRetention;

	/** 会话ID */
	private String sessionId;

	/** 复习时间 */
	private Date reviewedAt;

}
