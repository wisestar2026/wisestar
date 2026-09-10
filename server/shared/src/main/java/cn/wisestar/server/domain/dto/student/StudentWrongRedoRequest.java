package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 错题重做请求。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentWrongRedoRequest {

	/** 题目ID */
	private String questionId;

	/** 学员作答（{type:'option',optionId} / {type:'options',optionIds} / {type:'text',text}） */
	private java.util.Map<String, Object> answer;

}
