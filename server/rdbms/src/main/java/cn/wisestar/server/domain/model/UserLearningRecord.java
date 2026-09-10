package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 学习行为记录实体（对应数据库表 t_user_learning_record，奖励发放与防刷依据）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@TableName("t_user_learning_record")
@EqualsAndHashCode(callSuper = false)
public class UserLearningRecord extends BaseModel {

	/** 学员ID */
	private String userId;

	/** 学科ID，可空 */
	private String subjectId;

	/** 知识点ID，可空 */
	private String knowledgePointId;

	/** 小节ID，可空 */
	private String sectionId;

	/** 学习行为类型 */
	private String actionType;

	/** 业务关联ID（练习会话/日期等） */
	private String refId;

	/** 本次发放学习币（上限裁剪后实际值） */
	private Integer coins;

	/** 本次发放学海积分 */
	private Integer points;

	/** 学期键 */
	private String semester;

	/** 行为发生时间 */
	private Date learnedAt;

}
