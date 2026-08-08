package cn.wisestar.server.storage;

import cn.wisestar.server.core.constant.LocalStorageNameStrategyEnum;
import cn.wisestar.server.core.constant.LocalStoragePathStrategyEnum;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import java.io.File;

/**
 * 文件存储配置属性（StorageProperties）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：通过 {@code @ConfigurationProperties("file-storage")} 绑定
 * 配置文件前缀 {@code file-storage} 下的所有存储相关属性，供
 * {@link LocalStorageService} / {@link AbstractStorageService} 读取使用。</p>
 *
 * <p><b>配置示例</b>（application.yml）：
 * <pre>
 * file-storage:
 *   local:
 *     root-path: ./data/file
 *     path-strategy: byId     # byNo / byId / byDate
 *     date-format: yyyyMM/dd
 *     name-strategy: seqAndOriginalName   # seqAndOriginalName / originalNameAndSeq / seq / uuid
 *   thumb-image:
 *     width: 640
 *     height: 480
 * </pre></p>
 *
 * @author javahuang
 * @date 2021/9/7
 */
@ConfigurationProperties("file-storage")
@Data
public class StorageProperties {

	/**
	 * 本次存储配置（本地存储的根目录与路径/文件名策略）
	 */
	@NestedConfigurationProperty
	private final LocalStorage local = new LocalStorage();

	/**
	 * 缩略图配置（上传图片时生成的缩略图尺寸）。
	 */
	public final ThumbImage thumbImage = new ThumbImage();

	/**
	 * 缩略图尺寸配置（file-storage.thumb-image.*）。
	 */
	@Data
	public class ThumbImage {

		/**
		 * 生成的缩略图的宽度
		 */
		private int width = 640;

		/**
		 * 生成的缩略图的高度
		 */
		private int height = 480;

	}

	/**
	 * 本地磁盘存储配置（file-storage.local.*）。
	 */
	@Data
	public class LocalStorage {

		/**
		 * 本次存储目录的根目录
		 */
		private String rootPath;

		/**
		 * 路劲策略： 1. byNo:所有文件存储在 rootPath 下 2. byId:按照项目的short-id分文件夹存储,例如 rootPath/RyP2rR
		 * 3. byDate:按照上传日期存储，例如 rootPath/2022/06/01
		 */
		private String pathStrategy = LocalStoragePathStrategyEnum.BY_ID.getStrategy();

		/**
		 * 日期格式（pathStrategy=byDate 时按此格式组织日期目录）
		 */
		private String dateFormat = "yyyyMM" + File.separator + "dd";

		/**
		 * 文件名策略 1. seqAndOriginalName: 序列号加原文件名 2. originalNameAndSeq: 原文件名+序列号 3. Seq:
		 * 序列号（项目启动时间戳的自增） 4. UUID: 去除短杠'-'的UUID
		 */
		private String nameStrategy = LocalStorageNameStrategyEnum.SEQ_ADN_ORIGINAL_NAME.getStrategy();

	}

}
