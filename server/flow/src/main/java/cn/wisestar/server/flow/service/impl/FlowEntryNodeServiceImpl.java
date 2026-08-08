package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import cn.wisestar.server.flow.mapper.FlowEntryNodeMapper;
import cn.wisestar.server.flow.service.FlowEntryNodeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程节点服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowEntryNode}（已发布
 * 流程节点配置）提供通用 CRUD 实现，绑定 {@link FlowEntryNodeMapper}；当前无额外
 * 业务逻辑，是 {@link FlowEntryNodeService} 的默认实现，由 Spring 注入给
 * FlowServiceImpl、TaskHelper、各监听器与 TaskHandler 等调用方。</p>
 *
 * <p>所属流程环节：流程部署环节（节点落库）与审批处理环节（节点配置读取）。</p>
 *
 * <p>被谁调用：FlowServiceImpl、AbstractTaskHandler、TaskHelper、ActivityStartedListener 等。</p>
 *
 * <p>依赖什么：{@link FlowEntryNodeMapper}（数据访问层）。</p>
 */
@Service
public class FlowEntryNodeServiceImpl extends ServiceImpl<FlowEntryNodeMapper, FlowEntryNode>
		implements FlowEntryNodeService {

}
