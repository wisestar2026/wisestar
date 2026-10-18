package cn.wisestar.server.domain.dto.english;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语学习诊断交卷请求。
 *
 * <p>题目以内容键（contentKey）标识，后端按内容键回读句子包/情景包复核答案，
 * 并按单元、模块三态加权后落诊断记录，供学习趋势追踪使用。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
public class EnglishDiagnoseSubmitRequest {

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别 */
	private String term;

	/** 作答列表 */
	private List<EnglishUnitPaperSubmitRequest.Item> items = new ArrayList<>();

	/** 作答耗时（ms） */
	private Long durationMs;

	/** 客户端幂等令牌 */
	private String clientToken;

}
