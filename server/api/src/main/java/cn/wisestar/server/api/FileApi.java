package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.FileQuery;
import cn.wisestar.server.domain.dto.FileView;
import cn.wisestar.server.domain.dto.UploadFileRequest;
import cn.wisestar.server.service.FileService;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文件管理接口（FileApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供系统内文件（图片、附件、Excel 导入模板等）的上传、下载、
 * 列表查询与删除能力，供管理后台"文件管理"页面以及问卷/答案附件场景使用。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/file}（api.prefix 通常为 /api），
 * 各方法在类级路径上追加子路径。</p>
 * <p><b>被谁调用</b>：前端管理后台文件管理页、问卷编辑器（上传问卷封面/附件）、
 * 公开答卷页（经 SurveyApi 转发到 fileService.upload）。</p>
 * <p><b>依赖的服务</b>：注入 {@link FileService}（shared 模块接口，rdbms 模块实现），
 * 其内部负责文件落盘（本地/OSS 等存储策略）与文件元数据表的读写。</p>
 *
 * <p><b>完整数据流（以 getFile 为例）</b>：</p>
 * <pre>
 *   前端 HTTP GET /api/file?id=xxx（FileQuery 绑定查询参数）
 *     --&gt; FileApi#getFile(FileQuery)                 （本类）
 *     --&gt; FileService#loadFile(FileQuery)            （shared 接口）
 *     --&gt; FileServiceImpl#loadFile(...)              （rdbms 实现）
 *     --&gt; 文件 Mapper 查询元数据 + 从存储读取二进制流
 *     --&gt; ResponseEntity&lt;Resource&gt; 以字节流响应给前端
 * </pre>
 *
 * @author javahuang
 * @date 2021/9/10
 */
@RestController
@RequestMapping("${api.prefix}/file")
@AllArgsConstructor
public class FileApi {

	/**
	 * 文件服务（业务层入口）。
	 * <p>由 Lombok @AllArgsConstructor 生成全参构造器注入（final 字段），
	 * 实际注入 shared 模块 {@link FileService} 接口的 rdbms 实现。</p>
	 */
	private final FileService fileService;

	/**
	 * 获取文件（按文件 id / 条件下载单个文件内容）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/file（如 /api/file?id=xxx）。</p>
	 *
	 * <p><b>功能</b>：根据 {@link FileQuery} 中的条件定位一个文件，将其内容以
	 * {@link ResponseEntity}&lt;Resource&gt; 字节流形式返回；支持按 dispositionType
	 * 控制是内联预览（inline）还是附件下载（attachment），可携带自定义响应头
	 * （如 SurveyApi#preview 中设置 30 天缓存头）。</p>
	 *
	 * <p><b>请求参数</b>：FileQuery（GET 查询参数绑定）——包含文件 id（attachmentId）、
	 * 文件名、目标路径、dispositionType 等筛选/控制字段，详情见 shared 模块 FileQuery DTO。</p>
	 *
	 * <p><b>返回值结构</b>：HTTP 200 + 文件二进制流（Resource），响应头含 Content-Disposition、
	 * 缓存头等；文件不存在时由服务层返回 404 响应。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link FileService#loadFile(FileQuery)}。</p>
	 *
	 * <p><b>数据流</b>：GET 请求 → 本方法 → fileService.loadFile(query)
	 * → 查询文件元数据 → 读取存储二进制 → ResponseEntity&lt;Resource&gt; 返回。</p>
	 *
	 * @param query 文件查询/下载控制参数（GET 参数绑定）
	 * @return 文件内容响应（Resource 字节流 + 响应头）
	 */
	@GetMapping
	public ResponseEntity<Resource> getFile(FileQuery query) {
		return fileService.loadFile(query);
	}

	/**
	 * 获取文件列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/file/list（如 /api/file/list）。</p>
	 *
	 * <p><b>功能</b>：按 {@link FileQuery} 中的条件（如所属项目、关联业务 id、文件类型等）
	 * 分页/筛选查询文件元数据列表，供前端文件管理列表展示。</p>
	 *
	 * <p><b>请求参数</b>：FileQuery（GET 查询参数绑定，筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<FileView>}，FileView 为文件元数据视图
	 * （文件名、路径、大小、上传时间、上传人等，详见 shared 模块 DTO）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link FileService#listFiles(FileQuery)}。</p>
	 *
	 * @param query 文件列表筛选条件
	 * @return 文件元数据视图列表
	 */
	@GetMapping("/list")
	public List<FileView> listFiles(FileQuery query) {
		return fileService.listFiles(query);
	}

	/**
	 * 添加文件（上传文件）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/file/create（如 /api/file/create）。</p>
	 *
	 * <p><b>功能</b>：接收 multipart/form-data 上传的文件（UploadFileRequest 为
	 * 表单字段绑定，含文件流、所属业务 id、文件类型等），将文件落盘存储并写入文件元数据表。</p>
	 *
	 * <p><b>请求参数</b>：UploadFileRequest（multipart 表单绑定，非 JSON body）——
	 * 含 file（MultipartFile 文件流）、projectId（所属项目）、业务类型等字段。</p>
	 *
	 * <p><b>返回值结构</b>：FileView（新文件的元数据视图，含文件 id 供后续引用）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link FileService#upload(UploadFileRequest)}。</p>
	 *
	 * @param request 上传文件请求（multipart 表单）
	 * @return 上传成功后文件的元数据视图（含文件 id）
	 */
	@PostMapping("/create")
	public FileView upload(UploadFileRequest request) {
		return fileService.upload(request);
	}

	/**
	 * 删除文件。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/file/delete（如 /api/file/delete）。</p>
	 *
	 * <p><b>功能</b>：按文件 id 删除文件——移除存储中的二进制文件并删除（或标记删除）
	 * 文件元数据记录。请求体为 JSON。</p>
	 *
	 * <p><b>请求参数</b>：UploadFileRequest（@RequestBody JSON）——其中 id 字段标识要删除的文件。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：先取 {@code request.getId()}，再调用
	 * {@link FileService#deleteFile(String)}（注意：本方法在 Controller 层只透传 id）。</p>
	 *
	 * @param request 删除请求（JSON，含文件 id）
	 */
	@PostMapping("/delete")
	public void deleteImage(@RequestBody UploadFileRequest request) {
		fileService.deleteFile(request.getId());
	}

	/**
	 * 下载导入 Excel 模板。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/file/downloadTemplate（如
	 * /api/file/downloadTemplate?name=xxx）。</p>
	 *
	 * <p><b>功能</b>：下载系统预置的 Excel 导入模板（如用户导入、题目导入模板），
	 * 供管理员下载后按模板填写数据再批量导入。</p>
	 *
	 * <p><b>请求参数</b>：name（GET 查询参数，导入模板名称，用于定位 classpath 下的模板文件）。</p>
	 *
	 * <p><b>返回值结构</b>：ResponseEntity&lt;Resource&gt;（Excel 模板文件字节流，附件下载）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link FileService#downloadTemplate(String)}。</p>
	 *
	 * @param name 导入模板名称
	 * @return 模板文件内容响应（Resource 字节流）
	 */
	@GetMapping("/downloadTemplate")
	public ResponseEntity<Resource> downloadTemplate(String name) {
		return fileService.downloadTemplate(name);
	}

}
