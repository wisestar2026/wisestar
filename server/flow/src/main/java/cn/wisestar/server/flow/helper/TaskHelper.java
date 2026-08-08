package cn.wisestar.server.flow.helper;

import cn.wisestar.server.flow.domain.model.FlowEntryNode;
import cn.wisestar.server.flow.service.FlowEntryNodeService;
import cn.wisestar.server.service.UserService;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 任务辅助组件（BPMN 表达式 Bean，名称为 "t"）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>{@link #getUsers(String, String)}：作为 BPMN XML 用户任务节点"审批人集合"
 *       表达式（如 <code>${t.getUsers(execution.activityId, execution.starterUserId)}</code>）
 *       的求值入口，根据节点配置的 identity（U:用户 / R:角色 / P:岗位）解析出最终审批人 ID 集合；</li>
 *   <li>{@link #condition(DelegateExecution)}：条件判断占位实现（预留，当前为空）。</li>
 * </ul>
 *
 * <p>所属流程环节：审批流转环节。引擎每次进入用户任务节点创建待办任务时调用
 * getUsers 计算该节点的审批人（候选者），是"流程如何流转"中确定下一审批人的关键一步。</p>
 *
 * <p>被谁调用：Flowable 引擎（BPMN 表达式求值）。</p>
 *
 * <p>依赖什么：{@link FlowEntryNodeService}（按节点 ID 读取节点配置）、
 * {@link UserService}（按角色/岗位组展开用户，getUsersByGroup）。</p>
 *
 * @author javahuang
 * @date 2022/1/19
 */
@Component("t")
@RequiredArgsConstructor
public class TaskHelper {

	/** 流程节点服务：按节点 ID 读取节点配置（审批人 identity 列表） */
	private final FlowEntryNodeService nodeService;

	/** 用户服务：将角色/岗位组展开为具体用户 ID */
	private final UserService userService;

	/**
	 * 根据 xml 节点计算审批人。
	 *
	 * <p>内部逻辑：按 activityId 加载节点配置，遍历 identity 列表：</p>
	 * <ul>
	 *   <li>格式为 "U:用户ID" → 直接取冒号后的用户 ID；</li>
	 *   <li>格式为 "R:角色ID" / "P:岗位ID" → 调用 userService.getUsersByGroup
	 *       将该组下所有成员（并结合发起人过滤，如部门主管取申请人所在部门主管）展开为具体用户。</li>
	 * </ul>
	 * 结果使用 LinkedHashSet 保证有序且去重，作为引擎创建任务时的候选者集合。
	 *
	 * @param activityId 当前 XML 节点 ID（BPMN activityId）
	 * @param starterUserId 发起人用户 ID（展开部门/角色组成员时可能需要）
	 * @return 该节点的最终审批人用户 ID 集合
	 */
	public Set<String> getUsers(String activityId, String starterUserId) {
		// 加载节点配置，获取配置的授权用户（identity）列表
		FlowEntryNode node = nodeService.getById(activityId);
		Set<String> result = new LinkedHashSet<>();
		for (String identity : node.getIdentity()) {
			// 普通用户
			if (identity.startsWith("U:")) {
				result.add(identity.split(":")[1]);
			}
			else if (identity.startsWith("R:") || identity.startsWith("P:")) {
				// 角色和岗位：按组展开成员用户
				result.addAll(userService.getUsersByGroup(identity, starterUserId));
			}
		}
		return result;
	}

	/**
	 * 条件判断占位方法。
	 *
	 * <p>预留扩展点：设计用于 BPMN 排他网关等条件表达式的求值（如根据表单值决定
	 * 流程走向），当前实现为空，未参与流程流转判断。</p>
	 *
	 * @param execution 引擎执行上下文（可读取流程变量与节点数据）
	 */
	public void condition(DelegateExecution execution) {

	}

}
