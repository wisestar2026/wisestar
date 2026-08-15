package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.constant.ProjectModeEnum;
import cn.wisestar.server.core.model.BaseModel;
import cn.wisestar.server.domain.dto.SurveySchema;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 问题模板表实体（对应数据库表 t_template）
 *
 * 【类职责】
 * 代表"题库"中的一个具体题目（一道单选/多选/判断/填空/简答题），是 AI 自习室系统中
 * 题库（Repo）与问卷/考试（Project）之间的桥梁：一个题库（t_repo）下挂多道题目，
 * 题目以"模板"形式存储；出卷时（pickQuestionFromRepo / RandomSurveyProcessor）从题库
 * 拉取模板并渲染为问卷题目（SurveySchema）。
 *
 * 【被谁调用】
 * - 数据访问层：TemplateMapper（MyBatis-Plus 基础 CRUD）、t_tag 标签表通过 entity_id 关联本表
 * - 业务层：TemplateServiceImpl（四维筛选 listTemplate）、RepoServiceImpl（导出/批量导入/
 *   随机抽题 pickQuestionFromRepo）、SurveyServiceImpl（题库练习加载题目）、RandomSurveyProcessor
 *
 * 【依赖什么】
 * - 继承 BaseModel，自动获得 id、createAt、createBy、updateAt、updateBy、deleted 逻辑删除字段
 * - 题目 JSON 结构复用 domain.dto.SurveySchema（问卷 schema 同一套结构）
 * - ProjectModeEnum：题目所处模式（survey 问卷 / exam 考试）
 *
 * 【核心数据流】
 * 题库管理端（Controller）→ RepoServiceImpl/TemplateServiceImpl → TemplateMapper
 * → t_template 表；出题时 RepoServiceImpl.pickQuestionFromRepo 将本实体转换为 SurveySchema
 * 下发到前端答题。题目难度体系（subject/chapter/knowledgePoint/difficulty 四字段）在答题
 * 提交时被 AnswerServiceImpl.generateAnswerDetails 快照进 t_answer_detail 答题明细表，
 * 供 AnalysisServiceImpl 做知识点聚合分析。
 *
 * @author javahuang
 * @date 2021/9/23
 */
@Data
@TableName(value = "t_template", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class Template extends BaseModel {

	/**
	 * 所属题库ID（对应 t_repo.id）。
	 * 一个题库下有多道题目，通过该字段与题库建立一对多关系；
	 * 逻辑删除/删除题库时（RepoServiceImpl.deleteRepo）会连带按 repoId 删除本表数据。
	 */
	private String repoId;

	/**
	 * 题目序号（同题库内用于排序/定位的编号）。
	 * 批量导入导出（batchAddRepoTemplate）时以"序号 + 题型"作为匹配键做更新幂等。
	 */
	private String serialNo;

	/**
	 * 模板标题（即题干文本，对应数据库列 name）。
	 * 前端题目列表展示的题目名称。
	 */
	private String name;

	/**
	 * 问题类型（对应数据库列 question_type，MyBatis-Plus 按枚举 name 存储）。
	 * 取值见 SurveySchema.QuestionType：Radio 单选 / Checkbox 多选 / Judge 判断 /
	 * FillBlank 填空 / Textarea 简答 / Survey 问卷题 / RandomSurvey 随机抽题等。
	 */
	private SurveySchema.QuestionType questionType;

	/**
	 * 题目完整 JSON 结构（对应数据库列 template，LONGVARCHAR 文本，Jackson 序列化存储）。
	 * 结构复用问卷 schema（SurveySchema）：title 题干、children 选项/空列表、
	 * attribute 题目属性（含 examScore 分值、examCorrectAnswer 整题正确答案、
	 * subject/chapter/knowledgePoint/difficulty 知识点旧快照、examAnalysis 解析等）。
	 * 注意：同一题目存在两套知识点字段——本实体顶层 subject/chapter/knowledgePoint/difficulty
	 * 为新数据格式，attribute 内为兼容旧数据的快照格式（见 RepoServiceImpl.knowledgePointText 的兼容逻辑）。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.LONGVARCHAR)
	private SurveySchema template;

	/**
	 * 标签数组（对应数据库列 tag，VARCHAR，Jackson 序列化为 JSON 数组存储，如 ["代数","函数"]）。
	 * 与 t_tag 标签表冗余存储：批量添加题目时会将模板内 tags 同步写 t_tag（entity_id 关联），
	 * 标签筛选走 t_tag 表 exists 子查询。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.VARCHAR)
	private String[] tag;

	/**
	 * 模板所属模式（对应数据库列 mode）：survey 问卷 / exam 考试。
	 * 决定题目被哪个模式的出卷流程使用。
	 */
	private ProjectModeEnum mode;

	/**
	 * 模板分类（对应数据库列 category），主要用于"模板广场"按分类浏览。
	 * listTemplateCategories 按该字段 DISTINCT 去重查询。
	 */
	private String category;

	/**
	 * 模板预览地址（对应数据库列 preview_url），模板广场/详情预览用。
	 */
	private String previewUrl;

	/**
	 * 排序优先级（对应数据库列 priority），值越小优先级越高，
	 * listTemplate 默认按该字段升序排列。
	 */
	private Integer priority;

	/**
	 * 创建人（对应数据库列 create_by，FieldFill.INSERT 自动填充为当前登录用户）。
	 */
	@TableField(fill = FieldFill.INSERT)
	private String createBy;

	/**
	 * 是否与其他用户共享（对应数据库列 shared）：0 私有（仅创建人可见）、1 共享。
	 * 列表查询按"自己创建 OR 共享"过滤（RepoServiceImpl.listRepo / TemplateServiceImpl.listTemplate）。
	 */
	private Integer shared;

	/**
	 * 学科（对应数据库列 subject，知识点体系一级维度）。
	 * 新题目数据格式：题目直接挂载学科；旧数据存储在 template JSON 的 attribute.subject 中。
	 */
	private String subject;

	/**
	 * 章节（对应数据库列 chapter，知识点体系二级维度）。
	 * 与 subject 配合组成"学科 > 章节"两级定位。
	 */
	private String chapter;

	/**
	 * 知识点数组（对应数据库列 knowledge_point，VARCHAR，Jackson 序列化为 JSON 数组，
	 * 如 ["函数单调性","奇偶性"]，一道题可挂多个知识点）。
	 * 答题提交时被 AnswerServiceImpl.generateAnswerDetails 以逗号连接快照进
	 * t_answer_detail.knowledge_point；AnalysisServiceImpl 再按逗号拆开做聚合统计。
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.VARCHAR)
	private String[] knowledgePoint;

	/**
	 * 难度（对应数据库列 difficulty）：easy 简单 / medium 中等 / hard 困难。
	 * 四维筛选（listTemplate 的 subject/chapter/knowledgePoint/difficulty）之一，
	 * 导出时转换为中文标签"简单/中等/困难"。
	 */
	private String difficulty;

	/**
	 * 年级标签（对应数据库列 grade，如 一年级/二年级/三年级）。
	 * 与 subject/chapter/knowledgePoint/difficulty 共同组成题目标签体系，
	 * 用于题目管理列表与章节/小节绑定时按年级筛选。
	 */
	private String grade;

}
