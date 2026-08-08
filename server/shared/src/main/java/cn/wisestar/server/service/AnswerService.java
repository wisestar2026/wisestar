package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.core.uitls.ContextHelper;
import cn.wisestar.server.core.uitls.HTTPUtils;
import cn.wisestar.server.core.uitls.IPUtils;
import cn.wisestar.server.domain.dto.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 答卷服务接口（AnswerService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：提供答卷（Answer）的管理与回收能力：答卷分页、详情、
 * 计数、保存（提交/编辑）、删除、回收站（已删除列表/批量销毁/恢复）、
 * 附件/答卷 Excel 下载、答卷上传、历史练习查询。实现类位于 rdbms 模块
 * （AnswerServiceImpl）。</p>
 *
 * <p><b>内置 default 方法</b>：</p>
 * <ul>
 *   <li>{@link #parseClientInfo}：从当前请求解析答卷人客户端信息（User-Agent、
 *       IP、限制 Cookie），用于答卷元数据记录与作答频率限制；</li>
 *   <li>{@link #download}：按下载类型分发到 downloadSurvey（答卷 Excel）或
 *       downloadAttachment（附件），统一拼装 Content-Disposition 响应头。</li>
 * </ul>
 *
 * @author javahuang
 * @date 2021/8/3
 */
public interface AnswerService {

	/**
	 * 分页查询答卷列表。
	 *
	 * @param filter 分页 + 筛选条件（项目、状态、提交时间等，见 {@link AnswerQuery}）
	 * @return 答卷分页列表
	 */
	PaginationResponse<AnswerView> listAnswer(AnswerQuery filter);

	/**
	 * 获取单份答卷详情。
	 *
	 * @param query 查询条件（含答卷 id）
	 * @return 答卷视图（含答案明细）
	 */
	AnswerView getAnswer(AnswerQuery query);

	/**
	 * 统计答卷数量（按条件）。
	 *
	 * @param query 统计条件
	 * @return 答卷数量
	 */
	long count(AnswerQuery query);

	/**
	 * 保存（提交）答卷。
	 *
	 * @param answer 答卷请求（含答案明细、客户端信息等）
	 * @return 保存后的答卷视图
	 */
	AnswerView saveAnswer(AnswerRequest answer);

	/**
	 * 更新答卷（允许修改场景）。
	 *
	 * @param answer 答卷更新请求
	 * @return 更新后的答卷视图
	 */
	AnswerView updateAnswer(AnswerRequest answer);

	/**
	 * 删除答卷（逻辑删除，移入回收站）。
	 *
	 * @param request 删除请求（含 id）
	 */
	void deleteAnswer(AnswerRequest request);

	/**
	 * 下载答卷附件（打包下载）。
	 *
	 * @param query 下载条件（含答卷/项目范围，见 {@link DownloadQuery}）
	 * @return 附件打包下载数据（文件名、媒体类型、资源流）
	 */
	DownloadData downloadAttachment(DownloadQuery query);

	/**
	 * 下载答卷数据（Excel 导出）。
	 *
	 * @param query 下载条件
	 * @return 答卷 Excel 下载数据
	 */
	DownloadData downloadSurvey(DownloadQuery query);

	/**
	 * 解析答卷人客户端信息（默认实现）。
	 *
	 * <p>从当前 HTTP 请求中提取 User-Agent、客户端 IP 与限制 Cookie
	 * （{@link AppConsts#COOKIE_LIMIT_NAME}），组装为
	 * {@link AnswerMetaInfo.ClientInfo}。用于答卷元数据记录、作答频率限制
	 * 与防作弊统计。</p>
	 *
	 * @param clientInfo 已有的客户端信息（可空，空则新建）
	 * @return 填充完成（Agent/IP/Cookie）的客户端信息
	 */
	default AnswerMetaInfo.ClientInfo parseClientInfo(AnswerMetaInfo.ClientInfo clientInfo) {
		HttpServletRequest request = ContextHelper.getCurrentHttpRequest();
		if (clientInfo == null) {
			clientInfo = new AnswerMetaInfo.ClientInfo();
		}
		String userAgentStr = request.getHeader("User-Agent");
		clientInfo.setAgent(userAgentStr);
		clientInfo.setRemoteIp(IPUtils.getClientIpAddress(request));
		Cookie limitCookie = WebUtils.getCookie(request, AppConsts.COOKIE_LIMIT_NAME);
		if (limitCookie != null) {
			clientInfo.setCookie(limitCookie.getValue());
		}
		return clientInfo;
	}

	/**
	 * 通用下载入口（默认实现）：按类型分发并组装下载响应。
	 *
	 * <p><b>分发逻辑</b>：</p>
	 * <ul>
	 *   <li>DownloadType.answer → downloadSurvey（问卷答案 Excel）；</li>
	 *   <li>DownloadType.answerAttachment / attachment → downloadAttachment（附件包）；</li>
	 *   <li>其他类型 → 抛 {@link InternalServerError}（"未知下载类型"）。</li>
	 * </ul>
	 * <p>当请求携带 id 列表时，按 id 数量覆盖 pageSize 以支持"选中项下载"。</p>
	 *
	 * @param query 下载条件（含类型与范围）
	 * @return HTTP 文件下载响应（含 Content-Disposition 文件名头）
	 */
	default ResponseEntity<Resource> download(DownloadQuery query) {
		DownloadData download;
		if (!CollectionUtils.isEmpty(query.getIds())) {
			query.setPageSize(query.getIds().size());
		}
		// 下载问卷答案
		if (query.getType() == DownloadQuery.DownloadType.answer) {
			download = downloadSurvey(query);
		}
		// 下载附件
		else if (query.getType() == DownloadQuery.DownloadType.answerAttachment
				|| query.getType() == DownloadQuery.DownloadType.attachment) {
			download = downloadAttachment(query);
		}
		else {
			throw new InternalServerError("未知下载类型");
		}
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, HTTPUtils.getContentDispositionValue(download.getFileName()))
				.contentType(download.getMediaType()).body(download.getResource());
	}

	/**
	 * 查询回收站中的已删除答卷。
	 *
	 * @param query 查询条件
	 * @return 已删除答卷列表
	 */
	List<AnswerView> listAnswerDeleted(AnswerQuery query);

	/**
	 * 批量彻底销毁答卷。
	 *
	 * @param request 请求（含 id 列表）
	 */
	void batchDestroyAnswer(AnswerRequest request);

	/**
	 * 恢复回收站中的答卷。
	 *
	 * @param request 请求（含 id）
	 */
	void restoreAnswer(AnswerRequest request);

	/**
	 * 上传答卷附件（答题过程中的附件上传）。
	 *
	 * @param request 上传请求（见 {@link AnswerUploadRequest}）
	 * @return 上传结果视图（含文件路径）
	 */
	AnswerUploadView upload(AnswerUploadRequest request);

	/**
	 * 分页查询历史练习记录（考试练习场景）。
	 *
	 * @param query 分页 + 筛选条件（见 {@link HistoryExerciseQuery}）
	 * @return 练习记录分页列表（ExerciseView）
	 */
	PaginationResponse<ExerciseView> historyExercise(HistoryExerciseQuery query);
}
