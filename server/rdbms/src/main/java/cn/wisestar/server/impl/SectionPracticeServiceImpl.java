package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.knowledge.SectionPracticeConfig;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.service.SectionPracticeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 小节练习配置服务实现。
 *
 * <p>将 t_section.practice 的 JSON 原文反序列化为 {@link SectionPracticeConfig}；
 * 原文为空或非法时补全缺省值并记录告警，不阻断组卷与判定。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SectionPracticeServiceImpl implements SectionPracticeService {

	private final SectionMapper sectionMapper;

	private final ObjectMapper objectMapper;

	@Override
	public SectionPracticeConfig getConfig(String sectionId) {
		if (!StringUtils.hasText(sectionId)) {
			return parse(null);
		}
		Section section = sectionMapper.selectById(sectionId);
		return parse(section == null ? null : section.getPractice());
	}

	@Override
	public SectionPracticeConfig parse(String json) {
		SectionPracticeConfig config = new SectionPracticeConfig();
		if (StringUtils.hasText(json)) {
			try {
				SectionPracticeConfig parsed = objectMapper.readValue(json, SectionPracticeConfig.class);
				if (parsed != null) {
					config = parsed;
				}
			}
			catch (Exception e) {
				log.warn("section practice config parse failed, fallback to defaults", e);
			}
		}
		config.applyDefaults();
		return config;
	}

}
