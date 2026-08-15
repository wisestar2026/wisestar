package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.student.StudentRequest;
import cn.wisestar.server.domain.dto.student.StudentView;
import cn.wisestar.server.domain.model.Student;
import org.mapstruct.Mapper;

/**
 * Student 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/12
 */
@Mapper
public interface StudentViewMapper extends BaseModelMapper<StudentRequest, StudentView, Student> {

	Student fromRequest(StudentRequest request);

	StudentView toView(Student item);

}
