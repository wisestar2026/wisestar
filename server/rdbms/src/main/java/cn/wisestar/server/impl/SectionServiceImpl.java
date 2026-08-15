package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.SectionRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.mapper.RepoViewMapper;
import cn.wisestar.server.domain.mapper.SectionViewMapper;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
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
 * SectionRepoMapper/RepoMapper（BaseMapper CRUD）、
 * SectionViewMapper/RepoViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】SectionApi → SectionServiceImpl → SectionMapper（t_section）；列表返回时经
 * KnowledgePointMapper 统计各小节知识点数、经 SectionRepoMapper 统计已绑定题库数；
 * 小节题库经 t_section_repo 关联题库管理（t_repo），全量替换式保存；
 * 删除时级联逻辑删除其下知识点、知识点题目绑定与本小节的题库绑定。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SectionServiceImpl extends BaseService<SectionMapper, Section> implements SectionService {

	private final SectionViewMapper sectionViewMapper;

	private final RepoViewMapper repoViewMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final SectionRepoMapper sectionRepoMapper;

	private final RepoMapper repoMapper;

	/**
	 * 小节列表（按章节过滤，sort 升序），并统计各小节下知识点数与已绑定题库数。
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
		Map<String, Long> repoCountMap = sectionIds.isEmpty() ? Collections.emptyMap()
				: sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
						.select(SectionRepo::getSectionId).in(SectionRepo::getSectionId, sectionIds)).stream()
				.collect(Collectors.groupingBy(SectionRepo::getSectionId, Collectors.counting()));
		return sections.stream().map(section -> {
			SectionView view = sectionViewMapper.toView(section);
			view.setKnowledgePointCount(knowledgePointCountMap.getOrDefault(section.getId(), 0L));
			view.setRepoCount(repoCountMap.getOrDefault(section.getId(), 0L));
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
	 * 删除小节（级联逻辑删除其下知识点、知识点-题目绑定与本小节的题库绑定）。
	 */
	@Override
	public void deleteSection(SectionRequest request) {
		sectionRepoMapper.delete(Wrappers.<SectionRepo>lambdaQuery()
				.eq(SectionRepo::getSectionId, request.getId()));
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
	 * 保存小节-题库绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
	 */
	@Override
	public void saveRepos(SectionRepoRequest request) {
		sectionRepoMapper.delete(Wrappers.<SectionRepo>lambdaQuery()
				.eq(SectionRepo::getSectionId, request.getSectionId()));
		if (CollectionUtils.isEmpty(request.getRepoIds())) {
			return;
		}
		request.getRepoIds().stream().filter(Objects::nonNull).distinct().forEach(repoId -> {
			SectionRepo binding = new SectionRepo();
			binding.setSectionId(request.getSectionId());
			binding.setRepoId(repoId);
			sectionRepoMapper.insert(binding);
		});
	}

	/**
	 * 查询小节已绑定的题库列表（题库管理 t_repo 数据，保持绑定顺序）。
	 */
	@Override
	public List<RepoView> listRepos(String sectionId) {
		List<SectionRepo> bindings = sectionRepoMapper.selectList(
				Wrappers.<SectionRepo>lambdaQuery()
						.eq(SectionRepo::getSectionId, sectionId)
						.orderByAsc(SectionRepo::getCreateAt));
		if (CollectionUtils.isEmpty(bindings)) {
			return Collections.emptyList();
		}
		List<String> repoIds = bindings.stream().map(SectionRepo::getRepoId)
				.collect(Collectors.toList());
		Map<String, Repo> repoMap = repoMapper.selectBatchIds(repoIds).stream()
				.collect(Collectors.toMap(Repo::getId, r -> r));
		return bindings.stream().map(binding -> repoMap.get(binding.getRepoId()))
				.filter(Objects::nonNull).map(repoViewMapper::toView).collect(Collectors.toList());
	}

}
