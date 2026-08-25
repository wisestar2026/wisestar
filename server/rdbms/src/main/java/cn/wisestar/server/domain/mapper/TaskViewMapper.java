package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.task.TaskRequest;
import cn.wisestar.server.domain.dto.task.TaskView;
import cn.wisestar.server.domain.model.Task;
import org.mapstruct.Mapper;

/**
 * Task 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/23
 */
@Mapper
public interface TaskViewMapper extends BaseModelMapper<TaskRequest, TaskView, Task> {

	Task fromRequest(TaskRequest request);

	TaskView toView(Task item);

}
