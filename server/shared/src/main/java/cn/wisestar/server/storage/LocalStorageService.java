package cn.wisestar.server.storage;

import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.exception.ErrorCodeException;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * 本地磁盘存储服务（LocalStorageService）。
 *
 * <p><b>所属模块</b>：shared 模块存储抽象包（cn.wisestar.server.storage）。</p>
 * <p><b>类职责</b>：{@link StorageService} 的本地文件系统实现：
 * 把上传文件保存到配置的根目录（{@code file-storage.local.root-path}），
 * 支持文件上传、字节读取与流式读取。同时具备缩略图能力（继承
 * {@link AbstractStorageService}）。</p>
 *
 * <p><b>安全设计</b>：{@link #resolvePath} 对目标路径做 normalize 归一化并校验
 * 必须位于根目录内（startsWith 检查），防止通过 ../ 等路径穿越写出根目录
 * （路径穿越防护）。</p>
 *
 * <p><b>实例化</b>：由 {@link StorageAutoConfiguration} 在配置了
 * {@code file-storage.local.root-path} 时创建（@ConditionalOnProperty）。</p>
 *
 * <p><b>异常约定</b>：上传失败抛 {@link ErrorCode#FileUploadError}（4041）、
 * 读取失败抛 {@link ErrorCode#FileNotExists}（4040），由全局异常处理器转成 JSON 响应。</p>
 *
 * @author javahuang
 * @date 2021/9/6
 */
public class LocalStorageService extends AbstractStorageService {

	/**
	 * 存储根目录（由配置 rootPath 解析得到，构造时创建）。
	 */
	private Path rootLocation;

	/**
	 * 构造本地存储服务（触发父类初始化流程）。
	 *
	 * @param configuration 存储配置（含 local.rootPath 等）
	 */
	public LocalStorageService(StorageProperties configuration) {
		super(configuration);
	}

	/**
	 * 初始化：创建存储根目录（目录已存在时无副作用）。
	 */
	@Override
	public void init() {
		try {
			this.rootLocation = Paths.get(getStorageConfig().getLocal().getRootPath());
			Files.createDirectories(rootLocation);
		}
		catch (UnknownHostException e) {
			e.printStackTrace();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 上传文件到指定相对路径（父目录自动创建，已存在文件覆盖）。
	 *
	 * @param file 文件内容输入流（调用方负责关闭，本方法使用 try-with-resources）
	 * @param path 目标相对路径（相对存储根目录）
	 */
	@Override
	public void uploadFile(InputStream file, String path) {
		Path destinationFile = resolvePath(path);

		try (InputStream inputStream = file) {
			Files.createDirectories(destinationFile.getParent());
			Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ErrorCodeException(ErrorCode.FileUploadError);
		}
	}

	/**
	 * 解析并校验目标路径：归一化后必须位于存储根目录内。
	 *
	 * <p>先把相对路径 normalize（消除 ../ 等冗余），再 resolve 到根目录下，
	 * 最后校验解析结果以根目录开头，防止路径穿越写入根目录之外。</p>
	 *
	 * @param path 相对路径
	 * @return 根目录下的绝对路径
	 */
	private Path resolvePath(String path) {
		Path normalizedPath = Paths.get(path).normalize();
		Path destinationPath = this.rootLocation.resolve(normalizedPath);

		// Ensure the destination path is within the root location
		if (!destinationPath.startsWith(this.rootLocation)) {
			throw new ErrorCodeException(ErrorCode.FileUploadError);
		}

		return destinationPath;
	}

	/**
	 * 按相对路径读取文件全部字节（下载）。
	 *
	 * @param filePath 相对路径（经过路径穿越校验）
	 * @return 文件内容字节数组
	 */
	@Override
	public byte[] download(String filePath) {
		Path resolvedPath = resolvePath(filePath);

		try {
			return Files.readAllBytes(resolvedPath);
		} catch (IOException e) {
			e.printStackTrace();
			throw new ErrorCodeException(ErrorCode.FileNotExists);
		}
	}

	/**
	 * 按相对路径以输入流方式读取文件（适用于流式响应/大文件）。
	 *
	 * <p>注意：本方法直接 resolve 根目录，未做路径穿越校验（与 download 不同），
	 * 调用方需确保传入的是服务端生成的受控路径。</p>
	 *
	 * @param filePath 相对路径
	 * @return 文件输入流
	 */
	@Override
	public InputStream downloadAsStream(String filePath) {
		try {
			return Files.newInputStream(rootLocation.resolve(filePath));
		}
		catch (IOException e) {
			throw new ErrorCodeException(ErrorCode.FileNotExists);
		}
	}

}
