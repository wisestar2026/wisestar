package cn.wisestar.server.core.constant;

/**
 * 缓存名称常量（CacheConsts）。
 *
 * <p><b>所属模块</b>：shared 模块常量包（cn.wisestar.server.core.constant）。</p>
 * <p><b>类职责</b>：集中定义 Spring Cache 抽象中使用的所有 cacheName 常量。
 * 业务代码通过 @Cacheable(cacheNames = CacheConsts.xxx) 引用，
 * 避免魔法字符串散落各处。每个 cacheName 对应的过期时长在配置文件
 * {@code custom-cache.entries} 中配置（见
 * {@link cn.wisestar.server.core.config.CacheConfig}）。</p>
 *
 * <p><b>注意</b>：cacheName 必须与 application.yml 中 custom-cache.entries
 * 的 key 保持一致，否则 CacheConfig 创建缓存时取不到 TTL 配置会抛空指针。</p>
 *
 * @author javahuang
 * @date 2021/10/15
 */
public final class CacheConsts {

	/** 项目信息缓存名（缓存项目视图/明细数据） */
	public static final String projectCache = "projectCache";

	/** 通用数据缓存名（字典等通用数据） */
	public static final String commonCacheName = "commonCache";

	/** 用户信息缓存名 */
	public static final String userCacheName = "userCache";

	/** 文件信息缓存名 */
	public static final String fileCacheName = "fileCache";

	/** 部门树缓存名 */
	public static final String deptCacheName = "deptCache";

	/** 项目权限缓存名（当前用户可访问的项目集合） */
	public static final String projectPermissionCacheName = "projectPermissionCache";

}
