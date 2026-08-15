package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.student.OrderRequest;
import cn.wisestar.server.domain.dto.student.OrderView;
import cn.wisestar.server.domain.model.StudentOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StudentOrder 对象转换（Request ↔ Model ↔ View）。
 *
 * <p>订单表 grades 以逗号分隔字符串存储，视图层转为 List&lt;String&gt;。</p>
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Mapper
public interface OrderViewMapper extends BaseModelMapper<OrderRequest, OrderView, StudentOrder> {

	/**
	 * 请求 → 订单模型（学科/年级多选列表转逗号分隔字符串）。
	 */
	@Mapping(target = "subjectIds", expression = "java(join(request.getSubjectIds()))")
	@Mapping(target = "grades", expression = "java(join(request.getGrades()))")
	@Override
	StudentOrder fromRequest(OrderRequest request);

	/**
	 * 订单模型 → 视图（年级逗号分隔字符串转列表；学员信息与学科名称由 Service 回填）。
	 */
	@Mapping(target = "studentNo", ignore = true)
	@Mapping(target = "studentName", ignore = true)
	@Mapping(target = "subjects", ignore = true)
	@Mapping(target = "grades", expression = "java(split(item.getGrades()))")
	OrderView toView(StudentOrder item);

	/**
	 * 字符串列表 → 逗号分隔字符串（学科ID/年级存库格式）。
	 *
	 * @param values 列表（可为空）
	 * @return 逗号分隔字符串；空列表返回 null
	 */
	default String join(List<String> values) {
		if (values == null || values.isEmpty()) {
			return null;
		}
		return String.join(",", values);
	}

	/**
	 * 逗号分隔字符串 → 字符串列表（视图展示格式）。
	 *
	 * @param values 逗号分隔字符串（可为空）
	 * @return 字符串列表；空输入返回空列表
	 */
	default List<String> split(String values) {
		if (values == null || values.trim().isEmpty()) {
			return Collections.emptyList();
		}
		return Arrays.stream(values.split(",")).filter(x -> !x.trim().isEmpty()).collect(Collectors.toList());
	}

}
