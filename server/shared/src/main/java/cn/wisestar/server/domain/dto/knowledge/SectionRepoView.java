package cn.wisestar.server.domain.dto.knowledge;

import cn.wisestar.server.domain.dto.RepoView;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 小节已绑定题库视图。
 *
 * <p>在题库信息（{@link RepoView}）基础上追加用途标记，供管理端
 * 「练习设置」回显每个绑定题库的用途（preview/practice/both）。</p>
 *
 * @author wisestar
 * @date 2026/9/11
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SectionRepoView extends RepoView {

	/**
	 * 用途标记（preview 预习专用 / practice 练习专用 / both 通用）。
	 */
	private String usageType;

}
