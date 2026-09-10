package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 知识点详情视图（掌握度/评级/薄弱/预习状态）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentKnowledgeDetailView {

	private String id;

	private String name;

	private String desc;

	/** 掌握度 0-100 */
	private int mastery;

	/** 评级标签 */
	private String level;

	/** 是否薄弱 */
	private boolean weak;

	/** 是否已预习完成 */
	private boolean previewed;

}
