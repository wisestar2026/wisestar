package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.model.FlowInstance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程实例 Mapper 接口（对应表 t_flow_instance）。
 *
 * <p>职责：为 {@link FlowInstance} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的
 * @MapperScan 扫描注册。</p>
 *
 * <p>所属流程环节：贯穿流程全生命周期——发起时创建、流转中由监听器更新状态、
 * "我发起的"列表与统计查询。</p>
 *
 * <p>被谁调用：FlowInstanceServiceImpl（Service 层 CRUD）、各流程监听器
 * （状态同步）、FlowServiceImpl（我发起的列表/统计）。</p>
 *
 * <p>依赖什么：{@link FlowInstance} 实体（无特殊类型处理器）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowInstance
 */
public interface FlowInstanceMapper extends BaseMapper<FlowInstance> {

}
