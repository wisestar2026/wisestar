package cn.wisestar.server.flow.config;

import cn.wisestar.server.flow.listener.*;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工作流模块 Spring 配置类。
 *
 * <p>职责：</p>
 * <ol>
 *   <li>通过 {@code @MapperScan} 扫描 cn.wisestar.server.flow.mapper 包下的 MyBatis-Plus
 *       Mapper 接口，将其注册为 Spring Bean；</li>
 *   <li>自定义 Flowable 流程引擎配置：为流程引擎注册全局事件监听器，使流程实例在
 *       开始/完成/取消/挂起以及活动节点开始时，同步维护本模块的 t_flow_instance 表状态。</li>
 * </ol>
 *
 * <p>所属流程环节：配置引导环节。应用启动时由 Spring 容器自动加载，是整个流程模块
 * 与 Flowable 引擎、MyBatis 集成的基础设施。</p>
 *
 * <p>被谁调用：由 Spring Boot 自动装配加载，无直接调用方。</p>
 *
 * <p>依赖什么：{@link SpringProcessEngineConfiguration}（Flowable Spring 配置类）、
 * 流程监听器（{@link ProcessStartedListener}、{@link ProcessCompletedListener}、
 * {@link ActivityStartedListener}、{@link ProcessCancelledListener}、{@link ProcessSuspendedListener}）。</p>
 *
 * @author javahuang
 * @date 2021/11/19
 */
@Configuration
@MapperScan("cn.wisestar.server.flow.mapper")
@Slf4j
public class WorkflowConfig {

	/**
	 * 定制 Flowable 流程引擎配置：注册全局流程事件监听器。
	 *
	 * <p>仅在 classpath 中存在 {@link SpringProcessEngineConfiguration}（即项目引入了
	 * Flowable Spring Boot Starter）时才生效（{@code @ConditionalOnClass}），避免
	 * 未引入 Flowable 的环境加载失败。</p>
	 *
	 * <p>内部逻辑：构建事件类型 → 监听器列表的映射表并注入引擎配置：</p>
	 * <ul>
	 *   <li>PROCESS_STARTED（流程启动）→ {@link ProcessStartedListener}：插入 t_flow_instance 流程实例记录；</li>
	 *   <li>PROCESS_COMPLETED（流程正常结束）→ {@link ProcessCompletedListener}：实例状态置为"已结束"；</li>
	 *   <li>PROCESS_CANCELLED（流程被删除/拒绝）→ {@link ProcessCancelledListener}：实例状态置为"已拒绝"；</li>
	 *   <li>ACTIVITY_STARTED（活动节点开始）→ {@link ActivityStartedListener}：更新实例的当前审批阶段与"审批中"状态；</li>
	 *   <li>ENTITY_SUSPENDED / ENTITY_ACTIVATED（实例挂起/激活）→ {@link ProcessSuspendedListener}：状态置为"申请人完善中"。</li>
	 * </ul>
	 *
	 * @return 引擎配置定制器，Flowable Boot 会自动将该 Bean 应用到引擎配置上
	 */
	@Bean
	@ConditionalOnClass(SpringProcessEngineConfiguration.class)
	public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customizeSpringProcessEngineConfiguration() {
		return processEngineConfiguration -> {
			log.info("Overriding process engine configuration");

			// 事件类型 -> 监听器列表：同一事件可挂多个监听器，这里每个事件各挂一个
			Map<String, List<FlowableEventListener>> instanceListener = new HashMap<>();
			instanceListener.put(FlowableEngineEventType.PROCESS_COMPLETED.name(),
					Collections.singletonList(new ProcessCompletedListener()));
			instanceListener.put(FlowableEngineEventType.ACTIVITY_STARTED.name(),
					Collections.singletonList(new ActivityStartedListener()));
			instanceListener.put(FlowableEngineEventType.PROCESS_STARTED.name(),
					Collections.singletonList(new ProcessStartedListener()));
			instanceListener.put(FlowableEngineEventType.PROCESS_CANCELLED.name(),
					Collections.singletonList(new ProcessCancelledListener()));
			instanceListener.put(FlowableEngineEventType.ENTITY_SUSPENDED.name(),
					Collections.singletonList(new ProcessSuspendedListener()));
			instanceListener.put(FlowableEngineEventType.ENTITY_ACTIVATED.name(),
					Collections.singletonList(new ProcessSuspendedListener()));
			// 将全局监听器注入流程引擎配置，引擎在对应事件发生时回调监听器
			processEngineConfiguration.setTypedEventListeners(instanceListener);
		};
	}

}
