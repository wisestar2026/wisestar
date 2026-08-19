package cn.wisestar.server.impl;

import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.ChapterImportRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.domain.dto.knowledge.ImportResultView;
import cn.wisestar.server.domain.mapper.ChapterViewMapper;
import cn.wisestar.server.domain.mapper.RepoViewMapper;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.ChapterRepo;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Repo;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.SectionRepo;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterRepoMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.ChapterService;
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
import java.math.BigDecimal;
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
 * 章节管理业务实现（知识管理板块二级维度）。
 *
 * <p>【被谁调用】ChapterApi（管理端章节管理）。</p>
 * <p>【依赖什么】ChapterMapper/SectionMapper/KnowledgePointMapper/KnowledgePointQuestionMapper/
 * ChapterRepoMapper/SectionRepoMapper/RepoMapper（BaseMapper CRUD）、
 * ChapterViewMapper/RepoViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】ChapterApi → ChapterServiceImpl → ChapterMapper（t_chapter）；列表返回时经
 * SectionMapper 统计各章节小节数、经 ChapterRepoMapper 统计已绑定题库数；
 * 章节题库经 t_chapter_repo 关联题库管理（t_repo），全量替换式保存；
 * 删除时级联逻辑删除其下小节/知识点/知识点题目绑定、章节题库绑定与各小节题库绑定。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ChapterServiceImpl extends BaseService<ChapterMapper, Chapter> implements ChapterService {

	private final ChapterViewMapper chapterViewMapper;

	private final RepoViewMapper repoViewMapper;

	private final SectionMapper sectionMapper;

	private final KnowledgePointMapper knowledgePointMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final ChapterRepoMapper chapterRepoMapper;

	private final SectionRepoMapper sectionRepoMapper;

	private final RepoMapper repoMapper;

	private final SubjectMapper subjectMapper;

	/**
	 * 章节列表（按学科过滤，sort 升序），并统计各章节下小节数与已绑定题库数。
	 */
	@Override
	public List<ChapterView> listChapters(ChapterRequest query) {
		List<Chapter> chapters = list(Wrappers.<Chapter>lambdaQuery()
				.eq(hasText(query.getSubjectId()), Chapter::getSubjectId, query.getSubjectId())
				.orderByAsc(Chapter::getSort));
		// 一次查出相关小节与章节题库绑定，按 chapterId 分组计数，避免逐条 N+1 查询
		List<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
		Map<String, Long> sectionCountMap = chapterIds.isEmpty() ? Collections.emptyMap() : sectionMapper.selectList(
				Wrappers.<Section>lambdaQuery().select(Section::getChapterId)
						.in(Section::getChapterId, chapterIds)).stream()
				.collect(Collectors.groupingBy(Section::getChapterId, Collectors.counting()));
		Map<String, Long> repoCountMap = chapterIds.isEmpty() ? Collections.emptyMap() : chapterRepoMapper.selectList(
				Wrappers.<ChapterRepo>lambdaQuery().select(ChapterRepo::getChapterId)
						.in(ChapterRepo::getChapterId, chapterIds)).stream()
				.collect(Collectors.groupingBy(ChapterRepo::getChapterId, Collectors.counting()));
		return chapters.stream().map(chapter -> {
			ChapterView view = chapterViewMapper.toView(chapter);
			view.setSectionCount(sectionCountMap.getOrDefault(chapter.getId(), 0L));
			view.setRepoCount(repoCountMap.getOrDefault(chapter.getId(), 0L));
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
	 * 批量导入章节（Excel：章节名/图标/排序；按 subjectId+name 去重）。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ImportResultView importChapters(ChapterImportRequest request) {
		Map<String, String> subjectCache = subjectMapper.selectList(null).stream()
				.collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));
		Set<String> existing = this.baseMapper.selectList(null).stream()
				.map(c -> c.getSubjectId() + "|" + c.getName()).collect(Collectors.toSet());
		AtomicInteger imported = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		List<Chapter> toSave = new ArrayList<>();
		try (InputStream is = request.getFile().getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
			wb.getSheets().forEach(sheet -> {
				try (Stream<Row> rows = sheet.openStream()) {
					rows.forEach(r -> {
						if (r.getRowNum() == 1) {
							return; // 跳过表头
						}
						String subjectName = r.getCellText(0);
						String name = r.getCellText(1);
						if (!hasText(subjectName) || !hasText(name)) {
							skipped.incrementAndGet();
							return;
						}
						String subjectId = subjectCache.get(subjectName.trim());
						String key = (subjectId == null ? "?" : subjectId) + "|" + name.trim();
						if (subjectId == null || existing.contains(key)) {
							skipped.incrementAndGet();
							return;
						}
						existing.add(key);
						Chapter chapter = new Chapter();
						chapter.setSubjectId(subjectId);
						chapter.setName(name.trim());
						String icon = r.getCellText(2);
						chapter.setIcon(hasText(icon) ? icon : null);
						chapter.setSort(r.getCellAsNumber(3).orElse(BigDecimal.ONE).intValue());
						toSave.add(chapter);
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
		return new ImportResultView(imported.get(), skipped.get());
	}

	/**
	 * 更新章节。
	 */
	@Override
	public void updateChapter(ChapterRequest request) {
		updateById(chapterViewMapper.fromRequest(request));
	}

	/**
	 * 删除章节（级联逻辑删除其下小节、知识点、知识点-题目绑定、章节题库绑定与各小节题库绑定）。
	 */
	@Override
	public void deleteChapter(ChapterRequest request) {
		chapterRepoMapper.delete(Wrappers.<ChapterRepo>lambdaQuery()
				.eq(ChapterRepo::getChapterId, request.getId()));
		List<Section> sections = sectionMapper.selectList(Wrappers.<Section>lambdaQuery()
				.select(Section::getId).eq(Section::getChapterId, request.getId()));
		List<String> sectionIds = sections.stream().map(Section::getId).collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(sectionIds)) {
			sectionRepoMapper.delete(Wrappers.<SectionRepo>lambdaQuery()
					.in(SectionRepo::getSectionId, sectionIds));
		}
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
	 * 保存章节-题库绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
	 */
	@Override
	public void saveRepos(ChapterRepoRequest request) {
		chapterRepoMapper.delete(Wrappers.<ChapterRepo>lambdaQuery()
				.eq(ChapterRepo::getChapterId, request.getChapterId()));
		if (CollectionUtils.isEmpty(request.getRepoIds())) {
			return;
		}
		request.getRepoIds().stream().filter(Objects::nonNull).distinct().forEach(repoId -> {
			ChapterRepo binding = new ChapterRepo();
			binding.setChapterId(request.getChapterId());
			binding.setRepoId(repoId);
			chapterRepoMapper.insert(binding);
		});
	}

	/**
	 * 查询章节已绑定的题库列表（题库管理 t_repo 数据，保持绑定顺序）。
	 */
	@Override
	public List<RepoView> listRepos(String chapterId) {
		List<ChapterRepo> bindings = chapterRepoMapper.selectList(
				Wrappers.<ChapterRepo>lambdaQuery()
						.eq(ChapterRepo::getChapterId, chapterId)
						.orderByAsc(ChapterRepo::getCreateAt));
		if (CollectionUtils.isEmpty(bindings)) {
			return Collections.emptyList();
		}
		List<String> repoIds = bindings.stream().map(ChapterRepo::getRepoId)
				.collect(Collectors.toList());
		Map<String, Repo> repoMap = repoMapper.selectBatchIds(repoIds).stream()
				.collect(Collectors.toMap(Repo::getId, r -> r));
		return bindings.stream().map(binding -> repoMap.get(binding.getRepoId()))
				.filter(Objects::nonNull).map(repoViewMapper::toView).collect(Collectors.toList());
	}

}
