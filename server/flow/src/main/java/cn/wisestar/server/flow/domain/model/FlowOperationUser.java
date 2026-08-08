package cn.wisestar.server.flow.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程操作人关联实体（对应表 t_flow_operation_user）。
 *
 * <p>职责：记录"某条操作记录由哪些用户参与处理"。主要用于已办事项的归属判断：
 * 已办列表通过 exists 子查询（latest=1 且 user_id=当前用户）过滤出当前用户处理过
 * 的操作；同时 latest 字段保证同一用户在同一实例中只显示最近参与的节点。</p>
 *
 * <p>所属流程环节：审批处理环节（saveOperationUser 落库、updateOperationUserLatest
 * 置历史）与已办列表查询环节。</p>
 *
 * <p>被谁调用：FlowOperationUserService 及其实现（CRUD）、AbstractTaskHandler
 * （保存操作人）、FlowOperationMapper.updateOperationUserLatest（置为历史）。</p>
 *
 * <p>依赖什么：无（纯实体，无特殊类型处理器）。</p>
 *
 * @TableName t_flow_operation_user
 */
@TableName(value = "t_flow_operation_user")
@Data
public class FlowOperationUser implements Serializable {

	/**
	 * 节点id：主键
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 操作id：关联的操作记录（t_flow_operation.id）
	 */
	@TableField(value = "operation_id")
	private String operationId;

	/**
	 * 用户id：参与操作的用户
	 */
	@TableField(value = "user_id")
	private String userId;

	/**
	 * 组id：用户所属组/角色（预留字段）
	 */
	@TableField(value = "group_id")
	private String groupId;

	/**
	 * 用户类型：关联方式（用户/角色等，预留字段）
	 */
	@TableField(value = "link_type")
	private String linkType;

	/**
	 * 是否最新记录：同一用户在同一流程实例中仅一条 latest=true，
	 * 用于已办列表去重（只显示最近参与的节点）
	 */
	@TableField(value = "latest")
	private Boolean latest;

	/**
	 * 创建时间
	 */
	@TableField(value = "create_at")
	private Date createAt;

	/**
	 * 创建人
	 */
	@TableField(value = "create_by")
	private String createBy;

	/**
	 * 更新时间
	 */
	@TableField(value = "update_at")
	private Date updateAt;

	/**
	 * 更新人
	 */
	@TableField(value = "update_by")
	private String updateBy;

	/** 序列化版本号（不参与表字段映射） */
	@TableField(exist = false)
	private static final long serialVersionUID = 1L;

}
