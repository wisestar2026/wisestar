package cn.wisestar.server.storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * 存储服务接口（StorageService）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：定义文件存储抽象能力，屏蔽底层存储介质差异（当前唯一
 * 实现为本地磁盘 {@link LocalStorageService}，未来可扩展 OSS、S3 等）。
 * 业务代码（如 FileService 上传/下载接口）只依赖本接口编程。</p>
 *
 * <p><b>能力清单</b>：</p>
 * <ul>
 *   <li>init：初始化存储（如创建根目录）；</li>
 *   <li>uploadFile：上传文件（输入流 + 相对路径）；</li>
 *   <li>download / downloadAsStream：按相对路径读取文件（字节 / 流）；</li>
 *   <li>getThumbImageFilePath：由原路径推导缩略图路径；</li>
 *   <li>generateThumbImage：内存生成缩略图。</li>
 * </ul>
 *
 * <p><b>路径约定</b>：所有 path 均为相对存储根目录的相对路径。</p>
 *
 * @author javahuang
 * @date 2021/9/6
 */
public interface StorageService {

	/**
	 * 初始化存储资源（如创建根目录）；默认空实现。
	 */
	default void init() {
	}

	/**
	 * 上传文件。
	 *
	 * @param file 文件内容输入流
	 * @param path 目标相对路径（相对存储根目录）
	 */
	void uploadFile(InputStream file, String path);

	// /**
	// * 上传附件并且生成缩略图 缩略图为上传文件名+缩略图后缀 _150x150，如 xxx.jpg，缩略图为 xxx_150x150.jpg
	// */
	// StorePath uploadImage(UploadFileRequest request);

	/**
	 * 按相对路径下载文件（返回全部字节）。
	 *
	 * @param filePath 相对路径
	 * @return 文件内容字节数组
	 */
	byte[] download(String filePath);

	/**
	 * 按相对路径以输入流方式下载文件。
	 *
	 * @param filePath 相对路径
	 * @return 文件输入流
	 */
	InputStream downloadAsStream(String filePath);

	/**
	 * 根据原文件路径推导缩略图路径。
	 *
	 * @param filePath 原文件相对路径
	 * @return 缩略图相对路径
	 */
	String getThumbImageFilePath(String filePath);

	/**
	 * 从输入流生成缩略图输入流（内存操作）。
	 *
	 * @param inputStream 原图输入流
	 * @return 缩略图输入流
	 * @throws IOException 图片处理失败
	 */
	InputStream generateThumbImage(InputStream inputStream) throws IOException;

}
