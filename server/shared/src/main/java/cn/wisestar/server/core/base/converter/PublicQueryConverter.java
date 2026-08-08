package cn.wisestar.server.core.base.converter;

import cn.wisestar.server.core.uitls.MapBeanUtils;
import cn.wisestar.server.domain.dto.ProjectSetting;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;

/**
 * 公共查询参数转换器（PublicQueryConverter）。
 *
 * <p><b>所属模块</b>：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。</p>
 * <p><b>类职责</b>：把 Spring 参数绑定产生的 {@link LinkedHashMap}（JSON 对象/
 * 查询参数映射）转换为 {@link ProjectSetting.PublicQuery} 对象。
 * 在 {@link cn.wisestar.server.core.config.AppConfig#initAfterStartup()} 中
 * 注册到全局 DefaultConversionService，供"问卷公共查询设置"等复杂参数的
 * 自动类型转换使用。</p>
 *
 * <p><b>使用场景</b>：前端提交 JSON 中包含对象类型的字段（如问卷的公共查询
 * 条件），Spring 无法直接实例化目标类型时，会调用本转换器完成 map → 对象的映射。</p>
 *
 * @author javahuang
 * @date 2022/5/15
 */
public class PublicQueryConverter implements Converter<LinkedHashMap<String, ?>, ProjectSetting.PublicQuery> {

	/**
	 * 执行转换：把 Map 属性值映射到 PublicQuery 的对应字段。
	 *
	 * @param value 源 Map（key 为属性名，value 为属性值）
	 * @return 转换后的 PublicQuery 对象
	 */
	@Override
	@SneakyThrows
	public ProjectSetting.PublicQuery convert(LinkedHashMap<String, ?> value) {
		return MapBeanUtils.mapToBean(value, ProjectSetting.PublicQuery.class);
	}

}
