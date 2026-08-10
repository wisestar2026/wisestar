package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.model.KnowledgePoint;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * KnowledgePoint 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Mapper
public interface KnowledgePointViewMapper extends BaseModelMapper<KnowledgePointRequest, KnowledgePointView, KnowledgePoint> {

	KnowledgePoint fromRequest(KnowledgePointRequest request);

	@Mapping(target = "subjectName", ignore = true)
	@Mapping(target = "chapterName", ignore = true)
	@Mapping(target = "sectionName", ignore = true)
	@Mapping(target = "questionCount", ignore = true)
	KnowledgePointView toView(KnowledgePoint item);

}
