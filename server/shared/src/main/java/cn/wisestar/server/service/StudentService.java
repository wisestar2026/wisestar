package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.student.StudentView;

/**
 * 学员管理服务（学员管理模块）。
 *
 * <p><b>定位</b>：学员主数据 CRUD。新增学员时自动生成 8 位学号并创建
 * 学员登录账号（t_account，user_type=Student，初始密码 123456）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
public interface StudentService {

	/**
	 * 新增学员（自动生成学号 + 创建登录账号，同一事务）。
	 *
	 * @param request 学员请求（name/phone 必填）
	 * @return 学员视图（含系统生成的学号）
	 */
	StudentView createStudent(StudentRequest request);

	/**
	 * 学员分页查询。
	 *
	 * @param query 查询条件（name/studentNo/phone 模糊匹配）
	 * @return 分页的学员视图
	 */
	PaginationResponse<StudentView> pageStudents(StudentQuery query);

	/**
	 * 更新学员（学号不可修改，姓名+联系号码组合查重排除自身）。
	 *
	 * @param request 学员请求（含 id）
	 */
	void updateStudent(StudentRequest request);

	/**
	 * 删除学员（逻辑删除）。
	 *
	 * @param request 学员请求（含 id）
	 */
	void deleteStudent(StudentRequest request);

	/**
	 * 当前登录学员信息（学员端档案展示用，按登录用户ID查 t_student）。
	 *
	 * @return 学员视图；当前用户非学员时抛校验异常
	 */
	StudentView me();

}
