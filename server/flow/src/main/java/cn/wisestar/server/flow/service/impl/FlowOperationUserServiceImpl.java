package cn.wisestar.server.flow.service.impl;

import cn.wisestar.server.flow.domain.model.FlowOperationUser;
import cn.wisestar.server.flow.mapper.FlowOperationUserMapper;
import cn.wisestar.server.flow.service.FlowOperationUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 流程操作人服务实现。
 *
 * <p>职责：基于 MyBatis-Plus {@link ServiceImpl} 为 {@link FlowOperationUser}
 * （操作人关联记录）提供通用 CRUD 实现，绑定 {@link FlowOperationUserMapper}；
 * 当前无额外业务逻辑，是 {@link FlowOperationUserService} 的默认实现，由 Spring
 * 注入给 AbstractTaskHandler 使用。</p>
 *
 * <p>所属流程环节：审批处理环节（保存操作人记录）。</p>
 *
 * <p>被谁调用：AbstractTaskHandler.saveOperationUser。</p>
 *
 * <p>依赖什么：{@link FlowOperationUserMapper}（数据访问层）。</p>
 */
@Service
public class FlowOperationUserServiceImpl extends ServiceImpl<FlowOperationUserMapper, FlowOperationUser>
		implements FlowOperationUserService {

}
