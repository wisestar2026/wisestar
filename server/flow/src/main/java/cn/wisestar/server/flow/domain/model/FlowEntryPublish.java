package cn.wisestar.server.flow.domain.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程发布版本实体（对应表 t_flow_entry_publish）。
 *
 * <p>职责：记录流程定义的每次发布版本：关联的引擎流程定义 ID、版本号、是否主版本、
 * 是否激活、发布时间等。每次部署都会把旧版本标记为历史（mainVersion=false、
 * activeStatus=false），并新增一条当前主版本记录。</p>
 *
 * <p>所属流程环节：流程部署/发布环节（deploy 时生成与更新版本记录）。</p>
 *
 * <p>被谁调用：FlowEntryPublishService 及其实现（CRUD）、
 * FlowServiceImpl.deploy（更新旧版本、保存新版本）。</p>
 *
 * <p>依赖什么：无（纯实体，无特殊类型处理器）。</p>
 *
 * @TableName 发布版本
 */
@TableName(value = "t_flow_entry_publish", autoResultMap = true)
@Data
public class FlowEntryPublish implements Serializable {

	/**
	 * 主键：发布记录 ID，复用 Flowable 部署 ID（deployment.id），保证与引擎部署一一对应
	 */
	@TableId(value = "id")
	private String id;

	/**
	 * 流程Id：所属流程定义记录 ID（t_flow_entry.id）
	 */
	@TableField(value = "entry_id")
	private String entryId;

	/**
	 * 流程引擎的定义Id：Flowable 生成的流程定义 ID（processKey:version:deploymentId）
	 */
	@TableField(value = "process_definition_id")
	private String processDefinitionId;

	/**
	 * 发布版本：引擎流程定义的版本号（Flowable 自动递增）
	 */
	@TableField(value = "publish_version")
	private Integer publishVersion;

	/**
	 * 激活状态：当前版本是否处于激活可用状态
	 */
	@TableField(value = "active_status")
	private Boolean activeStatus;

	/**
	 * 是否为主版本：是否当前生效的最新版本（同一流程仅一条主版本）
	 */
	@TableField(value = "main_version")
	private Boolean mainVersion;

	/**
	 * 发布时间：本次发布操作的时间
	 */
	@TableField(value = "publish_time")
	private Date publishTime;

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
