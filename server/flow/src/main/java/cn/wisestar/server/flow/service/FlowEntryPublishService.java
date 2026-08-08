package cn.wisestar.server.flow.service;

import cn.wisestar.server.flow.domain.model.FlowEntryPublish;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 流程发布版本服务接口。
 *
 * <p>职责：为 {@link FlowEntryPublish}（流程发布版本记录）提供 MyBatis-Plus
 * 通用服务能力（继承 IService），当前无额外业务方法，如需扩展版本管理逻辑
 * （如查询主版本、历史版本清理等）可在此声明。</p>
 *
 * <p>所属流程环节：流程部署/发布环节（新旧版本切换）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.deploy（更新旧版本、保存新版本）。</p>
 *
 * <p>依赖什么：{@link FlowEntryPublish} 实体；实现见 {@code FlowEntryPublishServiceImpl}。</p>
 */
public interface FlowEntryPublishService extends IService<FlowEntryPublish> {

}
