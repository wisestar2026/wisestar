package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 薄弱知识点研判实体（对应数据库表 t_user_weak_knowledge，active/conquered）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
@TableName("t_user_weak_knowledge")
@EqualsAndHashCode(callSuper = false)
public class UserWeakKnowledge extends BaseModel {

	/** 学员ID */
	private String userId;

	/** 学科ID */
	private String subjectId;

	/** 知识点ID */
	private String knowledgePointId;

	/** 状态 active/conquered */
	private String status;

	/** 攻克次数 */
	private Integer conquerTimes;

	/** 首次薄弱时间 */
	private Date firstWeakAt;

	/** 最近攻克时间 */
	private Date conqueredAt;

}
