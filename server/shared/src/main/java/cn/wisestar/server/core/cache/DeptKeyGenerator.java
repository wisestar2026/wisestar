package cn.wisestar.server.core.cache;

import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 部门缓存 Key 生成器（DeptKeyGenerator）。
 *
 * <p><b>所属模块</b>：shared 模块缓存包（cn.wisestar.server.core.cache）。</p>
 * <p><b>类职责</b>：实现 Spring 缓存抽象 {@link KeyGenerator}，为部门相关的
 * @Cacheable 注解方法生成缓存 Key。Bean 名称 "deptKeyGenerator"，
 * 可通过 @Cacheable(keyGenerator = "deptKeyGenerator") 引用。</p>
 *
 * <p><b>Key 策略</b>：按第一个参数是否为 null 区分：</p>
 * <ul>
 *   <li>参数为 null → Key "all"：缓存"整棵部门树"（全量查询结果）；</li>
 *   <li>参数非 null → Key "some"：缓存"部分部门"（按条件筛选的查询结果）。</li>
 * </ul>
 * <p>即部门查询被划分为"全量"与"部分"两档缓存，避免大量不同查询参数
 * 造成缓存条目爆炸。</p>
 *
 * @author javahuang
 * @date 2022/2/8
 */
@Component("deptKeyGenerator")
public class DeptKeyGenerator implements KeyGenerator {

	/**
	 * 根据方法参数生成缓存 Key。
	 *
	 * @param target 目标对象（未使用）
	 * @param method 目标方法（未使用）
	 * @param params 方法参数（取第一个参数判断是否为空）
	 * @return "all"（参数为空）或 "some"（参数非空）
	 */
	@Override
	public Object generate(Object target, Method method, Object... params) {
		if (params[0] == null) {
			return "all";
		}
		return "some";
	}

}
