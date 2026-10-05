package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识点检测记录实体（对应 t_detect_record）。
 *
 * <p>精准破弱模型（WPB）的「定基」载体：学员某学科本学期首次全面检测标为
 * PRE 且 {@code isBaseline=1}，其薄弱点集合作为成长基线；其余为 STAGE。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
@TableName("t_detect_record")
@EqualsAndHashCode(callSuper = false)
public class DetectRecord extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 学科ID */
	private String subjectId;

	/** 学科名称快照 */
	private String subjectName;

	/** 年级快照 */
	private String grade;

	/** 册别快照 */
	private String term;

	/** 学期键（如 2026-1） */
	private String semester;

	/** 检测类型 PRE / STAGE */
	private String detectType;

	/** 是否成长基线 */
	private Boolean isBaseline;

	/** 基线唯一键（仅 PRE 写入：studentId:subjectId:semester） */
	private String baselineKey;

	/** 勾选章节ID JSON 数组 */
	private String chapterIds;

	/** 勾选章节名称 JSON 数组 */
	private String chapterNames;

	/** 组卷题量 */
	private Integer questionCount;

	/** 实际判分题数 */
	private Integer total;

	/** 正确题数 */
	private Integer correctCount;

	/** 正确率 0-100 */
	private Integer accuracy;

	/** 作答耗时（ms） */
	private Long durationMs;

	/** 薄弱点 JSON 数组 */
	private String weakPoints;

	/** 逐题结果 JSON 数组 */
	private String details;

	/** 客户端幂等令牌 */
	private String clientToken;

}
