package cn.wisestar.server.domain.model;

import cn.wisestar.server.core.model.BaseModel;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 章节-题库绑定实体（对应数据库表 t_chapter_repo）。
 *
 * <p>章节与题库（t_repo）的多对多关联：一个章节可绑定多个题库，
 * 一个题库也可被多个章节引用。绑定题库只能从题库管理选择，不能在本模块新增。</p>
 *
 * @author wisestar
 * @date 2026/8/11
 */
@Data
@TableName("t_chapter_repo")
@EqualsAndHashCode(callSuper = false)
public class ChapterRepo extends BaseModel {

	/**
	 * 章节ID（t_chapter.id）。
	 */
	private String chapterId;

	/**
	 * 题库ID（t_repo.id）。
	 */
	private String repoId;

}
