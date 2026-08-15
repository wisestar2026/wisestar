package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 章节-题库绑定请求。
 *
 * <p>将题库管理（t_repo）中的题库绑定到章节（全量替换式保存）：
 * 传入完整的 repoIds 列表，后端先清空旧绑定再写入新绑定。
 * 章节的题库仅能从题库管理选择，不能在此新增。</p>
 *
 * @author wisestar
 * @date 2026/8/11
 */
@Data
public class ChapterRepoRequest {

	/**
	 * 章节ID。
	 */
	private String chapterId;

	/**
	 * 要绑定的题库ID列表（t_repo.id）。
	 */
	private List<String> repoIds;

}
