package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 小节-测试题目绑定请求。
 *
 * <p>将题目库（t_template）中的题目绑定到小节（全量替换式保存）：
 * 传入完整的 questionIds 列表，后端先清空旧绑定再写入新绑定。
 * 小节的测试题目仅能从题目库选择，不能在此新增。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class SectionQuestionRequest {

	/**
	 * 小节ID。
	 */
	private String sectionId;

	/**
	 * 要绑定的题目ID列表（t_template.id）。
	 */
	private List<String> questionIds;

}
