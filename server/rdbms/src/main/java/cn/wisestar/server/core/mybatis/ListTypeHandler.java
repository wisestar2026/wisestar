package cn.wisestar.server.core.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.extension.handlers.AbstractJsonTypeHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * @author javahuang
 * @date 2021/9/17
 */
@Slf4j
@MappedTypes({ List.class })
@MappedJdbcTypes({ JdbcType.VARCHAR })
public abstract class ListTypeHandler<T> extends AbstractJsonTypeHandler<Object> {

	protected static ObjectMapper objectMapper = new ObjectMapper();

	private Class<?> type;

	public ListTypeHandler() {
	}

	public ListTypeHandler(Class<?> type) {
		if (log.isTraceEnabled()) {
			log.trace("JacksonTypeHandler(" + type + ")");
		}

		Assert.notNull(type, "Type argument cannot be null", new Object[0]);
		this.type = type;
	}

	/**
	 * 将 JSON 字符串解析为当前 handler 泛型对应的对象。
	 *
	 * @param json JSON 字符串
	 * @return 解析后的对象
	 */
	protected Object parse(String json) {
		try {
			Type type = getSuperGenricTypes(this.getClass());
			JavaType javaType = objectMapper.getTypeFactory().constructType(type);
			return objectMapper.readValue(json, javaType);
		}
		catch (IOException var3) {
			throw new RuntimeException(var3);
		}
	}

	/**
	 * 将对象序列化为 JSON 字符串。
	 *
	 * @param obj 待序列化对象
	 * @return JSON 字符串
	 */
	protected String toJson(Object obj) {
		try {
			return objectMapper.writeValueAsString(obj);
		}
		catch (JsonProcessingException var3) {
			throw new RuntimeException(var3);
		}
	}

	/**
	 * 设置用于 JSON 序列化与反序列化的静态 ObjectMapper。
	 *
	 * @param objectMapper 待设置的 ObjectMapper
	 */
	public static void setObjectMapper(ObjectMapper objectMapper) {
		Assert.notNull(objectMapper, "ObjectMapper should not be null", new Object[0]);
		objectMapper = objectMapper;
	}

	/**
	 * 自底向上查找父类，提取其首个泛型实参类型。
	 *
	 * @param clz 起始类
	 * @return 父类的首个泛型实参类型
	 * @throws IllegalArgumentException 向上查找至 Object 仍未找到参数化父类时抛出
	 */
	public static Type getSuperGenricTypes(final Class<?> clz) {
		Class<?> superClass = clz;
		Type type = superClass.getGenericSuperclass();
		while (superClass != Object.class && !(type instanceof ParameterizedType)) {
			superClass = superClass.getSuperclass();
			type = superClass.getGenericSuperclass();
		}
		if (superClass == Object.class) {
			throw new IllegalArgumentException("父类不含泛型类型：" + clz);
		}
		return ((ParameterizedType) type).getActualTypeArguments()[0];
	}

}