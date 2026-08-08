package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.model.FlowEntry;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程定义 Mapper 接口（对应表 t_flow_entry）。
 *
 * <p>职责：为 {@link FlowEntry} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的
 * @MapperScan 扫描注册。</p>
 *
 * <p>所属流程环节：流程设计/部署环节的数据访问层。</p>
 *
 * <p>被谁调用：FlowEntryServiceImpl（Service 层 CRUD）、
 * FlowServiceImpl（按 projectId 查询流程定义）。</p>
 *
 * <p>依赖什么：{@link FlowEntry} 实体（含 nodes 列的 JSON 类型处理器）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowEntry
 */
public interface FlowEntryMapper extends BaseMapper<FlowEntry> {

}
