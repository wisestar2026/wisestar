package cn.wisestar.server.core.aop;

import cn.wisestar.server.core.annotation.EnableDataPerm;
import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.service.ProjectPartnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * 数据权限校验切面（DataPermAspect）。
 *
 * <p><b>所属模块</b>：shared 模块 AOP 切面包（cn.wisestar.server.core.aop）。</p>
 * <p><b>类职责</b>：对标注了 {@link EnableDataPerm} 注解的 Controller 方法
 * 做<em>数据级权限</em>的前置校验（区别于 @PreAuthorize 的功能级权限）。
 * 用于"项目/问卷"维度的数据隔离：当前用户必须拥有访问指定问卷的权限，
 * 否则抛出异常阻止请求继续执行。</p>
 *
 * <p><b>工作机制</b>：</p>
 * <ol>
 *   <li>通过切点表达式 {@code @annotation(dataPerm)} 匹配所有标注
 *       @EnableDataPerm 的方法，并在方法执行前（@Before）切入；</li>
 *   <li>从方法参数构建 SpEL 求值上下文（参数名 → 参数值），用注解中的
 *       {@code key} 表达式（如 {@code #id}）求出目标资源的标识值
 *       （即"要校验的问卷 id"）；</li>
 *   <li>调用 {@link ProjectPartnerService#getProjectPerms()} 获取当前用户
 *       拥有访问权限的项目/问卷 id 集合；</li>
 *   <li>若标识值不在该集合中，抛出 {@link InternalServerError}
 *       （"没有权限访问本问卷"）拒绝访问。</li>
 * </ol>
 *
 * <p><b>数据流</b>：前端请求（携带目标问卷 id）→ Controller 方法（@EnableDataPerm(key="#id")）
 * → 本切面解析 id → ProjectPartnerService 查询当前用户可见项目集合
 * → 校验通过则执行业务方法，否则抛异常 → GlobalExceptionHandler 返回错误。</p>
 *
 * @author javahuang
 * @date 2022/1/30
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class DataPermAspect {

	/**
	 * 项目/问卷权限服务：提供当前用户可访问的项目权限码集合（构造器注入）。
	 */
	private final ProjectPartnerService projectPartnerService;

	/**
	 * SpEL 表达式解析器：用于求值注解 key 中的表达式（如 #id、#request.id）。
	 */
	private SpelExpressionParser spelParser = new SpelExpressionParser();

	/**
	 * 切点定义：匹配所有标注了 @EnableDataPerm 的方法。
	 *
	 * @param dataPerm 方法上的 @EnableDataPerm 注解（绑定到通知参数）
	 */
	@Pointcut("@annotation(dataPerm)")
	public void checkPermissionPointCut(EnableDataPerm dataPerm) {
	}

	/**
	 * 前置通知：方法执行前校验数据权限。
	 *
	 * <p><b>执行步骤</b>：</p>
	 * <ol>
	 *   <li>解析方法参数名与参数值，构建 SpEL 求值上下文（参数名作为变量）；</li>
	 *   <li>求值注解 key 表达式得到目标资源标识（如问卷 id），空值直接拒绝
	 *       （"未找到对应的问卷"）；</li>
	 *   <li>获取当前用户可见的项目权限集合，标识不在集合内则拒绝
	 *       （"没有权限访问本问卷"）。</li>
	 * </ol>
	 *
	 * @param joinPoint 连接点（含目标方法、参数等）
	 * @param dataPerm  方法上的 @EnableDataPerm 注解
	 * @throws Throwable 校验失败或求值异常
	 */
	@Before("checkPermissionPointCut(dataPerm)")
	public void around(JoinPoint joinPoint, EnableDataPerm dataPerm) throws Throwable {
		// 获取方法参数名和值
		MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
		List<String> paramNameList = Arrays.asList(methodSignature.getParameterNames());
		List<Object> paramList = Arrays.asList(joinPoint.getArgs());

		EvaluationContext ctx = new StandardEvaluationContext();
		for (int i = 0; i < paramNameList.size(); i++) {
			ctx.setVariable(paramNameList.get(i), paramList.get(i));
		}
		String value = spelParser.parseExpression(dataPerm.key()).getValue(ctx).toString();
		if (!StringUtils.hasText(value)) {
			throw new InternalServerError("未找到对应的问卷");
		}
		List<String> projectPerms = projectPartnerService.getProjectPerms();
		if (!projectPerms.contains(value)) {
			throw new InternalServerError("没有权限访问本问卷");
		}
	}

}
