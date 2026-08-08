package cn.wisestar.server.storage;

import lombok.Data;

/**
 * 存储文件的路径信息（StorePath）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：文件上传完成后的结果载体，携带文件名、相对路径以及
 * （可选）缩略图路径，供业务层落库（t_file 表）与返回前端预览地址使用。</p>
 *
 * <p><b>数据流</b>：上传文件 → 存储服务生成路径 → 组装 StorePath
 * → FileService 记录到数据库（文件名/路径）→ 前端通过路径拼接访问地址。</p>
 *
 * @author javahuang
 * @date 2021/9/8
 */
@Data
public class StorePath {

	/**
	 * 存储后的文件名（含扩展名）。
	 */
	private String fileName;

	/**
	 * 文件的相对存储路径（可拼接访问地址）。
	 */
	private String filePath;

	/**
	 * 缩略图的相对存储路径（图片类文件生成，可为空）。
	 */
	private String thumbFilePath;

	/**
	 * 仅文件名构造器。
	 *
	 * @param fileName 文件名
	 */
	public StorePath(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * 文件名 + 相对路径构造器。
	 *
	 * @param fileName 文件名
	 * @param filePath 相对路径
	 */
	public StorePath(String fileName, String filePath) {
		this.fileName = fileName;
		this.filePath = filePath;
	}

	/**
	 * 完整构造器（含缩略图路径）。
	 *
	 * @param fileName      文件名
	 * @param filePath      相对路径
	 * @param thumbFilePath 缩略图相对路径
	 */
	public StorePath(String fileName, String filePath, String thumbFilePath) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.thumbFilePath = thumbFilePath;
	}

}
