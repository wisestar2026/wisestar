package cn.wisestar.server.domain.dto.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识点-题目绑定请求。
 *
 * <p>将题目库（t_template）中的题目绑定到知识点（全量替换式保存）：
 * 传入完整的 questionIds 列表，后端先清空旧绑定再写入新绑定。
 * 题目不能在此新增，仅能从未绑定的题目库中选择。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Data
public class KnowledgePointQuestionRequest {

	/**
	 * 知识点ID。
	 */
	private String knowledgePointId;

	/**
	 * 要绑定的题目ID列表（t_template.id）。
	 */
	private List<String> questionIds;

}
