package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.knowledge.SectionPracticeConfig;

/**
 * 小节练习配置服务。
 *
 * <p>读取 t_section.practice JSON 并解析为 {@link SectionPracticeConfig}，
 * 供出题引擎（专项练习/小节通关）与通关判定使用。解析失败时返回缺省配置。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
public interface SectionPracticeService {

	/**
	 * 读取小节练习配置。
	 *
	 * @param sectionId 小节ID
	 * @return 练习配置（小节不存在或配置缺失时返回缺省配置）
	 */
	SectionPracticeConfig getConfig(String sectionId);

	/**
	 * 解析练习配置 JSON 原文。
	 *
	 * @param json t_section.practice 原文
	 * @return 练习配置（原文为空或非法时返回缺省配置）
	 */
	SectionPracticeConfig parse(String json);

}
