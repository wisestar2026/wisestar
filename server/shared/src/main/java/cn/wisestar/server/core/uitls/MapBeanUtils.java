package cn.wisestar.server.core.uitls;

import org.springframework.cglib.beans.BeanMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author javahuang
 * @date 2022/5/15
 */
public class MapBeanUtils {

	/**
	 * 将 JavaBean 转换为字段名到值的 Map。
	 *
	 * @param <T>  Bean 类型
	 * @param bean 待转换的 JavaBean
	 * @return 字段名到值的 Map
	 */
	public static <T> Map<String, ?> beanToMap(T bean) {
		BeanMap beanMap = BeanMap.create(bean);
		Map<String, Object> map = new HashMap<>();

		beanMap.forEach((key, value) -> {
			map.put(String.valueOf(key), value);
		});
		return map;
	}

	public static <T> T mapToBean(Map<String, ?> map, Class<T> clazz)
			throws IllegalAccessException, InstantiationException {
		T bean = clazz.newInstance();
		BeanMap beanMap = BeanMap.create(bean);
		beanMap.putAll(map);
		return bean;
	}

	/**
	 * 将 JavaBean 列表批量转换为 Map 列表。
	 *
	 * @param <T>     Bean 类型
	 * @param objList 待转换的 JavaBean 列表
	 * @return Map 列表，入参为空时返回空列表
	 */
	public static <T> List<Map<String, ?>> objectsToMaps(List<T> objList) {
		List<Map<String, ?>> list = new ArrayList<>();
		if (objList != null && objList.size() > 0) {
			Map<String, ?> map = null;
			T bean = null;
			for (int i = 0, size = objList.size(); i < size; i++) {
				bean = objList.get(i);
				map = beanToMap(bean);
				list.add(map);
			}
		}
		return list;
	}

	public static <T> List<T> mapsToObjects(List<Map<String, ?>> maps, Class<T> clazz)
			throws InstantiationException, IllegalAccessException {
		List<T> list = new ArrayList<>();
		if (maps != null && maps.size() > 0) {
			Map<String, ?> map = null;
			for (int i = 0, size = maps.size(); i < size; i++) {
				map = maps.get(i);
				T bean = mapToBean(map, clazz);
				list.add(bean);
			}
		}
		return list;
	}

}
