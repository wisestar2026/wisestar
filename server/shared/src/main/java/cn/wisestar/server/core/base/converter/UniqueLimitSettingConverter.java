package cn.wisestar.server.core.base.converter;

import cn.wisestar.server.core.constant.AnswerFreqEnum;
import cn.wisestar.server.domain.dto.ProjectSetting;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;

/**
 * 唯一性/提交次数限制设置转换器（UniqueLimitSettingConverter）。
 *
 * <p><b>所属模块</b>：shared 模块基础框架转换器包（cn.wisestar.server.core.base.converter）。</p>
 * <p><b>类职责</b>：把 Spring 参数绑定产生的 {@link LinkedHashMap} 转换为
 * {@link ProjectSetting.UniqueLimitSetting} 对象。在
 * {@link cn.wisestar.server.core.config.AppConfig#initAfterStartup()} 中注册到
 * 全局 DefaultConversionService。</p>
 *
 * <p><b>转换细节</b>：从 Map 中取出 limitNum（次数上限，Integer）与
 * limitFreq（频率单位，String），把 limitFreq 通过 {@link AnswerFreqEnum#valueOf}
 * 转为枚举后构造 UniqueLimitSetting 对象。若 limitFreq 传入的字符串不在
 * AnswerFreqEnum 枚举定义中，会抛出 IllegalArgumentException（由参数校验层兜底）。</p>
 *
 * @author javahuang
 * @date 2022/2/28
 */
public class UniqueLimitSettingConverter implements Converter<LinkedHashMap<?, ?>, ProjectSetting.UniqueLimitSetting> {

	/**
	 * 执行转换：解析限制次数与频率枚举。
	 *
	 * @param value 源 Map（包含 limitNum、limitFreq 键）
	 * @return 转换后的 UniqueLimitSetting 对象
	 */
	@Override
	public ProjectSetting.UniqueLimitSetting convert(LinkedHashMap<?, ?> value) {
		Integer limitNum = (Integer) value.get("limitNum");
		String limitFreq = (String) value.get("limitFreq");
		return new ProjectSetting.UniqueLimitSetting(limitNum, AnswerFreqEnum.valueOf(limitFreq));
	}

}
