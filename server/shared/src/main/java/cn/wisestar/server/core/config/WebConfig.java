package cn.wisestar.server.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC 配置（WebConfig）。
 *
 * <p><b>所属模块</b>：shared 模块核心框架配置包（cn.wisestar.server.core.config）。</p>
 * <p><b>类职责</b>：实现 {@link WebMvcConfigurer}，对 Spring MVC 做全局增强，
 * 同时本身也是一个 @RestController，负责单 jar 部署时根路径首页的返回：</p>
 * <ul>
 *   <li><b>消息转换器</b>：{@link #configureMessageConverters} 在转换器列表首位
 *       加入基于共享 ObjectMapper 的 {@link MappingJackson2HttpMessageConverter}，
 *       使 Controller 可以直接返回 String 类型（避免默认 StringHttpMessageConverter
 *       与 JSON 转换器顺序问题）；</li>
 *   <li><b>静态资源映射</b>：{@link #addResourceHandlers} 把 css/js/图片/字体等
 *       静态资源映射到 classpath:/static/ 目录，并设置优先级高于根路径 @GetMapping、
 *       缓存周期一天（浏览器缓存 86400 秒）；</li>
 *   <li><b>根路径首页</b>：{@link #index()} 返回 classpath:/static/index.html，
 *       保证访问根路径时加载前端 SPA 页面。</li>
 * </ul>
 *
 * <p><b>为什么静态资源优先级设为 -1</b>：registry.setOrder(-1) 使 ResourceHandler
 * 的优先级高于 @GetMapping 根路径映射，这样以 .js/.css 等结尾的请求先被静态资源
 * 处理器接管，不会落入 index() 兜底逻辑。</p>
 *
 * @author javahuang
 * @date 2021/8/11
 */
@Configuration
@RequiredArgsConstructor
@RestController
public class WebConfig implements WebMvcConfigurer {

	/**
	 * 共享的 Jackson ObjectMapper（构造器注入，保证与全局序列化配置一致）。
	 */
	private final ObjectMapper objectMapper;

	/**
	 * 配置 HTTP 消息转换器列表。
	 *
	 * <p>在转换器列表首位插入基于共享 ObjectMapper 的 JSON 转换器，
	 * 解决"Controller 直接返回 String 时被默认转换器处理导致乱码/类型错误"
	 * 的问题，并保证所有 JSON 序列化使用统一的 ObjectMapper 配置。</p>
	 *
	 * @param converters Spring MVC 转换器列表（可在头部插入自定义转换器）
	 */
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// 允许 controller 直接返回 string
		converters.add(0, new MappingJackson2HttpMessageConverter(objectMapper));
	}

	@Value("classpath:/static/index.html")
	private Resource indexHtml;

	// 匹配类型的静态资源都会被 ResourceHandler 来处理
	public static final String[] STATIC_RESOURCES = { "/**/*.css", "/**/*.js", "/**/*.jpg", "/**/*.png", "/**/*.svg", // 图片
			"/**/*.eot", "/**/*.ttf", "/**/*.woff", "/**/favicon.ico" };

	/**
	 * 注册静态资源处理器。
	 *
	 * <p>把 {@link #STATIC_RESOURCES} 匹配的静态资源映射到 classpath:/static/
	 * 目录；优先级设为 -1（高于根路径 @GetMapping），缓存周期 86400 秒（1 天）。</p>
	 *
	 * @param registry 静态资源注册器
	 */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.setOrder(-1) // 设置静态资源映射优先级高于下面配置的 @GetMapping
				.addResourceHandler(STATIC_RESOURCES).addResourceLocations("classpath:/static/")
				.setCachePeriod(3600 * 24);
	}

	/**
	 * 根路径首页兜底：返回前端 SPA 的 index.html。
	 *
	 * <p>单 jar 部署时访问 {@code /} 返回静态首页；未匹配任何静态资源与
	 * Controller 的路径由框架按此规则兜底。</p>
	 *
	 * @return index.html 资源（HTTP 200）
	 */
	@GetMapping
	public Object index() {
		return ResponseEntity.ok().body(indexHtml);
	}

}
