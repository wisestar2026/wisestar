package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowOperation;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程操作记录服务接口。
 *
 * <p>职责：为 {@link FlowOperation}（操作记录）提供 MyBatis-Plus 通用服务能力
 * （继承 IService），当前无额外业务方法，如需扩展操作记录查询逻辑
 * （如按实例查历史、按用户查已办等）可在此声明。</p>
 *
 * <p>所属流程环节：审批处理环节（操作记录落库）与审批详情/已办列表展示环节。</p>
 *
 * <p>被谁调用：AbstractTaskHandler（保存操作记录、查询历史、构建节点树）、
 * FlowServiceImpl（已办列表、审核记录、已办统计）。</p>
 *
 * <p>依赖什么：{@link FlowOperation} 实体；实现见 {@code FlowOperationServiceImpl}。</p>
 */
public interface FlowOperationService extends IService<FlowOperation> {

}
