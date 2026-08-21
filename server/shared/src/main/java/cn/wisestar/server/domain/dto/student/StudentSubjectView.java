package cn.wisestar.server.domain.dto.student;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 学员端学科视图（按订单有效权限过滤）。
 *
 * @author wisestar
 * @date 2026/8/20
 */
@Data
public class StudentSubjectView {

	private String id;

	/** 学科名称 */
	private String name;

	/** 学科图标 */
	private String icon;

	/** 该学科下有权限的教材版本（去重） */
	private List<String> versions = new ArrayList<>();

}
