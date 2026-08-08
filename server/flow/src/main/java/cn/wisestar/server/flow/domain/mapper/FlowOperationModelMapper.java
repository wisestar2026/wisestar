package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowOperationView;
import cn.wisestar.server.flow.domain.model.FlowOperation;
import org.mapstruct.Mapper;

/**
 * 流程操作对象映射器（MapStruct）。
 *
 * <p>职责：完成操作记录实体 {@link FlowOperation} 与展示视图 {@link FlowOperationView}
 * 之间的自动转换，供审核记录（审批历史）列表组装使用。</p>
 *
 * <p>所属流程环节：审批详情/审核记录展示环节。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getAuditRecord（操作记录实体列表转视图列表）。</p>
 *
 * <p>依赖什么：{@link BaseModelMapper}（核心模块提供的通用转换接口），编译期由
 * MapStruct 生成实现。</p>
 *
 * @author javahuang
 * @date 2022/1/13
 */
@Mapper
public interface FlowOperationModelMapper extends BaseModelMapper<Void, FlowOperationView, FlowOperation> {

}
