package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.model.Section;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Section 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Mapper
public interface SectionViewMapper extends BaseModelMapper<SectionRequest, SectionView, Section> {

	Section fromRequest(SectionRequest request);

	@Mapping(target = "knowledgePointCount", ignore = true)
	@Mapping(target = "repoCount", ignore = true)
	SectionView toView(Section item);

}
