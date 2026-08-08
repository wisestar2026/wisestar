package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.AnswerService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Locale;

/**
 * 答卷（答案）管理接口（AnswerApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供答卷数据的管理接口：答案列表/详情、回收站（删除/恢复/彻底删除）、
 * 数据表格保存答案、答案更新、答案导出（Excel）、答案附件上传。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/answer}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/answer/list、/api/answer/download 等）。</p>
 * <p><b>被谁调用</b>：前端管理后台：答卷数据管理页（表格展示/导出）、答卷详情页、
 * 附件上传场景。所有接口要求登录并校验对应权限点。</p>
 * <p><b>依赖的服务</b>：注入 {@link AnswerService}（shared 模块接口，rdbms 模块实现）——
 * 负责答卷的 CRUD、回收站、导出（fastexcel 写 Excel）与附件上传业务。</p>
 *
 * <p><b>数据流概览</b>：前端 HTTP 请求 → 本类方法（权限注解 + 参数校验）→
 * AnswerService（shared 接口）→ rdbms 实现 → 答卷 Mapper → 数据库 → 视图 DTO / Excel 响应流。</p>
 *
 * @author javahuang
 * @date 2021/8/6
 */
@RestController
@RequestMapping("${api.prefix}/answer")
@RequiredArgsConstructor
public class AnswerApi {

	/**
	 * 答卷服务（业务层入口，构造器注入）。
	 */
	private final AnswerService answerService;

	/**
	 * 获取答案列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/answer/list（如 /api/answer/list）。</p>
	 *
	 * <p><b>功能</b>：分页查询答卷列表（按项目、时间、答卷人等条件筛选），
	 * 供答卷数据管理页表格展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerQuery}（GET 查询参数）——分页 + 筛选条件。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}&lt;AnswerView&gt;（分页包装的答卷列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#listAnswer(AnswerQuery)}。</p>
	 *
	 * @param query 查询参数
	 * @return 答卷分页列表
	 */
	@PreAuthorize("hasAuthority('answer:list')")
	@GetMapping("/list")
	public PaginationResponse<AnswerView> listAnswer(AnswerQuery query) {
		return answerService.listAnswer(query);
	}

	/**
	 * 获取删除的答案（回收站列表）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/answer/trash（如 /api/answer/trash）。</p>
	 *
	 * <p><b>功能</b>：查询已删除（回收站中）的答卷列表，供答卷回收站页面展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerQuery}（GET 查询参数，筛选条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<AnswerView>}（已删除答卷列表）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:list')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#listAnswerDeleted(AnswerQuery)}。</p>
	 *
	 * @param query 查询参数
	 * @return 回收站答卷列表
	 */
	@PreAuthorize("hasAuthority('answer:list')")
	@GetMapping("/trash")
	public List<AnswerView> listAnswerDeleted(AnswerQuery query) {
		return answerService.listAnswerDeleted(query);
	}

	/**
	 * 获取答案详情。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/answer?id=xxx（如 /api/answer?id=xxx）。</p>
	 *
	 * <p><b>功能</b>：按查询条件获取单份答卷的详情（含各题答案明细），供答卷详情页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerQuery}（GET 查询参数，含答卷 id）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link AnswerView}（答卷详情视图）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:detail')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#getAnswer(AnswerQuery)}。</p>
	 *
	 * @param query 查询参数（含答卷 id）
	 * @return 答卷详情视图
	 */
	@GetMapping
	@PreAuthorize("hasAuthority('answer:detail')")
	public AnswerView getAnswer(AnswerQuery query) {
		return answerService.getAnswer(query);
	}

	/**
	 * 数据表格保存答案。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/create（如 /api/answer/create）。</p>
	 *
	 * <p><b>功能</b>：在答卷数据管理页中手动新增一条答卷记录（如人工录入/补录答卷）。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON）——答卷数据
	 * （项目 id、答卷人、各题答案等）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:create')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#saveAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 保存的答案数据
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('answer:create')")
	public void saveAnswer(@RequestBody AnswerRequest request) {
		answerService.saveAnswer(request);
	}

	/**
	 * 更新答案。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/update（如 /api/answer/update）。</p>
	 *
	 * <p><b>功能</b>：更新答卷记录（修改答卷人信息、答案明细、状态等）。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON，含答卷 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#updateAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 更新的答案数据
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('answer:update')")
	public void updateAnswer(@RequestBody AnswerRequest request) {
		answerService.updateAnswer(request);
	}

	/**
	 * 删除答案（答案放到回收站）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/delete（如 /api/answer/delete）。</p>
	 *
	 * <p><b>功能</b>：将答卷逻辑删除（标记删除状态，进入回收站，可恢复）。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON，含答卷 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#deleteAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 请求参数（含答卷 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('answer:delete')")
	public void deleteAnswer(@RequestBody AnswerRequest request) {
		answerService.deleteAnswer(request);
	}

	/**
	 * 从回收站里面清空答案（彻底删除）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/destroy（如 /api/answer/destroy）。</p>
	 *
	 * <p><b>功能</b>：将回收站中的答卷物理删除（支持批量，不可恢复）。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON，id 支持逗号分隔批量）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:delete')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#batchDestroyAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 请求参数（含待清空答卷 id）
	 */
	@PreAuthorize("hasAuthority('answer:delete')")
	@PostMapping("/destroy")
	public void batchDestroyAnswer(@RequestBody AnswerRequest request) {
		answerService.batchDestroyAnswer(request);
	}

	/**
	 * 从回收站里面恢复答案。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/restore（如 /api/answer/restore）。</p>
	 *
	 * <p><b>功能</b>：将回收站中的答卷恢复为正常状态。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON，含答卷 id）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:update')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#restoreAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 请求参数（含待恢复答卷 id）
	 */
	@PreAuthorize("hasAuthority('answer:update')")
	@PostMapping("/restore")
	public void restoreAnswer(@RequestBody AnswerRequest request) {
		answerService.restoreAnswer(request);
	}

	/**
	 * 答案导出。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/answer/download（如 /api/answer/download）。</p>
	 *
	 * <p><b>功能</b>：将符合条件的答卷数据导出为 Excel 文件。特殊处理——支持按请求参数
	 * locale 切换导出文件的国际化文案（如表头语言），导出前临时设置 LocaleContextHolder，
	 * 导出完成后在 finally 中恢复原 Locale，避免影响其他请求线程的国际化上下文。</p>
	 *
	 * <p><b>请求参数</b>：{@link DownloadQuery}（GET 查询参数）——导出筛选条件 + locale
	 * （语言标识，如下划线分隔的 zh_CN，方法内会规范为连字符语言标签 zh-CN）。</p>
	 *
	 * <p><b>返回值结构</b>：ResponseEntity&lt;Resource&gt;（Excel 文件字节流，附件下载）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:export')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#download(DownloadQuery)}。</p>
	 *
	 * @param query 答案导出查询请求（含 locale 参数）
	 * @return Excel 文件内容响应
	 */
	@GetMapping("/download")
	@PreAuthorize("hasAuthority('answer:export')")
	public ResponseEntity<Resource> download(DownloadQuery query) {
		Locale previous = LocaleContextHolder.getLocale();
		setLocale(query.getLocale());
		try {
			return answerService.download(query);
		}
		finally {
			LocaleContextHolder.setLocale(previous);
		}
	}

	/**
	 * 根据语言字符串设置 LocaleContextHolder 的 Locale（私有辅助方法）。
	 *
	 * <p><b>功能</b>：将请求参数中的语言标识（如 zh_CN、en_US）解析为 {@link Locale}
	 * 并设置到当前线程的 LocaleContextHolder，供导出过程读取国际化消息。
	 * 实现细节：将下划线 '_' 替换为连字符 '-' 后使用 {@link Locale#forLanguageTag} 解析；
	 * 语言参数为空时不做任何设置（沿用当前 Locale）。</p>
	 *
	 * @param localeStr 语言标识字符串（可为空，如 zh_CN）
	 */
	private void setLocale(String localeStr) {
		if (!StringUtils.hasText(localeStr)) {
			return;
		}
		String normalized = localeStr.replace('_', '-');
		LocaleContextHolder.setLocale(Locale.forLanguageTag(normalized));
	}

	/**
	 * 修改答案上传附件。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/answer/upload（如 /api/answer/upload）。</p>
	 *
	 * <p><b>功能</b>：为答卷上传/更新附件（如答卷中上传的图片、文件），
	 * 返回附件在项目 schema 中的引用信息。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerUploadRequest}（multipart 表单绑定）——附件文件 +
	 * 关联的答卷/题目信息。</p>
	 *
	 * <p><b>返回值结构</b>：{@link AnswerUploadView}（附件上传结果视图，含项目 schema 中附件引用）。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("hasAuthority('answer:upload')")。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnswerService#upload(AnswerUploadRequest)}。</p>
	 *
	 * @param request 附件上传请求
	 * @return 附件上传结果视图
	 */
	@PostMapping("/upload")
	@PreAuthorize("hasAuthority('answer:upload')")
	public AnswerUploadView upload(AnswerUploadRequest request) {
		return answerService.upload(request);
	}

}
