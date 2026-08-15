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

}
