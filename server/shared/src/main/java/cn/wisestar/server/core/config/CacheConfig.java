package cn.wisestar.server.core.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.index.qual.NonNegative;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 缓存配置（CacheConfig）。
 *
 * <p><b>所属模块</b>：shared 模块核心框架配置包（cn.wisestar.server.core.config）。</p>
 * <p><b>类职责</b>：启用 Spring 缓存抽象（@EnableCaching），并基于
 * <em>Caffeine</em> 本地缓存自定义 {@link CacheManager}，核心能力是：
 * <b>允许针对每个 cacheName 单独配置过期时间</b>（区别于 Spring 默认的
 * ConcurrentMapCacheManager 全局统一 TTL）。</p>
 *
 * <p><b>工作机制</b>：</p>
 * <ul>
 *   <li>通过 {@code @ConfigurationProperties("custom-cache")} 从配置文件读取
 *       {@code custom-cache.entries} 映射（cacheName → 过期时长 Duration）；</li>
 *   <li>创建 Caffeine 缓存：每个缓存条目的 TTL 由对应 cacheName 配置决定
 *       （expireAfterCreate 读取配置，expireAfterUpdate/expireAfterRead 保持原时长），
 *       单缓存最大容量 100 条；</li>
 *   <li>预注册所有配置的 cacheName，业务代码通过 @Cacheable / @CacheEvict 等注解
 *       使用缓存时按名字命中对应 Caffeine 缓存。</li>
 * </ul>
 *
 * <p><b>使用示例</b>（application.yml）：
 * <pre>
 * custom-cache:
 *   entries:
 *     user: 30m      # user 缓存 30 分钟
 *     deptTree: 1h   # deptTree 缓存 1 小时
 * </pre></p>
 *
 * <p><b>业务用途</b>：缓存用户信息、部门树、字典等读取频繁、变更低频的数据，
 * 降低数据库压力；配合 {@link cn.wisestar.server.core.cache.DeptKeyGenerator}
 * 等自定义 key 生成器使用。</p>
 *
 * @author javahuang
 * @date 2021/9/8
 */
@Configuration
@EnableCaching
@ConfigurationProperties("custom-cache")
public class CacheConfig extends CachingConfigurerSupport {

	/**
	 * 缓存配置映射：cacheName → 过期时长（由配置前缀 custom-cache.entries 绑定）。
	 */
	private Map<String, Duration> entries;

	/**
	 * 创建自定义 CacheManager（覆盖父类默认实现）。
	 *
	 * <p>内部采用匿名子类方式扩展 {@link ConcurrentMapCacheManager}：</p>
	 * <ul>
	 *   <li>每个 cacheName 构建一个 ConcurrentMapCache，底层存储使用 Caffeine
	 *       的 asMap()（具备 TTL 过期能力）；</li>
	 *   <li>过期策略：创建时按 entries 配置设置 TTL；更新/读取时沿用当前剩余时长
	 *       （不自动续期）；</li>
	 *   <li>maximumSize(100)：单缓存最多缓存 100 个键值对，防止内存无限增长。</li>
	 * </ul>
	 *
	 * @return 按 cacheName 分别配置 TTL 的缓存管理器
	 */
	@Bean
	@Override
	public CacheManager cacheManager() {
		ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager() {

			@Override
			protected Cache createConcurrentMapCache(final String name) {
				return new ConcurrentMapCache(name, Caffeine.newBuilder().expireAfter(new Expiry<Object, Object>() {
					@Override
					public long expireAfterCreate(Object key, Object value, long currentTime) {
						return entries.get(name).toNanos();
					}

					@Override
					public long expireAfterUpdate(Object key, Object value, long currentTime,
							@NonNegative long currentDuration) {
						return currentDuration;
					}

					@Override
					public long expireAfterRead(Object key, Object value, long currentTime,
							@NonNegative long currentDuration) {
						return currentDuration;
					}
				}).maximumSize(100).build().asMap(), false);
			}
		};

		cacheManager.setCacheNames(entries.entrySet().stream().map(x -> x.getKey()).collect(Collectors.toList()));
		return cacheManager;
	}

	/**
	 * 获取缓存配置映射。
	 *
	 * @return cacheName → 过期时长 的 Map
	 */
	public Map<String, Duration> getEntries() {
		return entries;
	}

	/**
	 * 设置缓存配置映射（由 Spring Boot ConfigurationProperties 绑定调用）。
	 *
	 * @param entries cacheName → 过期时长 的 Map
	 */
	public void setEntries(Map<String, Duration> entries) {
		this.entries = entries;
	}

}
