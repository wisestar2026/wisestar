package cn.wisestar.server.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 练习详情页「知识绑定回显」数据：某练习被挂载到的章节/小节位置。
 *
 * @author wisestar
 */
@Data
public class RepoBindLocationView {

	/**
	 * 该练习绑定的章节/小节位置集合（保持绑定时间顺序）。
	 */
	private List<RepoNodeBinding> bindings = new ArrayList<>();

	/**
	 * 单个绑定位置。
	 */
	@Data
	public static class RepoNodeBinding {

		/**
		 * 节点类型：CHAP=章节、SECTION=小节。
		 */
		private String nodeType;

		/**
		 * 节点 ID。
		 */
		private String nodeId;

		/**
		 * 节点名称。
		 */
		private String nodeName;

		/**
		 * 父节点 ID（小节所属章节；章节为空）。
		 */
		private String parentNodeId;

		/**
		 * 父节点名称（小节所属章节名；章节为空）。
		 */
		private String parentNodeName;

		/**
		 * 学科 ID（章节所属学科）。
		 */
		private String subjectId;

		/**
		 * 年级。
		 */
		private String grade;

		/**
		 * 学期。
		 */
		private String term;

		/**
		 * 教材版本（仅章节）。
		 */
		private String version;

	}

}
