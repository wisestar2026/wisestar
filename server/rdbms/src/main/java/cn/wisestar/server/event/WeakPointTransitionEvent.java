package cn.wisestar.server.event;

import cn.wisestar.server.domain.model.WeakPointEvent;

import java.util.List;

/**
 * 薄弱点状态跃迁事件（Spring 应用事件）。
 *
 * <p>承载一次评价刷新中产生的若干薄弱点跃迁（discovered / conquered / reopened），
 * 由 {@link WeakPointEventRecorder} 在主事务提交后批量落库，保证事件与源数据一致、
 * 且写入失败不阻断学习主流程。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
public class WeakPointTransitionEvent {

	private final List<WeakPointEvent> events;

	public WeakPointTransitionEvent(List<WeakPointEvent> events) {
		this.events = events;
	}

	public List<WeakPointEvent> getEvents() {
		return events;
	}

}
