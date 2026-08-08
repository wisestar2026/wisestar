package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowInstance;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程实例服务接口。
 *
 * <p>职责：为 {@link FlowInstance}（流程实例）提供 MyBatis-Plus 通用服务能力
 * （继承 IService），当前无额外业务方法，如需扩展实例管理逻辑（如按状态统计、
 * 实例生命周期查询等）可在此声明。</p>
 *
 * <p>所属流程环节：贯穿流程全生命周期——发起时创建、流转中由监听器更新状态、
 * "我发起的"列表与统计查询。</p>
 *
 * <p>被谁调用：ProcessStartedListener（创建实例）、ActivityStartedListener /
 * ProcessCompletedListener / ProcessCancelledListener / ProcessSuspendedListener
 * （状态同步）、FlowServiceImpl（列表/统计/审核记录）。</p>
 *
 * <p>依赖什么：{@link FlowInstance} 实体；实现见 {@code FlowInstanceServiceImpl}。</p>
 */
public interface FlowInstanceService extends IService<FlowInstance> {

}
