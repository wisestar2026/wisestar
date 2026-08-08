package cn.wisestar.server.flow.mapper;

import cn.wisestar.server.flow.domain.dto.UpdateFlowOperationUserRequest;
import cn.wisestar.server.flow.domain.model.FlowOperation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 流程操作记录 Mapper 接口（对应表 t_flow_operation）。
 *
 * <p>职责：为 {@link FlowOperation} 实体提供 MyBatis-Plus 通用 CRUD 能力
 * （继承 BaseMapper），并额外提供两个自定义更新方法（对应 XML 中的 SQL）：</p>
 * <ul>
 *   <li>{@link #updateOperationLatest(String)}：将某实例全部操作的 latest 置为 false，</li>
 *   <li>{@link #updateOperationUserLatest(UpdateFlowOperationUserRequest)}：将某用户在某
 *       实例中的操作人记录置为历史。</li>
 * </ul>
 * 由 {@link cn.wisestar.server.flow.config.WorkflowConfig} 中的 @MapperScan 扫描注册。
 *
 * <p>所属流程环节：审批处理环节（操作记录落库与 latest 标记维护）。</p>
 *
 * <p>被谁调用：AbstractTaskHandler.saveOperation（保存操作记录前调用两个自定义方法）。</p>
 *
 * <p>依赖什么：{@link FlowOperation} 实体、{@link UpdateFlowOperationUserRequest}（更新参数）。</p>
 *
 * @Entity cn.wisestar.server.flow.domain.FlowOperation
 */
public interface FlowOperationMapper extends BaseMapper<FlowOperation> {

	/**
	 * 将指定流程实例下所有操作记录的 latest 置为 false。
	 *
	 * <p>用途：在保存新操作记录前调用，保证每个实例仅最新一条操作的 latest=true，
	 * 从而限定"只有最新操作人才能撤回"。</p>
	 *
	 * @param instanceId 流程实例 ID
	 */
	void updateOperationLatest(@Param("instanceId") String instanceId);

	/**
	 * 将指定用户在某流程实例中的操作人记录全部置为 latest=false。
	 *
	 * <p>用途：在保存新操作人记录前调用，保证"同一用户在同一实例中只显示最近参与的
	 * 流程节点"（已办列表去重）。</p>
	 *
	 * @param request 更新参数（instanceId、userId、taskType）
	 */
	void updateOperationUserLatest(UpdateFlowOperationUserRequest request);

}
