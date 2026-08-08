package cn.wisestar.server.flow.domain.model;

import cn.wisestar.server.flow.domain.handler.FlowEntryNodeTypeHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 流程定义实体（对应表 t_flow_entry）。
 *
 * <p>职责：存储某个问卷绑定的流程定义信息，包括流程 BPMN XML（bpmnXml）、
 * 部署后的引擎定义 ID 与部署 ID、临时节点配置（nodes JSON）、发布状态等。
 * 一份流程定义对应一个问卷（projectId 即流程定义 key），可被多次部署生成多个版本。</p>
 *
 * <p>所属流程环节：流程设计环节（保存草稿、部署发布、查询回显）。</p>
 *
 * <p>被谁调用：FlowEntryService 及其实现（CRUD）、FlowServiceImpl
 * （保存/部署/查询）、AbstractTaskHandler（getFlowEntry 查询流程定义）、
 * FlowEntryNodeTypeHandler（nodes 字段 JSON 转换）。</p>
 *
 * <p>依赖什么：{@link FlowEntryNodeTypeHandler}（nodes 列 JSON 类型处理器）。</p>
 *
 * @TableName t_flow_entry
 */
@TableName(value = "t_flow_entry", autoResultMap = true)
@Data
public class FlowEntry implements Serializable {

	/**
	 * 主键：流程定义记录 ID（部署时作为 BPMN 资源文件名前缀使用）
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 项目id, 等于流程定义key：绑定问卷/项目，启动流程时按该 key 定位流程定义
	 */
	@TableField(value = "project_id")
	private String projectId;

	/**
	 * 流程定义 ID = processKey:version:deploymentId：Flowable 引擎生成的流程定义唯一标识，
	 * 部署成功后回填，用于查询 BPMN 模型
	 */
	@TableField(value = "process_definition_id")
	private String processDefinitionId;

	/**
	 * 部署id：Flowable 引擎的部署记录 ID，部署成功后回填
	 */
	@TableField(value = "deploy_id")
	private String deployId;

	/**
	 * 流程XML：前端流程设计器生成的 BPMN 2.0 XML，部署时解析为 BpmnModel
	 */
	@TableField(value = "bpmn_xml")
	private String bpmnXml;

	/**
	 * 临时节点：流程设计器画布上的节点配置列表（未发布前的草稿态节点），
	 * 以 JSON 存储在 nodes 列，部署时由 saveFlowElement 拆解保存到 t_flow_entry_node 表
	 */
	@TableField(typeHandler = FlowEntryNodeTypeHandler.class, value = "nodes")
	private List<FlowEntryNode> nodes;

	/**
	 * 流程图标：前端展示用（预留字段）
	 */
	@TableField(value = "icon")
	private String icon;

	/**
	 * 0未发布 1已发布：流程是否已部署到引擎，见 {@link cn.wisestar.server.flow.constant.FlowEntryStatus}
	 */
	@TableField(value = "status")
	private Integer status;

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
