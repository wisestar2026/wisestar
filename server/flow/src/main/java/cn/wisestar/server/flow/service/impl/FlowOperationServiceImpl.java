package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowOperation;
import cn.wisestar.server.flow.mapper.FlowOperationMapper;
import cn.wisestar.server.flow.service.FlowOperationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程操作记录服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowOperation}（操作记录）
 * 提供通用 CRUD 实现，绑定 {@link FlowOperationMapper}；当前无额外业务逻辑，是
 * {@link FlowOperationService} 的默认实现，由 Spring 注入给 AbstractTaskHandler
 * 与 FlowServiceImpl 等调用方（自定义 Mapper 方法通过 getBaseMapper() 获取调用）。</p>
 *
 * <p>所属流程环节：审批处理环节（操作记录落库）与审批详情/已办列表展示环节。</p>
 *
 * <p>被谁调用：AbstractTaskHandler、FlowServiceImpl。</p>
 *
 * <p>依赖什么：{@link FlowOperationMapper}（数据访问层，含 latest 标记更新 SQL）。</p>
 */
@Service
public class FlowOperationServiceImpl extends ServiceImpl<FlowOperationMapper, FlowOperation>
		implements FlowOperationService {

}
