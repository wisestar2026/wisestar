package cn.wisestar.server.storage;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * 存储服务自动配置（StorageAutoConfiguration）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：Spring Boot 自动配置类，负责在满足条件时创建
 * {@link StorageService} 实例注入容器，供业务代码（如 FileService）注入使用。</p>
 *
 * <p><b>装配条件</b>：</p>
 * <ul>
 *   <li>@ConfigurationPropertiesScan：扫描本包下所有 @ConfigurationProperties
 *       类（即 {@link StorageProperties}），绑定 file-storage.* 配置；</li>
 *   <li>@ConditionalOnProperty：配置了 {@code file-storage.local.root-path} 才创建
 *       （未配置本地存储时不装配，便于后续扩展其他存储介质）；</li>
 *   <li>@ConditionalOnMissingBean：容器中没有其他 StorageService 实现时创建，
 *       允许外部覆盖默认本地存储实现。</li>
 * </ul>
 *
 * <p><b>数据流</b>：文件上传请求 → FileService → StorageService（本类创建的
 * LocalStorageService）→ 本地磁盘。</p>
 *
 * @author javahuang
 * @date 2021/9/6
 */
@Configuration
@ConfigurationPropertiesScan("cn.wisestar.server.storage")
public class StorageAutoConfiguration {

	/**
	 * 创建本地存储服务 Bean（默认存储实现）。
	 *
	 * @param properties 存储配置（file-storage.* 绑定）
	 * @return LocalStorageService 实例
	 * @throws IOException 存储初始化异常
	 */
	@Bean
	@ConditionalOnProperty(prefix = "file-storage.local", name = "root-path")
	@ConditionalOnMissingBean
	public StorageService storageService(StorageProperties properties) throws IOException {
		return new LocalStorageService(properties);
	}

}
