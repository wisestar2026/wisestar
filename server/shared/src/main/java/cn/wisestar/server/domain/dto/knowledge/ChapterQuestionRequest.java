package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 章节-测试题目绑定请求。
 *
 * <p>将题目库（t_template）中的题目绑定到章节（全量替换式保存）：
 * 传入完整的 questionIds 列表，后端先清空旧绑定再写入新绑定。
 * 章节的测试题目仅能从题目库选择，不能在此新增。</p>
 *
 * @author wisestar
 * @date 2026/8/11
 */
@Data
public class ChapterQuestionRequest {

	/**
	 * 章节ID。
	 */
	private String chapterId;

	/**
	 * 要绑定的题目ID列表（t_template.id）。
	 */
	private List<String> questionIds;

}
