package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 记忆调度项实体（对应 t_memory_schedule）。
 *
 * <p>一条记录回答「这个知识点现在有多难、记得多牢、什么时候该复习、是否在薄弱池」。
 * FSRS-5 维护 difficulty（1~10）与 stability（天），retrievability 不落库、读取时按
 * elapsed 实时计算；历史不足时用 SM-2 冷启动。</p>
 *
 * @author wisestar
 * @date 2026/11/16
 */
@Data
@TableName("t_memory_schedule")
@EqualsAndHashCode(callSuper = false)
public class MemorySchedule extends BaseModel {

	/** 用户ID */
	private String userId;

	/** 模块：word/sentence/grammar/kp */
	private String module;

	/** 学科ID（知识点级必填，英语模块可为空） */
	private String subjectId;

	/** 知识点ID：单词/句子/语法点/学科知识点 */
	private String knowledgeId;

	/** 调度状态：new/learning/review/relearning */
	private String state;

	/** 难度 1~10 */
	private Double difficulty;

	/** 稳定度（天） */
	private Double stability;

	/** 累计复习次数 */
	private Integer reps;

	/** 累计失误次数 */
	private Integer lapses;

	/** 连续答错次数 */
	private Integer consecutiveWrong;

	/** 累计作答次数 */
	private Integer answers;

	/** 累计答对次数 */
	private Integer corrects;

	/** SM-2 ease factor（冷启动用） */
	private Double easeFactor;

	/** 最近一次评分 1~4 */
	private Integer lastGrade;

	/** 最近一次考察角度 */
	private String lastAngle;

	/** 最近一次作答方式 */
	private String lastDemand;

	/** 最近一次复习时间 */
	private Date lastReviewAt;

	/** 下次到期时间 */
	private Date dueAt;

	/** 上次安排间隔（天） */
	private Double scheduledDays;

	/** 距上次复习的天数 */
	private Double elapsedDays;

	/** 输出类作答累计错误次数 */
	private Integer outputWrong;

	/** 是否在薄弱池 */
	private Integer weakFlag;

	/** 薄弱原因 */
	private String weakReason;

	/** 首次进入薄弱池时间 */
	private Date firstWeakAt;

	/** 最近一次攻克时间 */
	private Date conqueredAt;

	/** 攻克后复发次数 */
	private Integer reclaimCount;

}
