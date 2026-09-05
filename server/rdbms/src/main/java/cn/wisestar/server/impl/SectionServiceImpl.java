package cn.wisestar.server.impl;

import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.SectionImportRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.domain.dto.knowledge.ImportResultView;
import cn.wisestar.server.domain.mapper.RepoViewMapper;
import cn.wisestar.server.domain.mapper.SectionViewMapper;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.SectionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	private final SubjectMapper subjectMapper;

	private final ChapterMapper chapterMapper;

	/**
	 * 小节列表（chapterId 可选，年级/学期可选等值过滤，sort 升序），
	 * 并统计各小节下知识点数与已绑定题库数。
	 */
	@Override
	public List<SectionView> listSections(SectionRequest query) {
		List<Section> sections = list(Wrappers.<Section>lambdaQuery()
				.eq(hasText(query.getChapterId()), Section::getChapterId, query.getChapterId())
				.eq(hasText(query.getGrade()), Section::getGrade, query.getGrade())
				.eq(hasText(query.getTerm()), Section::getTerm, query.getTerm())
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
	 * 新增小节（sort 为空时自动追加到所属章节现有最大 sort 之后）。
	 */
	@Override
	public String addSection(SectionRequest request) {
		Section section = sectionViewMapper.fromRequest(request);
		fillDefaults(section);
		save(section);
		return section.getId();
	}

	/** 新增小节时补齐系统默认值：sort 为空则追加到所属章节现有最大 sort 之后。 */
	private void fillDefaults(Section section) {
		if (section.getSort() == null) {
			Integer maxSort = getBaseMapper().selectList(Wrappers.<Section>lambdaQuery()
					.select(Section::getSort)
					.eq(Section::getChapterId, section.getChapterId())
					.orderByDesc(Section::getSort)
					.last("limit 1"))
					.stream().findFirst().map(Section::getSort).orElse(null);
			section.setSort(maxSort == null ? 1 : maxSort + 1);
		}
	}

	/**
	 * 批量导入小节（Excel：学科名/章节名/小节名/年级(选填)/学期(选填)；
	 * 排序不参与导入，按所属章节自动追加；按 chapterId+name 去重）。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ImportResultView importSections(SectionImportRequest request) {
		Map<String, String> subjectCache = subjectMapper.selectList(null).stream()
				.collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));
		Map<String, String> chapterCache = chapterMapper.selectList(null).stream()
				.collect(Collectors.toMap(c -> c.getSubjectId() + "|" + c.getName(), Chapter::getId, (a, b) -> a));
		List<Section> allSections = this.baseMapper.selectList(null);
		Set<String> existing = allSections.stream()
				.map(s -> s.getChapterId() + "|" + s.getName()).collect(Collectors.toSet());
		// 每个章节的「下一个可用排序号」，初始 = 现有最大 sort + 1；同一章节逐行递增，跨章节互不影响
		Map<String, AtomicInteger> nextSortByChapter = allSections.stream()
				.filter(s -> s.getSort() != null && s.getChapterId() != null)
				.collect(Collectors.groupingBy(Section::getChapterId,
						Collectors.collectingAndThen(
								Collectors.maxBy(java.util.Comparator.comparingInt(Section::getSort)),
								max -> new AtomicInteger(max.get().getSort() + 1))));
		AtomicInteger imported = new AtomicInteger(0);
		AtomicInteger missingRequired = new AtomicInteger(0);
		AtomicInteger sectionNotFound = new AtomicInteger(0);
		AtomicInteger duplicate = new AtomicInteger(0);
		List<Section> toSave = new ArrayList<>();
		try (InputStream is = request.getFile().getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
			wb.getSheets().forEach(sheet -> {
				try (Stream<Row> rows = sheet.openStream()) {
					rows.forEach(r -> {
						if (r.getRowNum() == 1) {
							return; // 跳过表头
						}
						String subjectName = cellText(r, 0);
						String chapterName = cellText(r, 1);
						String name = cellText(r, 2);
						if (!hasText(subjectName) || !hasText(chapterName) || !hasText(name)) {
							missingRequired.incrementAndGet();
							return;
						}
						String subjectId = subjectCache.get(subjectName.trim());
						String chapterId = subjectId == null ? null : chapterCache.get(subjectId + "|" + chapterName.trim());
						String key = (chapterId == null ? "?" : chapterId) + "|" + name.trim();
						if (chapterId == null) {
							sectionNotFound.incrementAndGet();
							return;
						}
						if (existing.contains(key)) {
							duplicate.incrementAndGet();
							return;
						}
						existing.add(key);
						Section section = new Section();
						section.setChapterId(chapterId);
						section.setName(name.trim());
						section.setGrade(normalizeBlank(cellText(r, 3)));
						section.setTerm(normalizeBlank(cellText(r, 4)));
						int nextSort = nextSortByChapter
								.computeIfAbsent(chapterId, s -> new AtomicInteger(1)).getAndIncrement();
						section.setSort(nextSort);
						toSave.add(section);
						if (toSave.size() >= 500) {
							saveBatch(toSave);
							imported.addAndGet(toSave.size());
							toSave.clear();
						}
					});
				}
				catch (Exception e) {
					throw new ValidationException("Excel 文件无法解析，请使用 Excel/WPS 导出的 .xlsx 文件");
				}
			});
		}
		catch (Exception e) {
			throw new ValidationException("Excel 文件无法解析，请使用 Excel/WPS 导出的 .xlsx 文件");
		}
		if (!toSave.isEmpty()) {
			saveBatch(toSave);
			imported.addAndGet(toSave.size());
		}
		ImportResultView result = new ImportResultView(imported.get(),
				missingRequired.get() + sectionNotFound.get() + duplicate.get());
		result.setMissingRequired(missingRequired.get());
		result.setSectionNotFound(sectionNotFound.get());
		result.setDuplicate(duplicate.get());
		return result;
	}

	/** 读取行中指定列文本（缺列/空单元格返回空串，不抛异常）。 */
	private String cellText(Row row, int index) {
		return row.getOptionalCell(index).map(cell -> {
			try {
				return cell.getText();
			}
			catch (Exception e) {
				return "";
			}
		}).orElse("");
	}

	/** 去掉前后空白；空文本归一为 null。 */
	private String normalizeBlank(String text) {
		return hasText(text) ? text.trim() : null;
	}

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 * grade/term 支持清空：请求显式传空串表达清除，统一落 null（updateById 忽略 null，
	 * 故清空需在 updateById 之外显式覆盖；未传的调用方（如仅存内容/练习设置）不受影响）。
	 */
	@Override
	public void updateSection(SectionRequest request) {
		Section m = sectionViewMapper.fromRequest(request);
		boolean touchGrade = m.getGrade() != null;
		boolean touchTerm = m.getTerm() != null;
		if (touchGrade) {
			m.setGrade(null);
		}
		if (touchTerm) {
			m.setTerm(null);
		}
		updateById(m);
		if (touchGrade || touchTerm) {
			update(Wrappers.<Section>lambdaUpdate()
					.eq(Section::getId, request.getId())
					.set(touchGrade, Section::getGrade, normalizeBlank(request.getGrade()))
					.set(touchTerm, Section::getTerm, normalizeBlank(request.getTerm())));
		}
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
