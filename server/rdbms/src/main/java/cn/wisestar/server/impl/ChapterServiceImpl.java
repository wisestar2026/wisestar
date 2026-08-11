package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.ChapterQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.mapper.ChapterViewMapper;
import cn.wisestar.server.domain.mapper.TemplateViewMapper;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.ChapterQuestion;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterQuestionMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.TemplateMapper;
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
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

/**
 * 章节管理业务实现（知识管理板块二级维度）。
 *
 * <p>【被谁调用】ChapterApi（管理端章节管理）。</p>
 * <p>【依赖什么】ChapterMapper/SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper/
 * ChapterQuestionMapper/TemplateMapper（BaseMapper CRUD）、
 * ChapterViewMapper/TemplateViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】ChapterApi → ChapterServiceImpl → ChapterMapper（t_chapter）；列表返回时经
 * SectionMapper 统计各章节小节数、经 ChapterQuestionMapper 统计已绑定测试题数；
 * 章节测试题目经 t_chapter_question 关联题目库（t_template），全量替换式保存；
 * 删除时级联逻辑删除其下小节/知识点/知识点题目绑定与章节测试题目绑定。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChapterServiceImpl extends BaseService<ChapterMapper, Chapter> implements ChapterService {

	private final ChapterViewMapper chapterViewMapper;

	private final TemplateViewMapper templateViewMapper;

	private final SectionMapper sectionMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final ChapterQuestionMapper chapterQuestionMapper;

	private final TemplateMapper templateMapper;

	/**
	 * 章节列表（按学科过滤，sort 升序），并统计各章节下小节数与已绑定测试题数。
	 */
	@Override
	public List<ChapterView> listChapters(ChapterRequest query) {
		List<Chapter> chapters = list(Wrappers.<Chapter>lambdaQuery()
				.eq(hasText(query.getSubjectId()), Chapter::getSubjectId, query.getSubjectId())
				.orderByAsc(Chapter::getSort));
		// 一次查出相关小节与章节测试绑定，按 chapterId 分组计数，避免逐条 N+1 查询
		List<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
		Map<String, Long> sectionCountMap = chapterIds.isEmpty() ? Collections.emptyMap() : sectionMapper.selectList(
				Wrappers.<Section>lambdaQuery().select(Section::getChapterId)
						.in(Section::getChapterId, chapterIds)).stream()
				.collect(Collectors.groupingBy(Section::getChapterId, Collectors.counting()));
		Map<String, Long> questionCountMap = chapterIds.isEmpty() ? Collections.emptyMap() : chapterQuestionMapper.selectList(
				Wrappers.<ChapterQuestion>lambdaQuery().select(ChapterQuestion::getChapterId)
						.in(ChapterQuestion::getChapterId, chapterIds)).stream()
				.collect(Collectors.groupingBy(ChapterQuestion::getChapterId, Collectors.counting()));
		return chapters.stream().map(chapter -> {
			ChapterView view = chapterViewMapper.toView(chapter);
			view.setSectionCount(sectionCountMap.getOrDefault(chapter.getId(), 0L));
			view.setQuestionCount(questionCountMap.getOrDefault(chapter.getId(), 0L));
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
	 * 删除章节（级联逻辑删除其下小节、知识点、知识点-题目绑定与章节测试题目绑定）。
	 */
	@Override
	public void deleteChapter(ChapterRequest request) {
		chapterQuestionMapper.delete(Wrappers.<ChapterQuestion>lambdaQuery()
				.eq(ChapterQuestion::getChapterId, request.getId()));
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

	/**
	 * 保存章节-测试题目绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
	 */
	@Override
	public void saveQuestions(ChapterQuestionRequest request) {
		chapterQuestionMapper.delete(Wrappers.<ChapterQuestion>lambdaQuery()
				.eq(ChapterQuestion::getChapterId, request.getChapterId()));
		if (CollectionUtils.isEmpty(request.getQuestionIds())) {
			return;
		}
		request.getQuestionIds().stream().filter(Objects::nonNull).distinct().forEach(questionId -> {
			ChapterQuestion binding = new ChapterQuestion();
			binding.setChapterId(request.getChapterId());
			binding.setQuestionId(questionId);
			chapterQuestionMapper.insert(binding);
		});
	}

	/**
	 * 查询章节已绑定的测试题目列表（题目库 t_template 数据，保持绑定顺序）。
	 */
	@Override
	public List<TemplateView> listQuestions(String chapterId) {
		List<ChapterQuestion> bindings = chapterQuestionMapper.selectList(
				Wrappers.<ChapterQuestion>lambdaQuery()
						.eq(ChapterQuestion::getChapterId, chapterId)
						.orderByAsc(ChapterQuestion::getCreateAt));
		if (CollectionUtils.isEmpty(bindings)) {
			return Collections.emptyList();
		}
		List<String> questionIds = bindings.stream().map(ChapterQuestion::getQuestionId)
				.collect(Collectors.toList());
		Map<String, Template> templateMap = templateMapper.selectBatchIds(questionIds).stream()
				.collect(Collectors.toMap(Template::getId, t -> t));
		return bindings.stream().map(binding -> templateMap.get(binding.getQuestionId()))
				.filter(Objects::nonNull).map(templateViewMapper::toView).collect(Collectors.toList());
	}

}
