package cn.wisestar.server;

import cn.wisestar.server.core.uitls.DatabaseInitHelper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * AI 自习室系统（wisestar）后端服务启动类（SurveyServerApplication）。
 *
 * <p><b>所属模块</b>：api 模块（Web 应用模块，是整个后端唯一可运行的 Spring Boot 启动入口，
 * 它同时依赖 rdbms、shared、ai 等模块，Maven 依赖会将这些模块的类都纳入 classpath）。</p>
 *
 * <p><b>类职责</b>：</p>
 * <ul>
 *   <li>作为 Spring Boot 应用的启动类，main 方法入口；</li>
 *   <li>声明组件扫描范围：@SpringBootApplication 默认扫描 cn.wisestar.server 包及其子包，
 *       因此 api（cn.wisestar.server.api）、shared、rdbms、ai（cn.wisestar.server.ai）各模块
 *       中的 @RestController / @Service / @Component 等都会被装配进 Spring 容器；</li>
 *   <li>通过 @EnableConfigurationProperties 开启配置属性类的绑定
 *       （配合核心包中的 @ConfigurationProperties 类，如 api.prefix 等自定义配置）。</li>
 * </ul>
 *
 * <p><b>被谁调用</b>：由运维/部署脚本或 IDE 以
 * {@code java -jar survey-server.jar} / {@code mvn spring-boot:run} 方式启动；
 * 支持在启动命令中传入额外参数以触发数据库初始化（见 main 方法说明）。</p>
 *
 * <p><b>依赖的服务</b>：启动阶段依赖 core 工具类
 * {@link DatabaseInitHelper}（cn.wisestar.server.core.uitls 包，用于快速建库/初始化数据）。
 * 运行期依赖 Spring Boot 全家桶（web/undertow、security、mybatis 等，由各模块 pom 引入）。</p>
 */
@SpringBootApplication
@EnableConfigurationProperties
public class SurveyServerApplication {

	/**
	 * 应用启动入口。
	 *
	 * <p><b>功能</b>：启动 Spring Boot 应用。特殊逻辑——如果 JVM 启动参数 args 非空，
	 * 则先调用 {@link DatabaseInitHelper#init(String[])} 执行一次快速的数据库初始化操作
	 * （例如初始化/升级数据库表结构、写入种子数据等，具体动作由该工具类实现决定），
	 * 然后再通过 {@link SpringApplication#run(Class, String...)} 正式启动 Spring 容器。</p>
	 *
	 * <p><b>请求/数据流</b>：本方法不处理 HTTP 请求，仅负责进程启动：
	 * JVM 启动参数 → 判断是否执行数据库初始化 → SpringApplication.run 装载所有 Bean
	 * → 内嵌 Undertow 容器监听端口 → 接受来自 api 模块各 Controller 的 HTTP 请求。</p>
	 *
	 * <p><b>异常</b>：若数据库不可用、配置错误或端口被占用，启动过程会抛出异常并终止进程。</p>
	 *
	 * @param args 命令行启动参数；传入任意参数（如参数中携带配置项）会触发数据库初始化逻辑
	 */
	public static void main(String[] args) {
		// 快速执行数据库初始化操作
		if (args.length > 0) {
			DatabaseInitHelper.init(args);
		}

		SpringApplication.run(SurveyServerApplication.class, args);
	}

}
