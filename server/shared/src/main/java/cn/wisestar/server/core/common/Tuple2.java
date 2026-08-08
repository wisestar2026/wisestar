package cn.wisestar.server.core.common;

/**
 * 二元组对象（Tuple2）。
 *
 * <p><b>所属模块</b>：shared 模块核心通用类（cn.wisestar.server.core.common）。</p>
 * <p><b>类职责</b>：简单的不可变二元组（Pair），用于"一个方法需要同时返回两个值"
 * 的场景，避免为临时结果单独定义 DTO 或使用 Object[] 数组。</p>
 *
 * <p><b>使用场景</b>：如 Service/工具方法需要返回"对象 + 状态"、"结果 + 计数"等
 * 成对数据时使用。字段通过 final 保证不可变，构造后不可修改。</p>
 *
 * @param <T1> 第一个值的类型
 * @param <T2> 第二个值的类型
 * @author javahuang
 * @date 2022/2/28
 */
public class Tuple2<T1, T2> {

	/**
	 * 第一个值（不可变）。
	 */
	private final T1 first;

	/**
	 * 第二个值（不可变）。
	 */
	private final T2 second;

	/**
	 * 构造二元组。
	 *
	 * @param first  第一个值
	 * @param second 第二个值
	 */
	public Tuple2(T1 first, T2 second) {
		this.first = first;
		this.second = second;
	}

	/**
	 * 获取第一个值。
	 *
	 * @return 第一个值
	 */
	public T1 getFirst() {
		return first;
	}

	/**
	 * 获取第二个值。
	 *
	 * @return 第二个值
	 */
	public T2 getSecond() {
		return second;
	}

}
