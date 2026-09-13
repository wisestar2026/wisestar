package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员本学期学习币视图（分学科，单科上限 10000）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentCoinsView {

	/** 总学习币（各科 + 手动发币） */
	private int total;

	/** 单科上限 */
	private int limit = 10000;

	/** 分科学币 */
	private List<SubjectCoin> list = new ArrayList<>();

	@Data
	public static class SubjectCoin {

		private String subjectId;

		private String subjectName;

		private int coins;

		private boolean reachedLimit;

	}

}
