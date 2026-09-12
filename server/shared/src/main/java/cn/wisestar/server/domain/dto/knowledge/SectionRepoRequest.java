package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 小节-题库绑定请求。
 *
 * <p>将题库管理（t_repo）中的题库绑定到小节（全量替换式保存）：
 * 传入完整的 repoIds 列表，后端先清空旧绑定再写入新绑定。
 * 小节的题库仅能从题库管理选择，不能在此新增。</p>
 *
 * <p>usageByRepo 为可选的用途标记（repoId → preview/practice/both）；
 * 未出现的 repoId 在保存时复用其既有用途，仍缺失时按通用处理。</p>
 *
 * @author wisestar
 * @date 2026/8/11
 */
@Data
public class SectionRepoRequest {

	/**
	 * 小节ID。
	 */
	private String sectionId;

	/**
	 * 要绑定的题库ID列表（t_repo.id）。
	 */
	private List<String> repoIds;

	/**
	 * 题库用途标记（repoId → preview/practice/both），可选。
	 */
	private Map<String, String> usageByRepo;

}
