package cn.wisestar.server.flow.domain.dto;

import cn.wisestar.server.domain.dto.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务列表查询参数 DTO。
 *
 * <p>职责：前端调用 <code>GET /workflow/getFlowTasks</code> 时的查询参数，继承通用
 * 分页查询 {@link PageQuery}（current / pageSize），额外携带任务查询类型、项目 ID、
 * 状态与创建人过滤条件。{@code FlowServiceImpl.getFlowTasks} 根据 type 分发到
 * 待办/已办/我发起的三个查询分支。</p>
 *
 * <p>所属流程环节：待办中心/任务列表查询环节。</p>
 *
 * <p>被谁调用：FlowApi.getFlowTasks（HTTP 参数绑定）、FlowServiceImpl.getFlowTasks
 * 及其内部查询分支方法。</p>
 *
 * <p>依赖什么：{@link PageQuery}（分页基类）。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class FlowTaskQuery extends PageQuery {

	/** 查询类型：1 我的待办 / 2 已办事项 / 3 我的抄送 / 4 我发起的，见 {@link cn.wisestar.server.flow.constant.FlowTaskQueryType} */
	private Integer type;

	/** 项目 ID：限定查询某个问卷/项目的任务（即流程定义 key） */
	private String projectId;

	/** 状态：流程实例状态过滤（当前查询分支中主要用于区分待办/已办展示，预留） */
	private Integer status;

	/** 创建人：按操作人/发起人过滤（预留过滤条件） */
	private String createBy;

}
