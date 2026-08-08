package cn.wisestar.server.flow.domain.model;

import cn.wisestar.server.flow.domain.dto.FlowNodeSetting;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * 已发布的流程节点实体（对应表 t_flow_entry_node）。
 *
 * <p>职责：存储某个流程定义部署后每个审批节点的持久化配置：审批人（identity）、
 * 字段权限（fieldPermission）、节点行为设置（setting）、任务类型、条件表达式等。
 * 节点 ID（id）与 BPMN XML 中的 activityId 一致，引擎运行到某节点时
 * （TaskHelper.getUsers、ActivityStartedListener、审批处理等）按节点 ID 读取配置。</p>
 *
 * <p>所属流程环节：流程部署环节（saveFlowElement 落库）与审批处理环节（读取配置）。</p>
 *
 * <p>被谁调用：FlowEntryNodeService 及其实现（CRUD）、FlowServiceImpl
 * （部署保存节点/权限过滤/审核记录节点名翻译）、AbstractTaskHandler（答案合并权限）、
 * TaskHelper（计算审批人）、ActivityStartedListener（审批阶段名）、
 * RevertTaskHandler（驳回节点名）等。</p>
 *
 * <p>依赖什么：{@link FlowNodeSetting}（节点行为设置，Jackson JSON 存储）、
 * {@link JacksonTypeHandler}（field_permission / setting / identity 列 JSON 转换）。</p>
 *
 * @TableName t_flow_entry_node
 */
@TableName(value = "t_flow_entry_node", autoResultMap = true)
@Data
public class FlowEntryNode implements Serializable {

	/**
	 * 节点id：与 BPMN XML 中的 activityId 一致，作为节点配置的定位键
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 节点名称：审批节点中文名（如"部门主管审批"），展示在审批阶段与审核记录中
	 */
	@TableField(value = "name")
	private String name;

	/**
	 * 项目id：所属问卷/项目 ID（即流程定义 key）
	 */
	@TableField(value = "project_id")
	private String projectId;

	/**
	 * 流程节点类型：任务类型（发起/用户任务/抄送等），见 {@link cn.wisestar.server.flow.constant.FlowTaskType}
	 */
	@TableField(value = "task_type")
	private Integer taskType;

	/**
	 * 字段权限：题目 ID → 权限值（0 隐藏 / 1 只读 / 2 可编辑）映射，
	 * JSON 存储，审批人加载表单与答案时按此过滤
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, value = "field_permission")
	private LinkedHashMap<String, Integer> fieldPermission;

	/**
	 * 流程设置：节点行为配置（是否可撤回、审批方式、流程日志可见性等），JSON 存储
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, value = "setting")
	private FlowNodeSetting setting;

	/**
	 * 授权用户：节点审批人集合，元素格式 U:用户ID / R:角色ID / P:岗位ID，
	 * JSON 存储，TaskHelper.getUsers 据此计算最终审批人列表
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, value = "identity")
	private String[] identity;

	/**
	 * 表达式：节点流转条件表达式（预留字段）
	 */
	@TableField(value = "expression")
	private String expression;

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
