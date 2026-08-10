package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.knowledge.SubjectRequest;
import cn.wisestar.server.domain.dto.knowledge.SubjectView;
import cn.wisestar.server.domain.model.Subject;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Subject 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Mapper
public interface SubjectViewMapper extends BaseModelMapper<SubjectRequest, SubjectView, Subject> {

	Subject fromRequest(SubjectRequest request);

	@Mapping(target = "chapterCount", ignore = true)
	SubjectView toView(Subject item);

}
