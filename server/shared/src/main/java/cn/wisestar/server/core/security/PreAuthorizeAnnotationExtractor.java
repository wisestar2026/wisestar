package cn.wisestar.server.core.security;

import cn.wisestar.server.core.uitls.ContextHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @PreAuthorize 注解权限码提取器（PreAuthorizeAnnotationExtractor）。
 *
 * <p><b>所属模块</b>：shared 模块核心安全框架（cn.wisestar.server.core.security）。</p>
 * <p><b>类职责</b>：静态工具类，在应用启动或初始化权限数据时，扫描容器中所有
 * 标注了 {@link RestController} 的 Controller（含其父类方法），收集其上所有
 * {@link PreAuthorize} 注解中的权限码（如 {@code hasAuthority('answer:list')}），
 * 并拆解为 <em>层级化权限码集合</em>（如 {answer, answer:list}）返回。</p>
 *
 * <p><b>用途</b>：被权限初始化逻辑（如 rdbms 模块中角色/权限初始化器）调用，
 * 用于自动登记系统中存在的全部权限码，避免手工维护权限字典与代码不同步。</p>
 *
 * <p><b>解析规则</b>：权限码按 {@code :} 冒号分级，如 {@code answer:list:export}
 * 会依次加入 {@code answer}、{@code answer:list}、{@code answer:list:export}，
 * 形成父子层级的权限集合，便于前端按层级展示权限树。</p>
 *
 * <p><b>缓存</b>：结果保存在静态字段 {@link #permissions} 中，仅首次调用时执行
 * 扫描，之后直接复用（注意：运行期新增 Controller 不会被重新扫描）。</p>
 *
 * @author javahuang
 * @date 2021/10/12
 */
@Slf4j
public class PreAuthorizeAnnotationExtractor {

	/**
	 * 权限码缓存集合（懒加载，首次提取后不再变化）。
	 */
	private static Set<String> permissions = null;

	/**
	 * 提取系统中全部 API 权限码（带缓存）。
	 *
	 * <p>遍历 Spring 容器中所有 @RestController Bean，分别提取类级与方法级的
	 * @PreAuthorize 注解，并把权限码按 {@code :} 拆成层级集合。</p>
	 *
	 * @return 去重后的层级权限码集合（LinkedHashSet 保证顺序稳定）
	 */
	public static Set<String> extractAllApiPermissions() {
		List<Object> controllerList = ContextHelper.getBeansByAnnotation(RestController.class);
		if (permissions == null) {
			permissions = new LinkedHashSet<>();
			extractApiPermissions(controllerList);
		}
		return permissions;
	}

	/**
	 * 扫描所有 Controller 的类级与方法级 @PreAuthorize 注解。
	 *
	 * @param controllerList 容器中所有 @RestController 的代理对象列表
	 */
	private static void extractApiPermissions(List<Object> controllerList) {
		for (Object obj : controllerList) {
			Class controllerClass = AopProxyUtils.ultimateTargetClass(obj);
			String title = null;
			// 提取类信息
			String codePrefix = null;
			// 注解
			PreAuthorize bindPermission = AnnotationUtils.findAnnotation(controllerClass, PreAuthorize.class);
			if (bindPermission != null) {
				// 当前资源权限
				parseAnnotationValue(bindPermission.value());
			}
			extractAnnotationMethods(controllerClass, PreAuthorize.class).forEach(method -> {
				PreAuthorize methodBindPermission = AnnotationUtils.getAnnotation(method, PreAuthorize.class);
				parseAnnotationValue(methodBindPermission.value());
			});
		}
	}

	/**
	 * 解析单个 @PreAuthorize 表达式中的权限码。
	 *
	 * <p>目前只识别包含 {@code hasAuthority} 的表达式，从单引号中截取权限码
	 * （形如 {@code @PreAuthorize("hasAuthority('answer:list')")}），并按
	 * {@code :} 拆分为层级集合加入 {@link #permissions}。</p>
	 *
	 * @param value @PreAuthorize 注解的表达式字符串（如 "hasAuthority('answer:list')"）
	 */
	private static void parseAnnotationValue(String value) {
		if (StringUtils.isBlank(value) || !StringUtils.contains(value, "hasAuthority")) {
			return;
		}
		String authorityCode = StringUtils.substringBetween(value, "'");
		if (StringUtils.isNotBlank(authorityCode)) {
			String[] authorityHierarchy = authorityCode.split(":");
			String current = authorityHierarchy[0];
			permissions.add(current.trim());
			for (int i = 1; i < authorityHierarchy.length; i++) {
				current = current + ":" + authorityHierarchy[i];
				permissions.add(current);
			}
		}
		else {
			log.warn("检测到权限码未配置 {}", value);
		}
	}

	/**
	 * 获取类及其所有父类中标注了指定注解的方法列表。
	 *
	 * <p>自底向上遍历继承链（含 Object），收集每个类上声明且带有目标注解的方法。
	 * 子类重写（override）父类方法时，以子类声明为准（父类的同名方法注解
	 * 会被子类方法取代，因为遍历时子类方法已加入列表）。</p>
	 *
	 * @param clazz          目标类（Controller 实现类）
	 * @param annotationClass 要查找的注解类型（如 PreAuthorize.class）
	 * @return 标注了目标注解的方法列表
	 */
	public static List<Method> extractAnnotationMethods(Class<?> clazz, Class<? extends Annotation> annotationClass) {
		List<Method> methodList = new ArrayList<>();
		while (clazz != null) {
			Method[] methods = clazz.getDeclaredMethods();
			if (methods != null) {
				// 被重写属性，以子类override的为准
				Arrays.stream(methods).forEach((method) -> {
					if (AnnotationUtils.getAnnotation(method, annotationClass) != null) {
						methodList.add(method);
					}
				});
			}
			clazz = clazz.getSuperclass();
		}
		return methodList;
	}

}
