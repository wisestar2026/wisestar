package cn.wisestar.server.flow.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.flow.domain.dto.FlowEntryNodeRequest;
import cn.wisestar.server.flow.domain.dto.FlowEntryNodeView;
import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import org.mapstruct.Mapper;

/**
 * 流程节点对象映射器（MapStruct）。
 *
 * <p>职责：完成流程节点三类对象之间的自动转换：</p>
 * <ul>
 *   <li>请求 DTO {@link FlowEntryNodeRequest} → 实体 {@link FlowEntryNode}（保存流程时落库）；</li>
 *   <li>实体 {@link FlowEntryNode} → 视图 DTO {@link FlowEntryNodeView}（查询流程时回显）。</li>
 * </ul>
 *
 * <p>所属流程环节：流程设计环节（保存/读取节点配置）。</p>
 *
 * <p>被谁调用：FlowServiceImpl.saveFlow（fromRequest 保存节点）、
 * FlowServiceImpl.getFlowEntry（toView 回显节点）、FlowServiceImpl.deploy（保存节点）。</p>
 *
 * <p>依赖什么：{@link BaseModelMapper}（核心模块提供的通用转换接口），编译期由
 * MapStruct 生成转换实现，运行时作为 Spring Bean 注入。</p>
 *
 * @author javahuang
 * @date 2022/1/6
 */
@Mapper(componentModel = "spring")
public interface FlowEntryElementModelMapper
		extends BaseModelMapper<FlowEntryNodeRequest, FlowEntryNodeView, FlowEntryNode> {

}
