package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowInstance;
import cn.wisestar.server.flow.mapper.FlowInstanceMapper;
import cn.wisestar.server.flow.service.FlowInstanceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程实例服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowInstance}（流程实例）
 * 提供通用 CRUD 实现，绑定 {@link FlowInstanceMapper}；当前无额外业务逻辑，是
 * {@link FlowInstanceService} 的默认实现，由 Spring 注入给各流程监听器与
 * FlowServiceImpl 等调用方。</p>
 *
 * <p>所属流程环节：贯穿流程全生命周期——发起时创建、流转中状态同步、列表与统计查询。</p>
 *
 * <p>被谁调用：ProcessStartedListener、ActivityStartedListener、ProcessCompletedListener、
 * ProcessCancelledListener、ProcessSuspendedListener、FlowServiceImpl。</p>
 *
 * <p>依赖什么：{@link FlowInstanceMapper}（数据访问层）。</p>
 */
@Service
public class FlowInstanceServiceImpl extends ServiceImpl<FlowInstanceMapper, FlowInstance>
		implements FlowInstanceService {

}
