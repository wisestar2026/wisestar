package cn.wisestar.server.api;

import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.domain.dto.*;
import cn.wisestar.server.service.FileService;
import cn.wisestar.server.service.SurveyService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;

/**
 * 答卷页面接口（SurveyApi）。
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供"公开答卷页"所需的全部接口：加载问卷、问卷校验、投票统计、
 * 答案保存/暂存、公开文件上传/预览、公开查询（验证码校验）、问卷字典加载、
 * 问卷结果/关联结果加载。这些接口均为公开访问（不要求登录）。</p>
 * <p><b>请求路径前缀</b>：类级路径为 {@code ${api.prefix}/public}（api.prefix 通常为 /api），
 * 各方法再追加子路径（如 /api/public/loadProject、/api/public/saveAnswer 等）。</p>
 * <p><b>被谁调用</b>：前端答题端（答卷页/公开查询页/投票页），面向答卷人开放，不要求登录。</p>
 * <p><b>依赖的服务</b>：</p>
 * <ul>
 *   <li>{@link SurveyService}——问卷加载、校验、统计、答案保存、公开查询等核心业务；</li>
 *   <li>{@link FileService}——公开上传（mark publicUpload=true）与文件预览加载。</li>
 * </ul>
 *
 * <p><b>数据流概览</b>：答卷人浏览器 → 本类各方法 → SurveyService（shared 接口）
 * → rdbms 实现 → MyBatis Mapper → 问卷/答案/字典表 → 视图 DTO 返回。</p>
 *
 * @author javahuang
 * @date 2021/8/22
 */
@RequestMapping("${api.prefix}/public")
@RequiredArgsConstructor
@RestController
public class SurveyApi {

	/**
	 * 问卷服务（业务层入口，构造器注入）。
	 */
	private final SurveyService surveyService;

	/**
	 * 文件服务（公开上传/预览用，构造器注入）。
	 */
	private final FileService fileService;

	/**
	 * 加载问卷。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/loadProject
	 * （如 /api/public/loadProject）。</p>
	 *
	 * <p><b>功能</b>：按项目短链接/校验码加载一份问卷的完整渲染数据（题目、选项、
	 * 样式、题目随机配置等），答卷页打开问卷时调用。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（@RequestBody JSON）——问卷定位条件
	 * （shortId / checkCode / id 等，详见 DTO）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicProjectView}（公开问卷视图：问卷基本信息 +
	 * 题目 schema + 样式配置）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#loadProject(ProjectQuery)}。</p>
	 *
	 * @param query 问卷定位查询参数
	 * @return 公开问卷视图数据
	 */
	@PostMapping("/loadProject")
	public PublicProjectView loadProject(@RequestBody ProjectQuery query) {
		return surveyService.loadProject(query);
	}

	/**
	 * 问卷校验。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/validateProject
	 * （如 /api/public/validateProject）。</p>
	 *
	 * <p><b>功能</b>：校验问卷是否可访问/可填写（问卷状态、有效期、填答次数限制、
	 * 白名单等），返回校验后的问卷视图；校验不通过时由服务层抛异常。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（@RequestBody JSON，问卷定位条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicProjectView}（校验通过的问卷视图）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#validateProject(ProjectQuery)}。</p>
	 *
	 * @param query 问卷定位查询参数
	 * @return 校验通过的问卷视图数据
	 */
	@PostMapping("/validateProject")
	public PublicProjectView validateProject(@RequestBody ProjectQuery query) {
		return surveyService.validateProject(query);
	}

	/**
	 * 单选、多选投票获取统计信息。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/statistics
	 * （如 /api/public/statistics）。</p>
	 *
	 * <p><b>功能</b>：针对问卷中的单选题/多选题（投票题）返回实时统计结果
	 * （每个选项的投票数/占比），供投票类问卷的结果页展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link ProjectQuery}（@RequestBody JSON，问卷定位条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicStatisticsView}（投票统计视图：题目 + 各选项计数）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#statProject(ProjectQuery)}。</p>
	 *
	 * @param query 问卷定位查询参数
	 * @return 投票统计信息
	 */
	@PostMapping("/statistics")
	public PublicStatisticsView statProject(@RequestBody ProjectQuery query) {
		return surveyService.statProject(query);
	}

	/**
	 * 答案保存。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/saveAnswer
	 * （如 /api/public/saveAnswer）。</p>
	 *
	 * <p><b>功能</b>：答卷人提交问卷答案，服务层落库并返回答卷结果视图
	 * （含提交成功标志、答卷编号、可查询校验码等，便于"公开查询"功能后续使用）。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON）——答卷数据：
	 * 问卷定位（projectId/shortId）、答卷人信息、各题目答案明细等。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicAnswerView}（答卷结果视图：答卷 id、查询码、
	 * 提交时间等）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#saveAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 答卷保存请求
	 * @return 答卷结果视图
	 */
	@PostMapping("/saveAnswer")
	public PublicAnswerView saveAnswer(@RequestBody AnswerRequest request) {
		PublicAnswerView publicAnswerView = surveyService.saveAnswer(request);
		return publicAnswerView;
	}

	/**
	 * 答案暂存（目前仅支持问题随机）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/tempSaveAnswer
	 * （如 /api/public/tempSaveAnswer）。</p>
	 *
	 * <p><b>功能</b>：答卷过程中的草稿暂存——当问卷开启"题目随机"时，暂存已答内容，
	 * 防止刷新/断线丢失；正式提交仍走 saveAnswer。</p>
	 *
	 * <p><b>请求参数</b>：{@link AnswerRequest}（@RequestBody JSON，同 saveAnswer）。</p>
	 *
	 * <p><b>返回值结构</b>：无返回值（HTTP 200 空响应体）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#tempSaveAnswer(AnswerRequest)}。</p>
	 *
	 * @param request 暂存答卷请求
	 */
	@PostMapping("/tempSaveAnswer")
	public void tempSaveAnswer(@RequestBody AnswerRequest request) {
		surveyService.tempSaveAnswer(request);
	}

	/**
	 * 上传文件（公开上传）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/upload（如 /api/public/upload）。</p>
	 *
	 * <p><b>功能</b>：答卷页（或公开场景）上传附件（如问卷附件题上传文件）。
	 * 关键点：先把 publicUpload 标记置为 true（表示公开上传、无需登录），再调用 FileService.upload。</p>
	 *
	 * <p><b>请求参数</b>：{@link UploadFileRequest}（multipart 表单绑定，含文件流与业务关联信息）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link FileView}（上传成功后文件元数据视图）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link FileService#upload(UploadFileRequest)}。</p>
	 *
	 * @param request 上传文件请求（multipart）
	 * @return 文件元数据视图
	 */
	@PostMapping("/upload")
	public FileView upload(UploadFileRequest request) {
		request.setPublicUpload(true);
		return fileService.upload(request);
	}

	/**
	 * 预览文件。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/public/preview/{attachmentId}
	 * （如 /api/public/preview/xxx）。</p>
	 *
	 * <p><b>功能</b>：按附件 id 内联预览文件（图片直接展示）。设置 30 天浏览器缓存头
	 * （Cache-Control: max-age=2592000）提升重复查看性能；dispositionType 设为 inline
	 * 使浏览器内联渲染而非下载。</p>
	 *
	 * <p><b>请求参数</b>：attachmentId（@PathVariable 路径变量，附件/文件 id）。</p>
	 *
	 * <p><b>返回值结构</b>：ResponseEntity&lt;Resource&gt;（文件字节流 + 缓存头 + 内联 Content-Disposition）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：先组装 {@link FileQuery}（id + 内联类型 + 自定义头），
	 * 再调用 {@link FileService#loadFile(FileQuery)}。</p>
	 *
	 * @param attachmentId 附件 id（路径变量）
	 * @return 文件内容响应（可缓存的字节流）
	 */
	@GetMapping("/preview/{attachmentId}")
	public ResponseEntity<Resource> preview(@PathVariable("attachmentId") String attachmentId) {
		FileQuery query = new FileQuery(attachmentId);
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CACHE_CONTROL, CacheControl.maxAge(Duration.ofDays(30)).getHeaderValue());
		query.setDispositionType(AppConsts.DispositionTypeEnum.inline);
		query.setHeaders(headers);
		return fileService.loadFile(query);
	}

	/**
	 * 加载公开查询验证页面数据。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/loadQuery
	 * （如 /api/public/loadQuery）。</p>
	 *
	 * <p><b>功能</b>：加载"公开查询"功能所需的数据（如查询验证方式、验证码配置、
	 * 问卷信息摘要），供查询验证页初始化渲染。</p>
	 *
	 * <p><b>请求参数</b>：{@link PublicQueryRequest}（@RequestBody JSON，问卷定位条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicQueryVerifyView}（公开查询验证页视图数据）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#loadQuery(PublicQueryRequest)}。</p>
	 *
	 * @param request 公开查询请求（问卷定位）
	 * @return 公开查询验证页面视图数据
	 */
	@PostMapping("/loadQuery")
	public PublicQueryVerifyView loadQuery(@RequestBody PublicQueryRequest request) {
		return surveyService.loadQuery(request);
	}

	/**
	 * 获取公开查询结果。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/getQueryResult
	 * （如 /api/public/getQueryResult）。</p>
	 *
	 * <p><b>功能</b>：答卷人通过问卷编码/查询码（+ 可能的验证码）查询自己的答卷结果，
	 * 返回结果数据（如分数、各题答案等，取决于问卷配置是否开放查询）。</p>
	 *
	 * <p><b>请求参数</b>：{@link PublicQueryRequest}（@RequestBody JSON，含查询码/验证码）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicQueryView}（答卷查询结果视图）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#getQueryResult(PublicQueryRequest)}。</p>
	 *
	 * @param request 公开查询请求
	 * @return 查询结果视图
	 */
	@PostMapping("/getQueryResult")
	public PublicQueryView getQueryResult(@RequestBody PublicQueryRequest request) {
		return surveyService.getQueryResult(request);
	}

	/**
	 * 问卷加载字典。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/loadDict
	 * （如 /api/public/loadDict）。</p>
	 *
	 * <p><b>功能</b>：加载问卷题目中引用的字典数据（题目依赖的字典选项列表），
	 * 供答卷页渲染下拉/级联类题目。</p>
	 *
	 * <p><b>请求参数</b>：{@link PublicDictRequest}（@RequestBody JSON，问卷/题目关联条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<PublicDictView>}（公开字典视图列表）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#loadDict(PublicDictRequest)}。</p>
	 *
	 * @param request 字典加载请求
	 * @return 公开字典视图列表
	 */
	@PostMapping("/loadDict")
	public List<PublicDictView> loadDict(@RequestBody PublicDictRequest request) {
		return surveyService.loadDict(request);
	}

	/**
	 * 加载问卷结果。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/loadExamResult
	 * （如 /api/public/loadExamResult）。</p>
	 *
	 * <p><b>功能</b>：加载考试/练习类问卷的作答结果（分数、正确/错误明细、解析等），
	 * 供答卷人查看考试结果页。</p>
	 *
	 * <p><b>请求参数</b>：{@link PublicExamRequest}（@RequestBody JSON，考试/答卷定位条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicExamResult}（考试结果视图）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#loadExamResult(PublicExamRequest)}。</p>
	 *
	 * @param request 考试结果加载请求
	 * @return 考试结果视图
	 */
	@PostMapping("/loadExamResult")
	public PublicExamResult loadExamResult(@RequestBody PublicExamRequest request) {
		return surveyService.loadExamResult(request);
	}

	/**
	 * 加载问卷关联结果。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/public/loadLinkResult
	 * （如 /api/public/loadLinkResult）。</p>
	 *
	 * <p><b>功能</b>：加载问卷的"关联结果"——问卷提交后根据答案联动展示的结果内容
	 * （如跳转问卷、动态展示文案），供答卷提交后展示。</p>
	 *
	 * <p><b>请求参数</b>：{@link PublicLinkRequest}（@RequestBody JSON，关联结果定位条件）。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PublicLinkResult}（关联结果视图）。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link SurveyService#loadLinkResult(PublicLinkRequest)}。</p>
	 *
	 * @param request 关联结果加载请求
	 * @return 关联结果视图
	 */
	@PostMapping("/loadLinkResult")
	public PublicLinkResult loadLinkResult(@RequestBody PublicLinkRequest request) {
		return surveyService.loadLinkResult(request);
	}

}
