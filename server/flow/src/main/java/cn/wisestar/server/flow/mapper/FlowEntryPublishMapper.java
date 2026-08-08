package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.model.FlowEntryPublish;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 流程发布版本 Mapper 接口（对应表 t_flow_entry_publish）。
 *
 * <p>职责：为 {@link FlowEntryPublish} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），无需手写 SQL；如需扩展复杂查询可在此补充方法。
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的
 * @MapperScan 扫描注册。</p>
 *
 * <p>所属流程环节：流程部署/发布环节（新旧版本切换、发布记录写入）。</p>
 *
 * <p>被谁调用：FlowEntryPublishServiceImpl（Service 层 CRUD）、
 * FlowServiceImpl.deploy（更新旧版本、保存新版本）。</p>
 *
 * <p>依赖什么：{@link FlowEntryPublish} 实体（无特殊类型处理器）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowEntryPublish
 */
public interface FlowEntryPublishMapper extends BaseMapper<FlowEntryPublish> {

}
