package cn.wisestar.server.core.config;

import cn.wisestar.server.core.base.converter.PublicQueryConverter;
import cn.wisestar.server.core.base.converter.RandomSurveyConverter;
import cn.wisestar.server.core.base.converter.UniqueLimitSettingConverter;
import cn.wisestar.server.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.event.EventListener;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 应用级配置（AppConfig）。
 *
 * <p><b>所属模块</b>：shared 模块核心框架配置包（cn.wisestar.server.core.config）。</p>
 * <p><b>类职责</b>：集中配置应用运行期的三类能力：</p>
 * <ul>
 *   <li><b>AOP 代理</b>（@EnableAspectJAutoProxy）：开启基于注解的 AOP
 *       （如 {@link cn.wisestar.server.core.aop.DataPermAspect} 数据权限切面）；</li>
 *   <li><b>异步支持</b>（@EnableAsync + AsyncConfigurer）：提供全局线程池
 *       （核心 4、最大 8、前缀 "MyExecutor-"），供 @Async 方法使用
 *       （如消息通知、异步统计等）；</li>
 *   <li><b>全局类型转换器注册</b>：应用启动完成（ApplicationReadyEvent）后，
 *       向 Spring 默认转换服务注册三个自定义 Converter：
 *       {@link UniqueLimitSettingConverter}、{@link PublicQueryConverter}、
 *       {@link RandomSurveyConverter}，使 URL 参数能自动绑定为对应的复杂类型。</li>
 * </ul>
 *
 * <p><b>为什么在 ApplicationReadyEvent 中注册转换器</b>：需要保证在 Spring 启动
 * 完成后、请求进入前完成注册；使用共享 DefaultConversionService 实例，
 * 使所有使用该转换服务的场景（如 @RequestParam 绑定）都能生效。</p>
 *
 * @author javahuang
 * @date 2021/10/19
 */
@Configuration
@EnableAspectJAutoProxy
@EnableAsync
public class AppConfig implements AsyncConfigurer {

	/**
	 * 用户服务（setter 注入，预留的初始化入口依赖；当前 init 逻辑未启用）。
	 */
	private UserService userService;

	@Autowired
	public void setUserService(UserService userService) {
		this.userService = userService;
	}

	/**
	 * 应用启动完成后的初始化操作。
	 *
	 * <p>向共享 DefaultConversionService 注册三个自定义类型转换器，用于
	 * HTTP 参数到复杂对象的自动转换：</p>
	 * <ul>
	 *   <li>{@link UniqueLimitSettingConverter}：字符串 → 唯一性/次数限制设置对象；</li>
	 *   <li>{@link PublicQueryConverter}：查询参数 → 公共查询对象；</li>
	 *   <li>{@link RandomSurveyConverter}：字符串 → 随机问卷条件对象。</li>
	 * </ul>
	 * <p>注：userService.init() 初始化逻辑已被注释停用。</p>
	 */
	@EventListener(ApplicationReadyEvent.class)
	public void initAfterStartup() {
		// userService.init();
		DefaultConversionService defaultConversionService = (DefaultConversionService) DefaultConversionService
				.getSharedInstance();
		defaultConversionService.addConverter(new UniqueLimitSettingConverter());
		defaultConversionService.addConverter(new PublicQueryConverter());
		defaultConversionService.addConverter(new RandomSurveyConverter());
	}

	/**
	 * 全局异步任务线程池。
	 *
	 * <p>核心线程数 4、最大线程数 8、线程名前缀 "MyExecutor-"，
	 * 供所有 @Async 注解方法提交任务使用（队列默认无界）。</p>
	 *
	 * @return 异步执行器
	 */
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(8);
		executor.setThreadNamePrefix("MyExecutor-");
		executor.initialize();
		return executor;
	}

}
