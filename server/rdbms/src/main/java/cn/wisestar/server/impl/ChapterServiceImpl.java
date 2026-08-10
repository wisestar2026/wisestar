package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.mapper.ChapterViewMapper;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.ChapterService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

/**
 * 章节管理业务实现（知识管理板块二级维度）。
 *
 * <p>【被谁调用】ChapterApi（管理端章节管理）。</p>
 * <p>【依赖什么】ChapterMapper/SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper
 * （BaseMapper CRUD）、ChapterViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】ChapterApi → ChapterServiceImpl → ChapterMapper（t_chapter）；列表返回时经
 * SectionMapper 统计各章节小节数填充 sectionCount；删除时级联逻辑删除其下小节/知识点/题目绑定。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChapterServiceImpl extends BaseService<ChapterMapper, Chapter> implements ChapterService {

	private final ChapterViewMapper chapterViewMapper;

	private final SectionMapper sectionMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	/**
	 * 章节列表（按学科过滤，sort 升序），并统计各章节下小节数。
	 */
	@Override
	public List<ChapterView> listChapters(ChapterRequest query) {
		List<Chapter> chapters = list(Wrappers.<Chapter>lambdaQuery()
				.eq(hasText(query.getSubjectId()), Chapter::getSubjectId, query.getSubjectId())
				.orderByAsc(Chapter::getSort));
		// 一次查出相关小节，按 chapterId 分组计数，避免逐条 N+1 查询
		List<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
		Map<String, Long> sectionCountMap = chapterIds.isEmpty() ? Collections.emptyMap() : sectionMapper.selectList(
				Wrappers.<Section>lambdaQuery().select(Section::getChapterId)
						.in(Section::getChapterId, chapterIds)).stream()
				.collect(Collectors.groupingBy(Section::getChapterId, Collectors.counting()));
		return chapters.stream().map(chapter -> {
			ChapterView view = chapterViewMapper.toView(chapter);
			view.setSectionCount(sectionCountMap.getOrDefault(chapter.getId(), 0L));
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 新增章节。
	 */
	@Override
	public String addChapter(ChapterRequest request) {
		Chapter chapter = chapterViewMapper.fromRequest(request);
		save(chapter);
		return chapter.getId();
	}

	/**
	 * 更新章节。
	 */
	@Override
	public void updateChapter(ChapterRequest request) {
		updateById(chapterViewMapper.fromRequest(request));
	}

	/**
	 * 删除章节（级联逻辑删除其下小节、知识点及知识点-题目绑定）。
	 */
	@Override
	public void deleteChapter(ChapterRequest request) {
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.select(Section::getId).eq(Section::getChapterId, request.getId()));
		List<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toList());
		List<String> knowledgePointIds = sectionIds.isEmpty() ? Collections.emptyList() : knowledgePointMapper.selectList(
				Wrappers.<KnowledgePoint>lambdaQuery().select(KnowledgePoint::getId)
						.in(KnowledgePoint::getSectionId, sectionIds)).stream()
				.map(KnowledgePoint::getId).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(knowledgePointIds)) {
			knowledgePointQuestionMapper.delete(Wrappers.<KnowledgePointQuestion>lambdaQuery()
					.in(KnowledgePointQuestion::getKnowledgePointId, knowledgePointIds));
		}
		if (!CollectionUtils.isEmpty(sectionIds)) {
			knowledgePointMapper.delete(Wrappers.<KnowledgePoint>lambdaQuery()
					.in(KnowledgePoint::getSectionId, sectionIds));
		}
		sectionMapper.delete(Wrappers.<Section>lambdaQuery().eq(Section::getChapterId, request.getId()));
		removeById(request.getId());
	}

}
