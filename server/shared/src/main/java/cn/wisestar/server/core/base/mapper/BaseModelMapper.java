package cn.wisestar.server.core.base.mapper;

import cn.wisestar.server.core.uitls.ContextHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.CollectionUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Model对象到Domain类型对象的相互转换。实现类通常声明在Model实体类中。
 *
 * <p><b>所属模块</b>：shared 模块基础框架（cn.wisestar.server.core.base.mapper）。</p>
 * <p><b>类职责</b>：定义"请求 DTO（R）→ 数据库实体（M）→ 视图 DTO（V）"
 * 三层对象转换的标准接口，并基于 Jackson 提供
 * "bean ↔ map" 的默认转换工具方法。各模块在 Model 实体类中通过
 * MapStruct 生成该接口的实现（配合 mapToMap 优化 Map 字段的拷贝）。</p>
 *
 * <p><b>数据流</b>：前端请求（Request DTO）→ fromRequest → Model 实体 → 落库；
 * 数据库查询（Model 实体）→ toView → View DTO → 响应前端。</p>
 *
 * @param <R> Request请求实体对象，通常用于新增和修改。
 * @param <V> View视图对象。
 * @param <M> Model数据库实体对象类型。
 * @author Jerry
 * @date 2021-06-06
 */
public interface BaseModelMapper<R, V, M> {

	/**
	 * 将请求转换成 Model 实体对象。
	 * @param request Model实体对象。
	 * @return Domain域对象。
	 */
	M fromRequest(R request);

	/**
	 * 批量：请求列表 → Model 实体列表。
	 * @param requestList 请求对象列表。
	 * @return Model 实体对象列表。
	 */
	List<M> fromRequest(List<R> requestList);

	/**
	 * 转换Model实体对象到视图对象。
	 * @param model model 对象。
	 * @return View视图对象。
	 */
	V toView(M model);

	/**
	 * 批量：Model 实体列表 → 视图对象列表。
	 * @param modelList Model 实体对象列表。
	 * @return View视图对象列表。
	 */
	List<V> toView(List<M> modelList);

	/**
	 * 转换bean到map
	 * @param bean bean对象。
	 * @return 转换后的map对象。
	 */
	default Map<String, Object> beanToMap(Object bean) {
		ObjectMapper om = ContextHelper.getBean(ObjectMapper.class);
		return om.convertValue(bean, Map.class);
	}

	/**
	 * 转换bean集合到map集合
	 * @param dataList bean对象集合。
	 * @param <T> bean类型。
	 * @return 转换后的map对象集合。
	 */
	default <T> List<Map<String, Object>> beanToMap(List<T> dataList) {
		if (CollectionUtils.isEmpty(dataList)) {
			return new LinkedList<>();
		}
		return dataList.stream().map(this::beanToMap).collect(Collectors.toList());
	}

	/**
	 * 转换map到bean。
	 * @param map map对象。
	 * @param beanClazz bean的Class对象。
	 * @param <T> bean类型。
	 * @return 转换后的bean对象。
	 */
	default <T> T mapToBean(Map<String, Object> map, Class<T> beanClazz) {
		ObjectMapper om = ContextHelper.getBean(ObjectMapper.class);
		return om.convertValue(map, beanClazz);
	}

	/**
	 * 转换map集合到bean集合。
	 * @param mapList map对象集合。
	 * @param beanClazz bean的Class对象。
	 * @param <T> bean类型。
	 * @return 转换后的bean对象集合。
	 */
	default <T> List<T> mapToBean(List<Map<String, Object>> mapList, Class<T> beanClazz) {
		if (CollectionUtils.isEmpty(mapList)) {
			return new LinkedList<>();
		}
		return mapList.stream().map(m -> mapToBean(m, beanClazz)).collect(Collectors.toList());
	}

	/**
	 * 对于Map字段到Map字段的映射场景，MapStruct会根据方法签名自动选择该函数 作为对象copy的函数。由于该函数是直接返回的，因此没有对象copy，效率更高。
	 * 如果没有该函数，MapStruct会生成如下代码： Map<String, Object> map =
	 * courseDto.getTeacherIdDictMap(); if ( map != null ) { course.setTeacherIdDictMap(
	 * new HashMap<String, Object>( map ) ); }
	 * @param map map对象。
	 * @return 直接返回的map。
	 */
	default Map<String, Object> mapToMap(Map<String, Object> map) {
		return map;
	}

}
