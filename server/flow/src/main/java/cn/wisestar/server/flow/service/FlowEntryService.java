package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowEntry;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程定义服务接口。
 *
 * <p>职责：为 {@link FlowEntry}（流程定义/草稿）提供 MyBatis-Plus 通用服务能力
 * （继承 IService），当前无额外业务方法，如需扩展流程定义相关逻辑可在此声明。</p>
 *
 * <p>所属流程环节：流程设计环节（保存草稿、部署、查询）。</p>
 *
 * <p>被谁调用：FlowServiceImpl（保存/部署/查询流程定义）、
 * AbstractTaskHandler（getFlowEntry 查询流程定义）。</p>
 *
 * <p>依赖什么：{@link FlowEntry} 实体；实现见 {@code FlowEntryServiceImpl}。</p>
 */
public interface FlowEntryService extends IService<FlowEntry> {

}
