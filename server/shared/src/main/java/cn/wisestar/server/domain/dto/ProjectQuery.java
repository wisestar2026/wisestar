package cn.wisestar.server.domain.dto;

import cn.wisestar.server.core.constant.ExamExerciseTypeEnum;
import cn.wisestar.server.core.constant.ProjectModeEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.LinkedHashMap;

/**
 * @author javahuang
 * @date 2021/8/31
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class ProjectQuery extends PageQuery {

	private String id;

	private String repoId;

	private String password;

	private String name;

	private String parentId;

	private ProjectModeEnum mode;

	private LinkedHashMap<String, Object> answer;

	private String answerId;

	private ExamExerciseTypeEnum examExerciseType;

	/**
	 * 设置查询的作答内容。
	 *
	 * @param answer 作答内容（键值对）
	 */
	public void setAnswer(LinkedHashMap<String, Object> answer) {
		this.answer = answer;

	}

}
