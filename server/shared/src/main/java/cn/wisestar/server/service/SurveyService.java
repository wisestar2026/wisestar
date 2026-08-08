package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.*;

import java.util.List;

/**
 * 问卷（公开访问）服务接口（SurveyService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。</p>
 * <p><b>类职责</b>：定义问卷对外（答卷人/公开链接）访问所需的核心能力：
 * 加载问卷、查看统计、提交/暂存答卷、公开查询（验证身份后查结果）、
 * 加载字典、加载考试结果、加载关联链接等。这些能力服务于
 * <em>无需登录</em> 或 <em>以答卷人身份</em> 访问的场景。</p>
 *
 * <p><b>实现类</b>：rdbms 模块 SurveyServiceImpl；<b>调用方</b>：api 模块
 * SurveyApi（/api/public/** 等公开端点，无需登录）。</p>
 *
 * @author javahuang
 * @date 2021/8/22
 */
public interface SurveyService {

	/**
	 * 加载问卷（答卷入口）。
	 *
	 * @param projectQuery 项目查询条件（含 shortId / 访问口令等）
	 * @return 问卷公开视图（标题、题目 schema、设置等）
	 */
	PublicProjectView loadProject(ProjectQuery projectQuery);

	/**
	 * 查询问卷公开统计信息（如回收量、进度等）。
	 *
	 * @param query 项目查询条件
	 * @return 公开统计视图
	 */
	PublicStatisticsView statProject(ProjectQuery query);

	/**
	 * 提交答卷。
	 *
	 * @param request 答卷提交请求（含答案明细、答卷人信息等）
	 * @return 提交后的答卷公开视图（含答卷 id）
	 */
	PublicAnswerView saveAnswer(AnswerRequest request);

	/**
	 * 加载公开查询（验证查询密码后加载查询表单）。
	 *
	 * @param request 公开查询请求（含密码、查询字段值）
	 * @return 查询验证结果视图
	 */
	PublicQueryVerifyView loadQuery(PublicQueryRequest request);

	/**
	 * 获取公开查询结果（按查询条件匹配答卷）。
	 *
	 * @param request 公开查询请求
	 * @return 查询结果视图
	 */
	PublicQueryView getQueryResult(PublicQueryRequest request);

	/**
	 * 校验项目是否可访问（如状态、截止时间等）。
	 *
	 * @param query 项目查询条件
	 * @return 校验通过的问卷公开视图
	 */
	PublicProjectView validateProject(ProjectQuery query);

	/**
	 * 加载问卷使用的字典数据（下拉选项）。
	 *
	 * @param request 字典请求（含字典编码列表）
	 * @return 字典公开视图列表
	 */
	List<PublicDictView> loadDict(PublicDictRequest request);

	/**
	 * 加载考试结果（自动判分后查询成绩）。
	 *
	 * @param request 公开考试请求（含答卷 id 等）
	 * @return 考试结果视图（含得分、正确率等）
	 */
	PublicExamResult loadExamResult(PublicExamRequest request);

	/**
	 * 暂存答卷（未提交，供下次继续填写）。
	 *
	 * @param request 答卷请求
	 */
	void tempSaveAnswer(AnswerRequest request);

	/**
	 * 加载关联链接（问卷题间/问卷间联动所需数据）。
	 *
	 * @param request 公开链接请求（含项目 id、问题/选项 id）
	 * @return 关联链接结果视图
	 */
	PublicLinkResult loadLinkResult(PublicLinkRequest request);

	/**
	 * 校验项目配置合法性（保存问卷时的服务端校验）。
	 *
	 * @param project 项目视图对象（含 schema、设置）
	 */
	void validateProject(ProjectView project);

}
