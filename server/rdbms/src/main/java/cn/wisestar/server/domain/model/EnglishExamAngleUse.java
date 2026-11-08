package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语考察角度使用记录实体（对应数据库表 t_english_exam_angle_use）。
 *
 * 【类职责】
 * 记录某学员在某知识点上已使用过的考察角度与次数，供复习取题时轮换角度，
 * 避免同一知识点连续推送同一考法的派生题（「换汤不换药」）。
 *
 * 【被谁调用】
 * EnglishExamAngleService。
 *
 * @author wisestar
 * @date 2026/11/05
 */
@Data
@TableName("t_english_exam_angle_use")
@EqualsAndHashCode(callSuper = false)
public class EnglishExamAngleUse extends BaseModel {

	/** 用户 ID */
	private String userId;

	/** 模块：word / sentence / grammar */
	private String module;

	/** 知识点 ID */
	private String knowledgeId;

	/** 考察角度编码 */
	private String angle;

	/** 使用次数 */
	private Integer useCount;

	/** 最近使用时间 */
	private java.util.Date lastUseAt;
}
