package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.ibatis.type.JdbcType;

/**
 * 模板组
 *
 * @TableName t_repo
 */
@Data
@TableName(value = "t_repo", autoResultMap = true)
@EqualsAndHashCode(callSuper = false)
public class Repo extends BaseModel {

	/**
	 * 标题
	 */
	private String name;

	/**
	 * 备注
	 */
	private String description;

	/**
	 * 题库类型 survey/exam
	 */
	private String mode;

	/**
	 * 是否公开 1是 0否
	 */
	private Boolean shared;

	/**
	 * 标签
	 */
	@TableField(typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.VARCHAR)
	private String[] tag;

	/**
	 * 排序优先级
	 */
	private Integer priority;

	/**
	 * 设置
	 */
	private String setting;

	private String category;

	private Boolean isPractice;

	/**
	 * 学科标签（对应数据库列 subject，知识管理维度，如 数学/语文/英语）
	 */
	private String subject;

	/**
	 * 年级标签（对应数据库列 grade，如 一年级/二年级/三年级）
	 */
	private String grade;

	/**
	 * 难度标签（对应数据库列 difficulty）：easy 简单 / medium 中等 / hard 困难
	 */
	private String difficulty;

	@JsonIgnore
	@TableField(exist = false)
	private boolean deleted = false;

}