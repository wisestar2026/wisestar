package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 英语练习错题实体（对应数据库表 t_english_wrong_question）。
 *
 * @author wisestar
 * @date 2026/10/23
 */
@Data
@TableName("t_english_wrong_question")
@EqualsAndHashCode(callSuper = false)
public class EnglishWrongQuestion extends BaseModel {

	/** 学员账号 ID */
	private String userId;

	/** 来源练习模块 */
	private String module;

	/** 题目内容键 */
	private String questionId;

	/** 教材版本 */
	private String version;

	/** 年级 */
	private String grade;

	/** 册别 */
	private String term;

	/** 单元 */
	private String unit;

	/** 章节 */
	private String chapter;

	/** 知识点 */
	private String knowledgePoint;

	/** 题型 */
	private String questionType;

	/** 题目快照 JSON（含标准答案，用于错题重做） */
	private String questionJson;

	/** 标准答案展示文本 */
	private String correctAnswer;

	/** 解析快照 */
	private String answerAnalysis;

	/** 累计做错次数 */
	private Integer wrongCount;

	/** 最近做错时间 */
	private java.util.Date lastWrongTime;

	/** 是否已被重做消灭（0 否 / 1 是） */
	private Integer eliminated;

}
