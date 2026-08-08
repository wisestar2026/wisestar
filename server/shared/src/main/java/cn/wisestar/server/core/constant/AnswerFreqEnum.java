package cn.wisestar.server.core.constant;

/**
 * 答题频率枚举（AnswerFreqEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义"答卷人同一问卷的提交频率限制"的单位枚举，
 * 每个枚举值同时携带对应的 Quartz Cron 表达式，用于"限制周期结束"
 * 定时任务的调度。与 {@link cn.wisestar.server.domain.dto.ProjectSetting.UniqueLimitSetting}
 * 配合使用（该对象由 {@link cn.wisestar.server.core.base.converter.UniqueLimitSettingConverter}
 * 将 limitFreq 字符串转换为本枚举）。</p>
 *
 * <p><b>取值说明</b>：only（仅一次）、hour（每小时）、day（每天）、week（每周）、
 * month（每月）、quarter（每季度）、year（每年）。</p>
 *
 * @author javahuang
 * @date 2022/2/27
 */
public enum AnswerFreqEnum {

	/** 仅一次（无周期 cron，表示不做周期重置） */
	only(""),
	/** 小时 */
	hour("0 0 * * * *"),
	/** 天 */
	day("0 0 0 1-31 * *"),
	/** 星期 */
	week("0 0 0 1-31 1-12 1"),
	/** 月 */
	month("0 0 0 1 * *"),
	/** 季度 */
	quarter("0 0 0 1 */3 *"),
	/** 年 */
	year("0 0  0 1 1 *");

	/**
	 * 对应频率的 Quartz Cron 表达式（用于周期重置定时任务）。
	 */
	private String cron;

	AnswerFreqEnum(String cron) {
		this.cron = cron;
	}

	/**
	 * 获取 Cron 表达式。
	 *
	 * @return Quartz Cron 表达式字符串（only 为空串）
	 */
	public String getCron() {
		return cron;
	}

}
