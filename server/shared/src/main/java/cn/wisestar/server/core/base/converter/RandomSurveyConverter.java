package cn.wisestar.server.core.base.converter;

import cn.wisestar.server.core.uitls.MapBeanUtils;
import cn.wisestar.server.domain.dto.ProjectSetting;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;

/**
 * 随机问卷条件转换器（RandomSurveyConverter）。
 *
 * <p><b>所属模块</b>：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。</p>
 * <p><b>类职责</b>：把 Spring 参数绑定产生的 {@link LinkedHashMap}（JSON 对象映射）
 * 转换为 {@link ProjectSetting.RandomSurveyCondition} 对象。
 * 在 {@link cn.wisestar.server.core.config.AppConfig#initAfterStartup()} 中
 * 注册到全局 DefaultConversionService。</p>
 *
 * <p><b>特殊处理</b>：随机问卷条件中的 examScore（分值）字段，前端 JSON 可能
 * 传整数（Integer）或小数（Double），本转换器先把 Integer 统一转为 Double，
*  再执行 map → bean 映射，避免类型不匹配导致的转换失败。</p>
 *
 * @author javahuang
 * @date 2022/5/15
 */
public class RandomSurveyConverter
		implements Converter<LinkedHashMap<String, Object>, ProjectSetting.RandomSurveyCondition> {

	/**
	 * 执行转换：修正 examScore 数值类型后映射为 RandomSurveyCondition 对象。
	 *
	 * @param value 源 Map（key 为属性名，value 为属性值）
	 * @return 转换后的 RandomSurveyCondition 对象
	 */
	@Override
	@SneakyThrows
	public ProjectSetting.RandomSurveyCondition convert(LinkedHashMap<String, Object> value) {
		Object examScore = value.get("examScore");
		// 将 Integer/Double 统一转换为 Double
		if (examScore != null && examScore instanceof Integer) {
			value.put("examScore", ((Integer) examScore).doubleValue());
		}

		return MapBeanUtils.mapToBean(value, ProjectSetting.RandomSurveyCondition.class);
	}

}
