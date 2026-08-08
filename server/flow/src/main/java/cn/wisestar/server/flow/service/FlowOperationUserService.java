package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowOperationUser;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程操作人服务接口。
 *
 * <p>职责：为 {@link FlowOperationUser}（操作人关联记录）提供 MyBatis-Plus
 * 通用服务能力（继承 IService），当前无额外业务方法，如需扩展操作人查询逻辑
 * 可在此声明。</p>
 *
 * <p>所属流程环节：审批处理环节（保存操作人）与已办列表查询环节（归属判断）。</p>
 *
 * <p>被谁调用：AbstractTaskHandler.saveOperationUser（保存操作人记录）。</p>
 *
 * <p>依赖什么：{@link FlowOperationUser} 实体；实现见 {@code FlowOperationUserServiceImpl}。</p>
 */
public interface FlowOperationUserService extends IService<FlowOperationUser> {

}
