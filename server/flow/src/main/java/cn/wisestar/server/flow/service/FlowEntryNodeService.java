package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程节点服务接口。
 *
 * <p>职责：为 {@link FlowEntryNode}（已发布流程节点配置）提供 MyBatis-Plus
 * 通用服务能力（继承 IService，包含 CRUD、批量操作、Lambda 查询等），
 * 当前无额外业务方法，如需扩展节点相关逻辑可在此声明。</p>
 *
 * <p>所属流程环节：流程部署环节（节点落库）与审批处理环节（节点配置读取）。</p>
 *
 * <p>被谁调用：FlowServiceImpl（部署保存节点、权限过滤、节点名翻译）、
 * AbstractTaskHandler（答案合并权限）、TaskHelper（计算审批人）、
 * ActivityStartedListener（审批阶段名）、RevertTaskHandler（驳回节点名）等。</p>
 *
 * <p>依赖什么：{@link FlowEntryNode} 实体；实现见 {@code FlowEntryNodeServiceImpl}。</p>
 */
public interface FlowEntryNodeService extends IService<FlowEntryNode> {

}
