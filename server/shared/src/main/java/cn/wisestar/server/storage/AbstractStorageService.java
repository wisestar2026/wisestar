package cn.wisestar.server.storage;

import lombok.Data;
import net.coobird.thumbnailator.Thumbnails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 存储服务抽象基类（AbstractStorageService）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：实现 {@link StorageService} 接口中与具体存储介质无关的
 * 公共逻辑——缩略图能力：</p>
 * <ul>
 *   <li>从配置读取缩略图尺寸（宽/高，见 {@link StorageProperties.ThumbImage}，
 *       默认 640x480）；</li>
 *   <li>生成缩略图文件路径：在原始文件名后缀前插入 {@code _宽x高}（如
 *       xxx.jpg → xxx_640x480.jpg，见 {@link #getThumbImageFilePath}）；</li>
 *   <li>基于 Thumbnailator 在内存中生成缩略图字节流（见
 *       {@link #generateThumbImage}）。</li>
 * </ul>
 *
 * <p><b>子类</b>：{@link LocalStorageService}（本地磁盘存储）继承本类，
 * 只需实现文件的上传/下载与初始化即可复用缩略图能力。构造时传入
 * {@link StorageProperties} 并调用子类 {@link #init()} 完成存储目录初始化。</p>
 *
 * @author javahuang
 * @date 2021/9/8
 */
@Data
public abstract class AbstractStorageService implements StorageService {

	/**
	 * 存储配置（含本地存储根目录、路径/文件名策略、缩略图尺寸等）。
	 */
	protected StorageProperties storageConfig;

	/**
	 * 缩略图宽度（px，由配置 thumbImage.width 决定，默认 640）。
	 */
	private int thumbImageWidth;

	/**
	 * 缩略图高度（px，由配置 thumbImage.height 决定，默认 480）。
	 */
	private int thumbImageHeight;

	/**
	 * 构造器：读取缩略图尺寸配置并触发子类存储初始化。
	 *
	 * @param storageConfig 存储配置
	 */
	public AbstractStorageService(StorageProperties storageConfig) {
		this.storageConfig = storageConfig;
		this.thumbImageWidth = storageConfig.getThumbImage().getWidth();
		this.thumbImageHeight = storageConfig.getThumbImage().getHeight();
		this.init();
	}

	/**
	 * 生成缩略图文件名后缀（如 "_640x480"）。
	 *
	 * @return 形如 _宽x高 的后缀字符串
	 */
	protected String getThumbPrefixName() {
		StringBuilder buffer = new StringBuilder();
		return buffer.append("_").append(thumbImageWidth).append("x").append(thumbImageHeight).toString();
	}

	/**
	 * 根据原文件路径推导缩略图文件路径。
	 *
	 * <p>在最后一个点号（扩展名前）插入缩略图后缀：
	 * 如 "dir/xxx.jpg" → "dir/xxx_640x480.jpg"。</p>
	 *
	 * @param filePath 原文件相对路径
	 * @return 缩略图相对路径
	 */
	@Override
	public String getThumbImageFilePath(String filePath) {
		StringBuilder buff = new StringBuilder(filePath);
		int index = buff.lastIndexOf(".");
		buff.insert(index, getThumbPrefixName());
		return buff.toString();
	}

	/**
	 * 在内存中生成缩略图字节流（不落盘）。
	 *
	 * <p>使用 Thumbnailator 把输入图片缩放为配置的宽高尺寸，
	 * 结果写入内存中的 ByteArrayInputStream，供上传流程直接保存。</p>
	 *
	 * @param inputStream 原图输入流
	 * @return 缩略图输入流
	 * @throws IOException 图片处理失败
	 */
	@Override
	public ByteArrayInputStream generateThumbImage(InputStream inputStream) throws IOException {
		// 在内存当中生成缩略图
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		Thumbnails.of(inputStream).size(thumbImageWidth, thumbImageHeight).toOutputStream(out);
		return new ByteArrayInputStream(out.toByteArray());
	}

}
