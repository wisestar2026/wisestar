package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.constant.AppConsts;
import cn.wisestar.server.core.security.PasswordEncoder;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.student.StudentPermissionView;
import cn.wisestar.server.domain.dto.student.StudentQuery;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.domain.mapper.StudentViewMapper;
import cn.wisestar.server.domain.model.Account;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentPermission;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.AccountMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.StudentPermissionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.StudentService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 学员管理业务实现（学员管理模块）。
 *
 * <p>【被谁调用】StudentApi（管理端学员管理）。</p>
 * <p>【依赖什么】StudentMapper（t_student CRUD）、AccountMapper（t_account 登录账号）、
 * StudentViewMapper（MapStruct 转换）、PasswordEncoder（初始密码 bcrypt 加密）。</p>
 * <p>【核心逻辑】新增学员：校验姓名+联系号码组合查重 → 生成 8 位唯一学号 →
 * 同一事务内写 t_student + t_account（user_type=Student、auth_account=学号、
 * 初始密码 123456）。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Service
@Transactional
@RequiredArgsConstructor
public class StudentServiceImpl extends BaseService<StudentMapper, Student> implements StudentService {

	/**
	 * 学员初始密码（固定 123456，学员端登录后自行修改）。
	 */
	private static final String DEFAULT_PASSWORD = "123456";

	/**
	 * 学号生成最大重试次数（随机 8 位数字，冲突时重新生成直至唯一）。
	 */
	private static final int STUDENT_NO_RETRY_TIMES = 10;

	private final StudentViewMapper studentViewMapper;

	private final AccountMapper accountMapper;

	private final PasswordEncoder passwordEncoder;

	private final StudentPermissionMapper studentPermissionMapper;

	private final SubjectMapper subjectMapper;

	/**
	 * 新增学员：自动生成学号 + 创建登录账号（同一事务）。
	 */
	@Override
	public StudentView createStudent(StudentRequest request) {
		if (!StringUtils.hasText(request.getName())) {
			throw new ValidationException("学员姓名不能为空");
		}
		if (!StringUtils.hasText(request.getPhone())) {
			throw new ValidationException("联系号码不能为空");
		}
		// 姓名 + 联系号码组合查重，防止同一学员重复录入
		Long duplicateCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getName, request.getName())
				.eq(Student::getPhone, request.getPhone()));
		if (duplicateCount != null && duplicateCount > 0) {
			throw new ValidationException("已存在同姓名、同联系号码的学员，请勿重复录入");
		}

		Student student = studentViewMapper.fromRequest(request);
		student.setStudentNo(generateUniqueStudentNo());
		save(student);

		// 创建学员登录账号（学号即账号，初始密码 123456）
		Account account = new Account();
		account.setUserId(student.getId());
		account.setUserType(AppConsts.USER_TYPE.Student.toString());
		account.setAuthType(AppConsts.AUTH_TYPE.PWD.name());
		account.setAuthAccount(student.getStudentNo());
		account.setAuthSecret(passwordEncoder.encode(DEFAULT_PASSWORD));
		account.setStatus(AppConsts.USER_STATUS.VALID);
		accountMapper.insert(account);

		return studentViewMapper.toView(student);
	}

	/**
	 * 学员有效权限（多条有效订单合并，expire_at > NOW()）。
	 */
	@Override
	public StudentPermissionView permissions() {
		String userId = SecurityContextUtils.getUserId();
		if (getById(userId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		List<StudentPermission> perms = studentPermissionMapper.selectList(Wrappers.<StudentPermission>lambdaQuery()
				.eq(StudentPermission::getStudentId, userId)
				.gt(StudentPermission::getExpireAt, new Date()));
		StudentPermissionView view = new StudentPermissionView();
		if (perms.isEmpty()) {
			return view;
		}
		// 学科去重并补名称
		Map<String, String> subjectNames = new LinkedHashMap<>();
		perms.forEach(p -> subjectNames.put(p.getSubjectId(), null));
		subjectMapper.selectBatchIds(subjectNames.keySet())
				.forEach(sub -> subjectNames.put(sub.getId(), sub.getName()));
		subjectNames.forEach((id, name) -> view.getSubjects().add(new StudentPermissionView.SubjectItem(id, name)));
		// 年级 / 教材版本去重
		perms.stream().map(StudentPermission::getGrade).filter(StringUtils::hasText).distinct()
				.forEach(view.getGrades()::add);
		perms.stream().map(StudentPermission::getVersion).filter(StringUtils::hasText).distinct()
				.forEach(view.getVersions()::add);
		return view;
	}

	/**
	 * 学员分页查询（姓名/学号/联系号码模糊匹配）。
	 */
	@Override
	public PaginationResponse<StudentView> pageStudents(StudentQuery query) {
		Page<Student> page = pageByQuery(query,
				Wrappers.<Student>lambdaQuery().like(StringUtils.hasText(query.getName()), Student::getName,
						query.getName())
						.like(StringUtils.hasText(query.getStudentNo()), Student::getStudentNo, query.getStudentNo())
						.like(StringUtils.hasText(query.getPhone()), Student::getPhone, query.getPhone())
						.orderByDesc(Student::getCreateAt));
		return new PaginationResponse<>(page.getTotal(), studentViewMapper.toView(page.getRecords()));
	}

	/**
	 * 更新学员（学号不可修改；姓名+联系号码组合查重排除自身）。
	 */
	@Override
	public void updateStudent(StudentRequest request) {
		if (!StringUtils.hasText(request.getId())) {
			throw new ValidationException("学员ID不能为空");
		}
		Student exist = getById(request.getId());
		if (exist == null) {
			throw new ValidationException("学员不存在");
		}
		// 组合查重（排除自身）
		Long duplicateCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getName, request.getName())
				.eq(Student::getPhone, request.getPhone()).ne(Student::getId, request.getId()));
		if (duplicateCount != null && duplicateCount > 0) {
			throw new ValidationException("已存在同姓名、同联系号码的学员，请勿重复录入");
		}
		Student student = studentViewMapper.fromRequest(request);
		// 学号仅系统生成，update 不允许改动
		student.setStudentNo(exist.getStudentNo());
		updateById(student);
	}

	/**
	 * 删除学员（逻辑删除）。
	 */
	@Override
	public void deleteStudent(StudentRequest request) {
		removeById(request.getId());
	}

	/**
	 * 当前登录学员信息（学员端档案展示，按登录用户ID查 t_student；学员ID即 t_student.id）。
	 */
	@Override
	public StudentView me() {
		String userId = SecurityContextUtils.getUserId();
		Student student = getById(userId);
		if (student == null) {
			throw new ValidationException("当前用户不是学员");
		}
		return studentViewMapper.toView(student);
	}

	/**
	 * 生成 8 位唯一学号：随机 [10000000, 99999999]，与已有学号冲突则重新生成，
	 * 最多重试 {@value #STUDENT_NO_RETRY_TIMES} 次（数据库唯一索引 uk_student_no 兜底）。
	 *
	 * @return 唯一学号
	 */
	private String generateUniqueStudentNo() {
		for (int i = 0; i < STUDENT_NO_RETRY_TIMES; i++) {
			String studentNo = String.valueOf(ThreadLocalRandom.current().nextInt(10000000, 100000000));
			Long existCount = count(Wrappers.<Student>lambdaQuery().eq(Student::getStudentNo, studentNo));
			if (existCount == null || existCount == 0) {
				return studentNo;
			}
		}
		throw new ValidationException("学号生成失败，请重试");
	}

}
