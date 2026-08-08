package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowEntry;
import cn.wisestar.server.flow.mapper.FlowEntryMapper;
import cn.wisestar.server.flow.service.FlowEntryService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程定义服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowEntry}（流程定义）
 * 提供通用 CRUD 实现，绑定 {@link FlowEntryMapper}；当前无额外业务逻辑，是
 * {@link FlowEntryService} 的默认实现，由 Spring 注入给 FlowServiceImpl 等调用方。</p>
 *
 * <p>所属流程环节：流程设计环节（保存草稿、部署、查询）。</p>
 *
 * <p>被谁调用：FlowServiceImpl、AbstractTaskHandler（通过 FlowEntryService 接口注入）。</p>
 *
 * <p>依赖什么：{@link FlowEntryMapper}（数据访问层）。</p>
 */
@Service
public class FlowEntryServiceImpl extends ServiceImpl<FlowEntryMapper, FlowEntry> implements FlowEntryService {

}
