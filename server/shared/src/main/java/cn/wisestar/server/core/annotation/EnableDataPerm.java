package cn.wisestar.server.core.annotation;

import cn.wisestar.server.core.constant.AppConsts;

import java.lang.annotation.*;

/**
 * 数据权限校验注解（EnableDataPerm）。
 *
 * <p><b>所属模块</b>：shared 模块核心注解包（cn.wisestar.server.core.annotation）。</p>
 * <p><b>类职责</b>：标注在 Controller 方法上，声明该方法需要做<em>数据级权限</em>
 * 校验。运行时由 {@link cn.wisestar.server.core.aop.DataPermAspect} 切面拦截，
 * 校验当前用户是否有权访问注解 key 指向的资源（如某个问卷/项目）。</p>
 *
 * <p><b>使用示例</b>：
 * <pre>
 * &#64;EnableDataPerm(key = "#id", permType = PermType.PROJECT)
 * public ProjectView getProject(@PathVariable String id) { ... }
 * </pre>
 * 切面会把方法参数（此处为 id）代入 SpEL 表达式求出目标资源标识，
 * 再与当前用户的可见项目集合比对。</p>
 *
 * <p><b>与 @PreAuthorize 的区别</b>：@PreAuthorize 校验"用户是否拥有某功能权限码"
 * （功能级）；本注解校验"用户是否有权操作某个具体数据对象"（数据级）。</p>
 *
 * @author javahuang
 * @date 2022/1/30
 */
@Target({ ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableDataPerm {

	/**
	 * SpEL 表达式，用于从方法参数中求出目标资源的标识值
	 * （如 "#id"、"#request.id"），切面据此做权限比对。
	 *
	 * @return SpEL 表达式字符串
	 */
	String key() default "";

	/**
	 * 权限类型，根据项目来区分权限
	 *
	 * @return 权限类型码，默认 PROJECT（项目维度）
	 */
	String permType() default AppConsts.PermType.PROJECT;

	/**
	 * 权限来源，默认通过 url 来获取
	 *
	 * @return 权限来源标识（预留扩展，当前切面未使用）
	 */
	String permFrom() default "";

}
