package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.student.OrderQuery;
import cn.wisestar.server.domain.dto.student.OrderRequest;
import cn.wisestar.server.domain.dto.student.OrderView;
import cn.wisestar.server.domain.mapper.OrderViewMapper;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentOrder;
import cn.wisestar.server.domain.model.StudentPermission;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.StudentOrderMapper;
import cn.wisestar.server.mapper.StudentPermissionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.OrderService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 学员订单业务实现（学员管理模块）。
 *
 * <p>【被谁调用】OrderApi（管理端订单管理）。</p>
 * <p>【依赖什么】StudentOrderMapper/StudentPermissionMapper/StudentMapper/SubjectMapper
 * （BaseMapper CRUD）、OrderViewMapper（MapStruct 转换）。</p>
 * <p>【核心逻辑】创建订单：校验 → 计算 expireAt（now + duration × 单位）→
 * 同一事务内写 t_student_order + 按学科×年级笛卡尔积展开写 t_student_permission。
 * 作废/删除订单时同步逻辑删除该订单的权限行。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl extends BaseService<StudentOrderMapper, StudentOrder> implements OrderService {

	/**
	 * 订单状态：生效。
	 */
	private static final int ORDER_STATUS_VALID = 1;

	/**
	 * 订单状态：作废。
	 */
	private static final int ORDER_STATUS_CANCELED = 0;

	/**
	 * 时长单位：天。
	 */
	private static final String UNIT_DAY = "DAY";

	/**
	 * 时长单位：月。
	 */
	private static final String UNIT_MONTH = "MONTH";

	/**
	 * 时长单位：年。
	 */
	private static final String UNIT_YEAR = "YEAR";

	private final OrderViewMapper orderViewMapper;

	private final StudentPermissionMapper studentPermissionMapper;

	private final StudentMapper studentMapper;

	private final SubjectMapper subjectMapper;

	/**
	 * 创建订单：订单主表 + 权限展开行（同一事务）。
	 */
	@Override
	public OrderView createOrder(OrderRequest request) {
		validateOrderRequest(request);

		Student student = studentMapper.selectById(request.getStudentId());
		if (student == null) {
			throw new ValidationException("学员不存在");
		}

		StudentOrder order = orderViewMapper.fromRequest(request);
		order.setExpireAt(calcExpireAt(request.getDuration(), request.getDurationUnit()));
		order.setStatus(ORDER_STATUS_VALID);
		save(order);

		// 学科×年级笛卡尔积展开写入权限表（多选学科 × 多选年级）
		List<StudentPermission> permissions = buildPermissions(order);
		permissions.forEach(studentPermissionMapper::insert);

		return buildOrderView(order);
	}

	/**
	 * 订单分页查询（studentName 模糊匹配先按学员名查学员ID再过滤）。
	 */
	@Override
	public PaginationResponse<OrderView> pageOrders(OrderQuery query) {
		Set<String> studentIds = null;
		if (StringUtils.hasText(query.getStudentName())) {
			studentIds = studentMapper.selectList(
					Wrappers.<Student>lambdaQuery().like(Student::getName, query.getStudentName()))
					.stream().map(Student::getId).collect(Collectors.toSet());
			if (studentIds.isEmpty()) {
				return new PaginationResponse<>(0L, Collections.emptyList());
			}
		}

		Page<StudentOrder> page = pageByQuery(query,
				Wrappers.<StudentOrder>lambdaQuery()
						.eq(StringUtils.hasText(query.getStudentId()), StudentOrder::getStudentId,
								query.getStudentId())
						.in(studentIds != null, StudentOrder::getStudentId, studentIds)
						.eq(query.getStatus() != null, StudentOrder::getStatus, query.getStatus())
						.orderByDesc(StudentOrder::getCreateAt));
		return new PaginationResponse<>(page.getTotal(),
				page.getRecords().stream().map(this::buildOrderView).collect(Collectors.toList()));
	}

	/**
	 * 作废订单：status=0 + 该订单权限逻辑删除。
	 */
	@Override
	public void cancelOrder(OrderRequest request) {
		if (!StringUtils.hasText(request.getId())) {
			throw new ValidationException("订单ID不能为空");
		}
		StudentOrder order = getById(request.getId());
		if (order == null) {
			throw new ValidationException("订单不存在");
		}
		StudentOrder update = new StudentOrder();
		update.setId(order.getId());
		update.setStatus(ORDER_STATUS_CANCELED);
		updateById(update);
		removePermissionsByOrder(order.getId());
	}

	/**
	 * 删除订单：逻辑删除 + 该订单权限逻辑删除。
	 */
	@Override
	public void deleteOrder(OrderRequest request) {
		removeById(request.getId());
		removePermissionsByOrder(request.getId());
	}

	/**
	 * 校验订单请求参数。
	 */
	private void validateOrderRequest(OrderRequest request) {
		if (!StringUtils.hasText(request.getStudentId())) {
			throw new ValidationException("学员不能为空");
		}
		if (CollectionUtils.isEmpty(request.getSubjectIds())) {
			throw new ValidationException("请至少选择一个学科");
		}
		if (CollectionUtils.isEmpty(request.getGrades())) {
			throw new ValidationException("请至少选择一个年级");
		}
		if (request.getDuration() == null || request.getDuration() <= 0) {
			throw new ValidationException("账号时长必须大于 0");
		}
		if (!Arrays.asList(UNIT_DAY, UNIT_MONTH, UNIT_YEAR).contains(request.getDurationUnit())) {
			throw new ValidationException("时长单位不合法");
		}
	}

	/**
	 * 按学科×年级笛卡尔积构造权限展开行。
	 */
	private List<StudentPermission> buildPermissions(StudentOrder order) {
		List<String> subjectIds = split(order.getSubjectIds());
		List<String> grades = split(order.getGrades());
		return subjectIds.stream().flatMap(subjectId -> grades.stream().map(grade -> {
			StudentPermission permission = new StudentPermission();
			permission.setStudentId(order.getStudentId());
			permission.setOrderId(order.getId());
			permission.setSubjectId(subjectId);
			permission.setGrade(grade);
			permission.setVersion(order.getVersion());
			permission.setExpireAt(order.getExpireAt());
			return permission;
		})).collect(Collectors.toList());
	}

	/**
	 * 逻辑删除某订单的全部权限行。
	 */
	private void removePermissionsByOrder(String orderId) {
		studentPermissionMapper.delete(
				Wrappers.<StudentPermission>lambdaQuery().eq(StudentPermission::getOrderId, orderId));
	}

	/**
	 * 组装订单视图：回填学员学号/姓名、学科名称列表。
	 */
	private OrderView buildOrderView(StudentOrder order) {
		OrderView view = orderViewMapper.toView(order);
		Student student = studentMapper.selectById(order.getStudentId());
		if (student != null) {
			view.setStudentNo(student.getStudentNo());
			view.setStudentName(student.getName());
		}
		// 学科ID → 名称（批量查一次，避免 N+1）
		List<String> subjectIds = split(order.getSubjectIds());
		Map<String, String> idNameMap = new HashMap<>();
		if (!subjectIds.isEmpty()) {
			subjectMapper.selectBatchIds(subjectIds)
					.forEach(subject -> idNameMap.put(subject.getId(), subject.getName()));
		}
		view.setSubjects(subjectIds.stream().map(id -> {
			OrderView.SubjectItem item = new OrderView.SubjectItem();
			item.setId(id);
			item.setName(idNameMap.get(id));
			return item;
		}).collect(Collectors.toList()));
		return view;
	}

	/**
	 * 按时长与单位计算有效期（DAY 加天数，MONTH 加月数，YEAR 加年数）。
	 */
	private Date calcExpireAt(Integer duration, String durationUnit) {
		LocalDateTime expire = LocalDateTime.now();
		if (UNIT_DAY.equals(durationUnit)) {
			expire = expire.plusDays(duration);
		} else if (UNIT_MONTH.equals(durationUnit)) {
			expire = expire.plusMonths(duration);
		} else {
			expire = expire.plusYears(duration);
		}
		return Date.from(expire.atZone(ZoneId.systemDefault()).toInstant());
	}

	/**
	 * 逗号分隔字符串 → 字符串列表。
	 */
	private List<String> split(String values) {
		if (values == null || values.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.stream(values.split(",")).filter(x -> !x.trim().isEmpty()).collect(Collectors.toList());
	}

}
