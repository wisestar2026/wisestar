package cn.wisestar.server.domain.dto.student;

import lombok.Data;

/**
 * 学员请求（学员管理模块）。
 *
 * <p>create/update 共用：新增时 id 为空；学号 studentNo 由系统自动生成，
 * update 接口不接收 studentNo（学号不可人工修改）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Data
public class StudentRequest {

	/**
	 * 学员ID（新增为空，更新必传）。
	 */
	private String id;

	/**
	 * 姓名（必填）。
	 */
	private String name;

	/**
	 * 年龄（选填）。
	 */
	private Integer age;

	/**
	 * 联系号码（必填，与姓名组合查重）。
	 */
	private String phone;

	/**
	 * 学校（选填）。
	 */
	private String school;

	/**
	 * 校区（选填，本迭代仅占位）。
	 */
	private String campus;

}
