package cn.wisestar.server.core.constant;

/**
 * 报表统计 Key 枚举（ReportStatKeyEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义项目报表（数据看板）中使用的统计指标 Key，
 * 作为统计数据聚合结果的标识（如日报表按天统计的数量）。
 * 目前仅定义"每日数量"指标，后续统计维度在此扩展。</p>
 *
 * @author javahuang
 * @date 2022/5/8
 */
public enum ReportStatKeyEnum {

	/** 每日数量 */
	dailyCount

}
