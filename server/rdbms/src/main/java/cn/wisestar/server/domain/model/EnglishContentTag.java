package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语题目打标标签实体（对应数据库表 t_english_content_tag）。
 *
 * 【类职责】
 * 保存英语题库打标结果中每道真实考题的诊断标签：知识点（新）、语言技能、认知层级、
 * 干扰项强度与难度初标（D1~D5）。标签按内容键绑定句子包/情景包中的具体题目，
 * 供薄弱点组卷按“知识点 + 难度阶梯”取题，不重复存储题目正文。
 *
 * 【被谁调用】
 * - EnglishContentTagSeeder：应用启动时把打标 JSON 批量写入空表；
 * - EnglishUnitPaperServiceImpl 按 module/unit/difficultySeed 查询并组阶梯卷。
 *
 * 【依赖什么】
 * - 继承 BaseModel，自动获得 id、createAt、createBy、updateAt、updateBy、deleted 字段。
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
@TableName("t_english_content_tag")
@EqualsAndHashCode(callSuper = false)
public class EnglishContentTag extends BaseModel {

	/** 模块：句子 / 单词 / 句式 */
	private String module;

	/** 内容唯一键（句子=版本+年级+册别+单元+来源句子ID+题型；情景=版本+年级+册别+单元+分组+情景标题+轮次） */
	private String contentKey;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 学期/册别（上册/下册） */
	private String term;

	/** 单元 */
	private String unit;

	/** 情景分组序号 */
	private String groupNo;

	/** 情景标题 */
	private String sceneTitle;

	/** 轮次 */
	private Integer turnNo;

	/** 题型 */
	private String questionType;

	/** 打标知识点（诊断最小单位） */
	private String knowledgePoint;

	/** 语言技能：词汇 / 语法 / 语用 */
	private String languageSkill;

	/** 认知层级：记忆 / 理解 / 应用 */
	private String cognitiveLevel;

	/** 干扰项强度：无 / 单一陷阱 / 多重陷阱 */
	private String distractorStrength;

	/** 难度初标：D1 ~ D5 */
	private String difficultySeed;

	/** 是否考题：1 是、0 否（语境行不入库，仅供内容展示） */
	@TableField("is_exam_question")
	private Integer isExamQuestion;

}
