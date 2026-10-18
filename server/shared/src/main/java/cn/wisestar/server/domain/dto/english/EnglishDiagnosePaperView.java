package cn.wisestar.server.domain.dto.english;

import cn.wisestar.server.domain.dto.student.StudentQuestionView;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 英语学习诊断卷（按 D1→D2→D3 三档分组）。
 *
 * <p>每组题量固定为 5 题，组内跨单元抽取，覆盖不同模块与知识点。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
@Data
public class EnglishDiagnosePaperView {

	/** 卷面标题 */
	private String title;

	/** 难度分组 */
	private List<Section> sections = new ArrayList<>();

	/**
	 * 难度分组。
	 */
	@Data
	public static class Section {

		/** 难度档位：D1 / D2 / D3 */
		private String level;

		/** 档位名称：基础 / 进阶 / 挑战 */
		private String label;

		/** 题目（已剥离答案） */
		private List<StudentQuestionView> questions = new ArrayList<>();

	}
}
