package cn.wisestar.server.core.base.converter;

import cn.wisestar.server.core.constant.AnswerFreqEnum;
import cn.wisestar.server.domain.dto.ProjectSetting;
import org.springframework.core.convert.converter.Converter;

import java.util.LinkedHashMap;

/**
 * spel converter
 *
 * @author javahuang
 * @date 2022/2/28
 */
public class UniqueLimitSettingConverter implements Converter<LinkedHashMap<?, ?>, ProjectSetting.UniqueLimitSetting> {

	@Override
	public ProjectSetting.UniqueLimitSetting convert(LinkedHashMap<?, ?> value) {
		Integer limitNum = (Integer) value.get("limitNum");
		String limitFreq = (String) value.get("limitFreq");
		return new ProjectSetting.UniqueLimitSetting(limitNum, AnswerFreqEnum.valueOf(limitFreq));
	}

}
