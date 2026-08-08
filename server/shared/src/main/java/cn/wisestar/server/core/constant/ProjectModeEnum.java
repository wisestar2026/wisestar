package cn.wisestar.server.core.constant;

/**
 * 项目场景模式枚举（ProjectModeEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义项目（Project）/模板（Template）的场景模式，
 * 决定项目的使用方式与功能集合。项目与模板视图/请求 DTO 中均通过
 * 本枚举标识模式。</p>
 *
 * <p><b>取值说明</b>：</p>
 * <ul>
 *   <li>survey：问卷模式（收集数据）；</li>
 *   <li>exam：考试模式（自动判分）；</li>
 *   <li>folder：文件夹模式（用于组织项目层级，非可答题项目）。</li>
 * </ul>
 *
 * @author javahuang
 * @date 2022/4/7
 */
public enum ProjectModeEnum {

	/** 问卷模式 */
	survey,
	/** 考试模式 */
	exam,
	/** 文件夹 */
	folder

}
