package cn.wisestar.server.event;

import cn.wisestar.server.domain.model.WeakPointEvent;
import cn.wisestar.server.mapper.WeakPointEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 薄弱点变化事件记录器（第 12.2 节：批量、幂等、提交后写入、容错）。
 *
 * <p><b>写入时机</b>：调用方在事务中 {@link #publish(List)}，监听器在
 * {@link TransactionPhase#AFTER_COMMIT} 触发，主事务回滚则不写；无事务时
 * （{@code fallbackExecution=true}）立即写入。</p>
 *
 * <p><b>容错</b>：监听器以 {@link Propagation#REQUIRES_NEW} 独立事务写入，
 * 逐条插入并捕获 {@link DuplicateKeyException}（幂等）与一般异常，仅记录日志、
 * 绝不向上抛出，保证不影响交卷/订正主流程。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeakPointEventRecorder {

	private final WeakPointEventMapper weakPointEventMapper;

	private final ApplicationEventPublisher applicationEventPublisher;

	/**
	 * 发布一批薄弱点跃迁事件（在事务内调用则提交后落库）。
	 */
	public void publish(List<WeakPointEvent> events) {
		if (events == null || events.isEmpty()) {
			return;
		}
		applicationEventPublisher.publishEvent(new WeakPointTransitionEvent(new ArrayList<>(events)));
	}

	/**
	 * 发布单条薄弱点跃迁事件。
	 */
	public void publish(WeakPointEvent event) {
		if (event != null) {
			publish(Collections.singletonList(event));
		}
	}

	/**
	 * 提交后批量落库（独立事务、逐条幂等、异常吞掉）。
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void onCommitted(WeakPointTransitionEvent transition) {
		if (transition == null || transition.getEvents() == null || transition.getEvents().isEmpty()) {
			return;
		}
		for (WeakPointEvent event : transition.getEvents()) {
			try {
				if (event.getOccurredAt() == null) {
					event.setOccurredAt(new Date());
				}
				weakPointEventMapper.insert(event);
			}
			catch (DuplicateKeyException dup) {
				// 幂等：同一跃迁重复触发，忽略
			}
			catch (Exception e) {
				log.warn("weak point event write failed: user={}, kp={}, type={}",
						event.getStudentId(), event.getKnowledgePointId(), event.getEventType(), e);
			}
		}
	}

}
