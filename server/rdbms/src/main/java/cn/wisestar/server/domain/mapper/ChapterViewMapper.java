package cn.wisestar.server.domain.mapper;

import cn.wisestar.server.core.base.mapper.BaseModelMapper;
import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.model.Chapter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Chapter 对象转换（Request ↔ Model ↔ View）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Mapper
public interface ChapterViewMapper extends BaseModelMapper<ChapterRequest, ChapterView, Chapter> {

	Chapter fromRequest(ChapterRequest request);

	@Mapping(target = "sectionCount", ignore = true)
	@Mapping(target = "repoCount", ignore = true)
	ChapterView toView(Chapter item);

}
