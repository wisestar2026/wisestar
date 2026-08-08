package cn.wisestar.server.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

/**
 * 答卷分页查询条件 DTO。
 *
 * 【类职责】
 * 承载"答卷列表查询"（AnswerApi.listAnswer）的过滤参数：按项目、主键、时间区间、
 * 项目名称等维度筛选答卷。
 *
 * 【被谁调用】
 * - AnswerApi.listAnswer → AnswerServiceImpl.listAnswer（构造 MyBatis-Plus 查询条件）
 * - AnswerServiceImpl.downloadSurvey（导出答卷时复用 listAnswer 查询）
 *
 * 【数据流】
 * 前端答卷列表页（AnswerListPage）GET /api/answer/list → 本 DTO（GET query 参数绑定）
 * → AnswerServiceImpl.listAnswer → AnswerMapper 分页查询 t_answer → AnswerView 列表返回
 *
 * @author javahuang
 * @date 2021/8/31
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AnswerQuery extends PageQuery {

	/** 答卷主键（精确匹配单条） */
	private String id;

	/** 问卷/考试项目 ID（精确匹配，指定时后端会补充答案附加信息） */
	private String projectId;

	/** 答卷主键集合（批量筛选） */
	private List<String> ids;

	/** 答卷 IP（预留筛选条件） */
	private String ip;

	/** 答卷 Cookie 标识（预留筛选条件） */
	private String cookie;

	/** 创建时间区间起始（大于该时间） */
	private Date startTime;

	/** 创建时间区间结束（小于该时间） */
	private Date endTime;

	/** 答题人/创建人 ID（学生 ID） */
	private String createBy;

	/** 获取最近一份答案（学生端"我的最近答卷"场景） */
	private Boolean latest;

	/** 是否启用排名（考试排名场景） */
	private boolean rankEnabled;

	/**
	 * 根据选项答案查询（预留：按答卷中某选项的值过滤）
	 */
	private String valueQuery;

	/**
	 * 项目名称模糊搜索（对应 t_project.name）。
	 * 由前端答卷列表页"按问卷名称搜索"传入：先按名称模糊查出匹配的 projectId 集合，
	 * 再对答卷做 IN 过滤（见 AnswerServiceImpl.listAnswer）。
	 */
	private String projectName;

}
