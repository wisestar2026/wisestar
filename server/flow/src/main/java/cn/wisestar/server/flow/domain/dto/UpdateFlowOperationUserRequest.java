package cn.wisestar.server.flow.domain.dto;

import lombok.Data;

/**
 * 更新操作人（已办记录）最新状态的请求 DTO。
 *
 * <p>职责：在保存操作记录时，将指定流程实例中某个用户的历史操作记录的 latest 标记
 * 全部置为 false，用于保证"同一用户在同一个流程实例中只保留最新一条已办记录"
 * （如果一个人参与了多个流程节点，只显示最近参与的流程节点）。由
 * {@code FlowOperationMapper.updateOperationUserLatest} 消费。</p>
 *
 * <p>所属流程环节：审批处理环节（操作记录落库时的已办去重）。</p>
 *
 * <p>被谁调用：AbstractTaskHandler.saveOperation（组装参数并调用 Mapper）。</p>
 *
 * <p>依赖什么：无（纯查询/更新参数）。</p>
 *
 * @author javahuang
 * @date 2022/1/11
 */
@Data
public class UpdateFlowOperationUserRequest {

	/** 流程实例 ID：限定更新哪个实例下的操作人记录 */
	private String instanceId;

	/** 用户 ID：要置为历史（latest=false）的操作人 */
	private String userId;

	/** 任务类型：限定更新哪种任务类型的操作记录（当前为用户任务 userTask） */
	private Integer taskType;

}
