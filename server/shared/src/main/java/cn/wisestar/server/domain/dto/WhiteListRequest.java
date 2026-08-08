package cn.wisestar.server.domain.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 问卷/考试参与白名单设置请求 DTO。
 *
 * 【类职责】
 * 承载"设置问卷参与白名单"接口（ProjectApi 的 setWhiteList 类接口）的请求参数，
 * 支持两种方式指定白名单人员：手工勾选用户列表（selected）或上传用户文件（file）。
 *
 * 【被谁调用】
 * - 写入方：ProjectApi 白名单设置接口，将本 DTO 转为 ProjectPartner / 白名单记录保存
 * - 来源：前端项目管理页的"白名单设置"弹窗表单
 *
 * 【依赖什么】
 * - MultipartFile：Spring MVC 文件上传对象，仅使用 selected 方式时可为空
 *
 * 【数据流】
 * 前端弹窗（勾选用户或上传文件）→ ProjectApi（HTTP multipart/form-data 或 JSON）
 * → 解析为 WhiteListRequest → Service 层落库（t_project_partner / 白名单相关表）
 *
 * @author javahuang
 * @date 2021/9/23
 */
@Data
public class WhiteListRequest {

	/**
	 * 手工勾选的用户 ID 列表（JSON 数组）。
	 * 与 file 二选一：selected 非空时以勾选名单为准。
	 */
	private List<String> selected = new ArrayList<>();

	/**
	 * 上传的白名单用户文件（Excel/CSV，multipart 文件）。
	 * 与 selected 二选一：file 非空时解析文件内容作为白名单。
	 */
	private MultipartFile file;

	/**
	 * 所属问卷/考试项目 ID（对应 t_project.id）。
	 * 白名单针对哪个问卷生效；空时可能表示应用到当前上下文项目。
	 */
	private String projectId;

}
