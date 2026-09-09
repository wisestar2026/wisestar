package cn.wisestar.server.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 英语语法实体（对应数据库表 t_english_grammar）。
 *
 * @author wisestar
 * @date 2026/9/9
 */
@Data
@TableName("t_english_grammar")
public class EnglishGrammar {

	/** 主键（雪花 ID，随 MyBatis-Plus 自动生成） */
	@TableId(type = IdType.ASSIGN_ID)
	private String id;

	/** 标题 */
	private String title;

	/** 讲解内容 */
	private String content;

	/** 例句（JSON：examples 数组） */
	private String examples;

	/** 练习题（JSON：exercises 数组） */
	private String exercises;

	/** 年级 */
	private String grade;

}
