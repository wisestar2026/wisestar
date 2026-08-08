package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程节点 Mapper 接口（对应表 t_flow_entry_node）。
 *
 * <p>职责：为 {@link FlowEntryNode} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的
 * @MapperScan 扫描注册。</p>
 *
 * <p>所属流程环节：流程部署环节（节点配置落库）与审批处理环节（节点配置读取）。</p>
 *
 * <p>被谁调用：FlowEntryNodeServiceImpl（Service 层 CRUD）、TaskHelper /
 * ActivityStartedListener / AbstractTaskHandler 等通过 Service 间接调用。</p>
 *
 * <p>依赖什么：{@link FlowEntryNode} 实体（field_permission / setting / identity
 * 列的 Jackson JSON 类型处理器）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowEntryNode
 */
public interface FlowEntryNodeMapper extends BaseMapper<FlowEntryNode> {

}
