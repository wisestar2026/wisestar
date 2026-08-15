package cn.wisestar.server.domain.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author javahuang
 * @date 2022/4/27
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class RepoQuery extends PageQuery {

	private String id;

	private String password;

	private String name;

	private String mode;

	private String category;

	private Boolean isPractice;

	/** 学科标签筛选（如 数学/语文/英语） */
	private String subject;

	/** 年级标签筛选（如 一年级/二年级/三年级） */
	private String grade;

	/** 难度标签筛选（easy 简单 / medium 中等 / hard 困难） */
	private String difficulty;
}
