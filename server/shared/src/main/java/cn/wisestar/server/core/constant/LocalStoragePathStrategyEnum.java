package cn.wisestar.server.core.constant;

/**
 * 本地存储路径策略枚举（LocalStoragePathStrategyEnum）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：定义本地存储（LocalStorageService）保存文件时的
 * 目录组织策略，供 {@link cn.wisestar.server.storage.StorageProperties}
 * 中 path-strategy 配置项引用（如 {@code storage.path-strategy: byId}）。</p>
 *
 * <p><b>取值说明</b>：</p>
 * <ul>
 *   <li>byNo：所有文件直接存 rootPath 下；</li>
 *   <li>byId：按项目的 short-id 分文件夹（rootPath/RyP2rR）；</li>
 *   <li>byDate：按上传日期分文件夹（rootPath/2022/06/01）。</li>
 * </ul>
 *
 * @author Jiutwo
 */
public enum LocalStoragePathStrategyEnum {

	/**
	 * 所有文件存储在 rootPath 下
	 */
	BY_NO("byNo"),
	/**
	 * 按照项目的short-id分文件夹存储,例如 rootPath/RyP2rR
	 */
	BY_ID("byId"),
	/**
	 * 按照上传日期存储，例如 rootPath/2022/06/01
	 */
	BY_DATE("byDate");

	/**
	 * 策略标识（配置文件中使用的字符串值）。
	 */
	private final String strategy;

	LocalStoragePathStrategyEnum(String strategy) {
		this.strategy = strategy;
	}

	/**
	 * 获取策略标识。
	 *
	 * @return 策略字符串（如 "byId"）
	 */
	public String getStrategy() {
		return strategy;
	}

}
