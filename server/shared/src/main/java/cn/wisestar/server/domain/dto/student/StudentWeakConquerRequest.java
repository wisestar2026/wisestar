package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 薄弱点攻克请求。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentWeakConquerRequest {

	/** 知识点ID */
	private String knowledgePointId;

	/** 本次复测正确率（0-100） */
	private Integer correctRate;

}
