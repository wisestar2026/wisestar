package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员有效权限视图（多条有效订单合并，expire_at > NOW() 才生效）。
 *
 * <p>供学员端按订单授予范围过滤可访问内容：学科 + 年级 + 教材版本。</p>
 *
 * @author wisestar
 * @date 2026/8/19
 */
@Data
public class StudentPermissionView {

	/** 可访问学科（去重） */
	private List<SubjectItem> subjects = new ArrayList<>();

	/** 可访问年级（去重） */
	private List<String> grades = new ArrayList<>();

	/** 可访问教材版本（去重） */
	private List<String> versions = new ArrayList<>();

	@Data
	public static class SubjectItem {

		private String id;

		private String name;

		public SubjectItem() {
		}

		public SubjectItem(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}
