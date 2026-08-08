package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowEntryView;
import cn.wisestar.server.flow.domain.model.FlowEntry;
import org.mapstruct.Mapper;

/**
 * 流程定义对象映射器（MapStruct）。
 *
 * <p>职责：完成流程定义实体 {@link FlowEntry} 与展示视图 {@link FlowEntryView}
 * 之间的自动转换（无请求 DTO，保存走实体直接落库）。</p>
 *
 * <p>所属流程环节：流程设计环节（读取流程定义回显）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.getFlowEntry（实体转视图返回前端）。</p>
 *
 * <p>依赖什么：{@link BaseModelMapper}（核心模块提供的通用转换接口），编译期由
 * MapStruct 生成实现。</p>
 *
 * @author javahuang
 * @date 2022/1/5
 */
@Mapper
public interface FlowEntryModelMapper extends BaseModelMapper<Void, FlowEntryView, FlowEntry> {

}
