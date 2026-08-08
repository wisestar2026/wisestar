package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowEntryPublish;
import cn.wisestar.server.flow.mapper.FlowEntryPublishMapper;
import cn.wisestar.server.flow.service.FlowEntryPublishService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程发布版本服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowEntryPublish}
 * （流程发布版本记录）提供通用 CRUD 实现，绑定 {@link FlowEntryPublishMapper}；
 * 当前无额外业务逻辑，是 {@link FlowEntryPublishService} 的默认实现，由 Spring
 * 注入给 FlowServiceImpl 使用。</p>
 *
 * <p>所属流程环节：流程部署/发布环节（新旧版本切换）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.deploy（更新旧版本、保存新版本）。</p>
 *
 * <p>依赖什么：{@link FlowEntryPublishMapper}（数据访问层）。</p>
 */
@Service
public class FlowEntryPublishServiceImpl extends ServiceImpl<FlowEntryPublishMapper, FlowEntryPublish>
		implements FlowEntryPublishService {

}
