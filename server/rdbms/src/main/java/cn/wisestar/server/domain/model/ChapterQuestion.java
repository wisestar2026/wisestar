package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节-测试题目绑定实体（对应数据库表 t_chapter_question）。
 *
 * <p>章节与题目（t_template）的多对多关联：一个章节可绑定多道题库测试题目，
 * 一道题目也可被多个章节引用。绑定题目只能从题目库选择，不能在本模块新增。</p>
 *
 * @author wisestar
 * @date 2026/8/11
 */
@Data
@TableName("t_chapter_question")
@EqualsAndHashCode(callSuper = false)
public class ChapterQuestion extends BaseModel {

	/**
	 * 章节ID（t_chapter.id）。
	 */
	private String chapterId;

	/**
	 * 题目ID（t_template.id）。
	 */
	private String questionId;

}
