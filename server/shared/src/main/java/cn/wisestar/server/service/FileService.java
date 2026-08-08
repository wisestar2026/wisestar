package cn.wisestar.server.service;

import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.domain.dto.FileQuery;
import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.dto.UploadFileRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

/**
 * 文件服务接口（FileService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供文件（附件/图片）的上传、列表查询、读取（预览/下载）、
 * 删除能力，以及模板文件下载。底层文件存储依赖 {@link cn.wisestar.server.storage.StorageService}，
 * 文件元数据记录在数据库（t_file）。实现类位于 rdbms 模块（FileServiceImpl）。</p>
 *
 * <p><b>调用方</b>：api 模块 FileApi（/api/file/**，GET 读取路径在
 * WebSecurityConfig 中配置为 permitAll，支持公开访问预览）。</p>
 *
 * @author javahuang
 * @date 2021/9/10
 */
public interface FileService {

	/**
	 * 支持的图片扩展名列表（来自 {@link AppConsts#SUPPORT_IMAGE_TYPE}，用于
	 * 判断上传文件是否为支持的图片格式）。
	 */
	List<String> SUPPORT_IMAGE_LIST = Arrays.asList(AppConsts.SUPPORT_IMAGE_TYPE);

	/**
	 * 上传文件（含图片缩略图生成）。
	 *
	 * @param request 上传请求（文件、存储类型、关联项目等，见 {@link UploadFileRequest}）
	 * @return 文件视图（含文件名、访问路径、缩略图路径等）
	 */
	FileView upload(UploadFileRequest request);

	/**
	 * 按条件分页/列表查询文件。
	 *
	 * @param query 查询条件（关联实体、存储类型等，见 {@link FileQuery}）
	 * @return 文件视图列表
	 */
	List<FileView> listFiles(FileQuery query);

	/**
	 * 读取文件内容（预览/下载，流式响应）。
	 *
	 * @param query 查询条件（含文件名/路径）
	 * @return HTTP 文件资源响应
	 */
	ResponseEntity<Resource> loadFile(FileQuery query);

	/**
	 * 删除文件（逻辑删除元数据 + 物理删除存储文件，由实现决定）。
	 *
	 * @param id 文件记录 id
	 */
	void deleteFile(String id);

	/**
	 * 是否支持该格式的图片上传
	 * @param fileName
	 * @return
	 */
	default boolean isSupportImage(String fileName) {
		String extType = fileName.substring(fileName.lastIndexOf(".") + 1);
		return SUPPORT_IMAGE_LIST.contains(extType.toUpperCase());
	}

	/**
	 * 下载模板文件（导入模板等固定资源）。
	 *
	 * @param name 模板文件名
	 * @return HTTP 文件资源响应
	 */
	ResponseEntity<Resource> downloadTemplate(String name);

}
