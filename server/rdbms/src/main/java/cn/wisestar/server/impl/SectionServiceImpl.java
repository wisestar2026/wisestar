package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.SectionQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.mapper.SectionViewMapper;
import cn.wisestar.server.domain.mapper.TemplateViewMapper;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionQuestion;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionQuestionMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.SectionService;
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
 * 小节管理业务实现（知识管理板块三级维度）。
 *
 * <p>【被谁调用】SectionApi（管理端小节管理）。</p>
 * <p>【依赖什么】SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper/
 * SectionQuestionMapper/TemplateMapper（BaseMapper CRUD）、
 * SectionViewMapper/TemplateViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】SectionApi → SectionServiceImpl → SectionMapper（t_section）；列表返回时经
 * KnowledgePointMapper 统计各小节知识点数、经 SectionQuestionMapper 统计已绑定测试题数；
 * 小节测试题目经 t_section_question 关联题目库（t_template），全量替换式保存；
 * 删除时级联逻辑删除其下知识点、知识点题目绑定与本小节的测试题目绑定。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SectionServiceImpl extends BaseService<SectionMapper, Section> implements SectionService {

	private final SectionViewMapper sectionViewMapper;

	private final TemplateViewMapper templateViewMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final SectionQuestionMapper sectionQuestionMapper;

	private final TemplateMapper templateMapper;

	/**
	 * 小节列表（按章节过滤，sort 升序），并统计各小节下知识点数与已绑定测试题数。
	 */
	@Override
	public List<SectionView> listSections(SectionRequest query) {
		List<Section> sections = list(Wrappers.<Section>lambdaQuery()
				.eq(hasText(query.getChapterId()), Section::getChapterId, query.getChapterId())
				.orderByAsc(Section::getSort));
		List<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toList());
		Map<String, Long> knowledgePointCountMap = sectionIds.isEmpty() ? Collections.emptyMap()
				: knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
						.select(KnowledgePoint::getSectionId).in(KnowledgePoint::getSectionId, sectionIds)).stream()
				.collect(Collectors.groupingBy(KnowledgePoint::getSectionId, Collectors.counting()));
		Map<String, Long> questionCountMap = sectionIds.isEmpty() ? Collections.emptyMap()
				: sectionQuestionMapper.selectList(Wrappers.<SectionQuestion>lambdaQuery()
						.select(SectionQuestion::getSectionId).in(SectionQuestion::getSectionId, sectionIds)).stream()
				.collect(Collectors.groupingBy(SectionQuestion::getSectionId, Collectors.counting()));
		return sections.stream().map(section -> {
			SectionView view = sectionViewMapper.toView(section);
			view.setKnowledgePointCount(knowledgePointCountMap.getOrDefault(section.getId(), 0L));
			view.setQuestionCount(questionCountMap.getOrDefault(section.getId(), 0L));
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 新增小节。
	 */
	@Override
	public String addSection(SectionRequest request) {
		Section section = sectionViewMapper.fromRequest(request);
		save(section);
		return section.getId();
	}

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 */
	@Override
	public void updateSection(SectionRequest request) {
		updateById(sectionViewMapper.fromRequest(request));
	}

	/**
	 * 删除小节（级联逻辑删除其下知识点、知识点-题目绑定与本小节的测试题目绑定）。
	 */
	@Override
	public void deleteSection(SectionRequest request) {
		sectionQuestionMapper.delete(Wrappers.<SectionQuestion>lambdaQuery()
				.eq(SectionQuestion::getSectionId, request.getId()));
		List<String> knowledgePointIds = knowledgePointMapper.selectList(Wrappers.<KnowledgePoint>lambdaQuery()
				.select(KnowledgePoint::getId).eq(KnowledgePoint::getSectionId, request.getId())).stream()
				.map(KnowledgePoint::getId).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(knowledgePointIds)) {
			knowledgePointQuestionMapper.delete(Wrappers.<KnowledgePointQuestion>lambdaQuery()
					.in(KnowledgePointQuestion::getKnowledgePointId, knowledgePointIds));
		}
		knowledgePointMapper.delete(Wrappers.<KnowledgePoint>lambdaQuery()
				.eq(KnowledgePoint::getSectionId, request.getId()));
		removeById(request.getId());
	}

	/**
	 * 保存小节-测试题目绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
	 */
	@Override
	public void saveQuestions(SectionQuestionRequest request) {
		sectionQuestionMapper.delete(Wrappers.<SectionQuestion>lambdaQuery()
				.eq(SectionQuestion::getSectionId, request.getSectionId()));
		if (CollectionUtils.isEmpty(request.getQuestionIds())) {
			return;
		}
		request.getQuestionIds().stream().filter(Objects::nonNull).distinct().forEach(questionId -> {
			SectionQuestion binding = new SectionQuestion();
			binding.setSectionId(request.getSectionId());
			binding.setQuestionId(questionId);
			sectionQuestionMapper.insert(binding);
		});
	}

	/**
	 * 查询小节已绑定的测试题目列表（题目库 t_template 数据，保持绑定顺序）。
	 */
	@Override
	public List<TemplateView> listQuestions(String sectionId) {
		List<SectionQuestion> bindings = sectionQuestionMapper.selectList(
				Wrappers.<SectionQuestion>lambdaQuery()
						.eq(SectionQuestion::getSectionId, sectionId)
						.orderByAsc(SectionQuestion::getCreateAt));
		if (CollectionUtils.isEmpty(bindings)) {
			return Collections.emptyList();
		}
		List<String> questionIds = bindings.stream().map(SectionQuestion::getQuestionId)
				.collect(Collectors.toList());
		Map<String, Template> templateMap = templateMapper.selectBatchIds(questionIds).stream()
				.collect(Collectors.toMap(Template::getId, t -> t));
		return bindings.stream().map(binding -> templateMap.get(binding.getQuestionId()))
				.filter(Objects::nonNull).map(templateViewMapper::toView).collect(Collectors.toList());
	}

}
