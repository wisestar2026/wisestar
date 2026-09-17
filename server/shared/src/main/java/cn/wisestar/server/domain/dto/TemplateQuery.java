package cn.wisestar.server.domain.dto;

import cn.wisestar.server.core.constant.ProjectModeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * @author javahuang
 * @date 2021/9/23
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class TemplateQuery extends PageQuery {

	/**
	 * 如果为空则查询普通的题型
	 */
	private SurveySchema.QuestionType questionType;

	private String name;

	/**
	 * 默认查询的是公共库
	 */
	private Integer shared;

	private List<String> categories = new ArrayList<>();

	private List<String> tag = new ArrayList<>();

	private String repoId;

	private ProjectModeEnum mode;

	private String id;

	/**
	 * 学科筛选
	 */
	private String subject;

	/**
	 * 章节筛选
	 */
	private String chapter;

	/**
	 * 小节筛选（知识结构：学科→章节→小节→知识点）
	 */
	private String section;

	/**
	 * 难度筛选
	 */
	private String difficulty;

	/**
	 * 年级筛选（如 一年级/二年级/三年级）
	 */
	private String grade;

	/**
	 * 知识点筛选（按名称匹配）
	 */
	private String knowledgePoint;

	/**
	 * 图片筛选：true=只看带配图的题目；false=只看无配图题目；null=不限。
	 * 配图存放在 template JSON 的 attribute.examImages，故按 template 文本包含匹配判断。
	 */
	private Boolean hasImage;

}
