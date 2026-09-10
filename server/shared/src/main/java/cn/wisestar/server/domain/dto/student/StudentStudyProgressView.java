package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员学科学习进度视图（章节 → 知识点掌握度/评级/薄弱）。
 *
 * @author wisestar
 * @date 2026/9/10
 */
@Data
public class StudentStudyProgressView {

	/** 章节列表 */
	private List<Chapter> chapters = new ArrayList<>();

	/**
	 * 章节。
	 */
	@Data
	public static class Chapter {

		/** 章节ID */
		private String id;

		/** 章节名称 */
		private String name;

		/** 章节图标 */
		private String icon;

		/** 章节下知识点 */
		private List<Kp> kps = new ArrayList<>();

	}

	/**
	 * 章节下知识点掌握度。
	 */
	@Data
	public static class Kp {

		/** 知识点ID */
		private String id;

		/** 知识点名称 */
		private String name;

		/** 所属小节ID */
		private String sectionId;

		/** 掌握度 0-100 */
		private int mastery;

		/** 评级：精通/熟练/夯实/待巩固/待攻克 */
		private String level;

		/** 是否薄弱 */
		private boolean weak;

	}

}
