package cn.wisestar.server.domain.dto.growth;

import lombok.Data;

/**
 * 每日学习轨迹视图（成长时间轴条目，按天聚合为一句叙述）。
 *
 * <p>把同一名学员同一天的全部学习轨迹事件聚合为一条面向家长的叙述文字；
 * 优先由 AI 生成语气自然的总结，AI 不可用时降级为规则模板，{@code model} 标记为 rule。</p>
 *
 * @author wisestar
 * @date 2026/10/11
 */
@Data
public class GrowthDailyView {

	/** 发生日期 yyyy-MM-dd */
	private String date;

	/** 学科ID（当天仅涉及单一学科时有值） */
	private String subjectId;

	/** 学科名称快照（当天仅涉及单一学科时有值） */
	private String subjectName;

	/** 当日学习叙述文字（纯文字，不含数字） */
	private String content;

	/** 生成方式：ai / rule */
	private String model;

}
