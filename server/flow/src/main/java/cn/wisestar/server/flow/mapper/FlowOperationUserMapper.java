package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.model.FlowOperationUser;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程操作人 Mapper 接口（对应表 t_flow_operation_user）。
 *
 * <p>职责：为 {@link FlowOperationUser} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的
 * @MapperScan 扫描注册。</p>
 *
 * <p>所属流程环节：审批处理环节（保存操作人、已办列表归属判断）。</p>
 *
 * <p>被谁调用：FlowOperationUserServiceImpl（Service 层 CRUD）、
 * AbstractTaskHandler.saveOperationUser（保存操作人）。</p>
 *
 * <p>依赖什么：{@link FlowOperationUser} 实体（无特殊类型处理器）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowOperationIdentitylink
 */
public interface FlowOperationUserMapper extends BaseMapper<FlowOperationUser> {

}
