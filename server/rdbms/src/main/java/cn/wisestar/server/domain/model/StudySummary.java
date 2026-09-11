package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 当日学习总结（t_study_summary）。
 *
 * <p>学员学习会话累计满 60 分钟后由系统生成；以 {@code studentId + summaryDate}
 * 定位当日唯一总结，重复触发时覆盖更新。{@code model} 记录生成来源，
 * AI 不可用时由规则模板生成并标记为 {@code rule}。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
@TableName("t_study_summary")
@EqualsAndHashCode(callSuper = false)
public class StudySummary extends BaseModel {

	/** 学员ID */
	private String studentId;

	/** 总结日期 yyyy-MM-dd */
	private String summaryDate;

	/** 触发总结的会话ID */
	private String sessionId;

	/** 总结正文 */
	private String content;

	/** 生成模型，规则模板为 rule */
	private String model;

	/** 状态：success/failed */
	private String status;

}
