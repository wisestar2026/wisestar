package cn.wisestar.server.core.constant;

/**
 * 附件命名变量枚举（AttachmentNameVariableEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义附件（上传文件）自定义命名规则中可用的变量名。
 * 用户在配置上传文件命名模板时，可以引用这些变量占位符，系统在保存文件时
 * 会将占位符替换为实际值（见 rdbms 模块附件命名逻辑）。</p>
 *
 * <p><b>可用变量</b>：</p>
 * <ul>
 *   <li>projectId：项目 id</li>
 *   <li>serialNum：全局附件序号（所有附件从 1 开始编号）</li>
 *   <li>serialNumInAnswer：同一问卷内附件序号（从 1 开始）</li>
 *   <li>uploadDate / uploadDateTime：上传日期 / 日期时间</li>
 *   <li>sourceName：原始文件名</li>
 *   <li>questionTitle：问题标题</li>
 * </ul>
 *
 * @author javahuang
 * @date 2022/4/11
 */
public enum AttachmentNameVariableEnum {

	/** 项目id */
	projectId,
	/** 所有的附件从1开始编号 */
	serialNum,
	/** 同一个问卷内附件从1开始编号 */
	serialNumInAnswer,
	/** 上传日期 */
	uploadDate,
	/** 上传日期时间 */
	uploadDateTime,
	/** 原始文件名 */
	sourceName,
	/** 问题标题 */
	questionTitle

}
