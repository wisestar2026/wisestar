package cn.wisestar.server.impl;

import cn.wisestar.server.core.constant.ErrorCode;
import cn.wisestar.server.core.exception.ErrorCodeException;
import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.core.uitls.ContextHelper;
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
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.ChapterRepoMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.RepoMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.mapper.SectionRepoMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.ChapterService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.validation.ValidationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
 * SectionMapper 统计各章节小节数、经 SectionRepoMapper 统计章节下小节直绑练习去重数
 * （业务规则：练习仅支持绑定到小节，章节自身不直接绑定练习）；
 * 删除时级联逻辑删除其下小节/知识点/知识点题目绑定、章节级残留绑定与各小节题库绑定。</p>
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

	private final TemplateMapper templateMapper;

	/**
	 * 章节列表（按学科/年级/学期/版本过滤，sort 升序），并统计各章节下小节数
	 * 与练习数（章节自身不再直接绑定练习，练习数 = 该章节下各小节直绑练习的去重数）。
	 */
	@Override
	public List<ChapterView> listChapters(ChapterRequest query) {
		List<Chapter> chapters = list(Wrappers.<Chapter>lambdaQuery()
				.eq(hasText(query.getSubjectId()), Chapter::getSubjectId, query.getSubjectId())
				.eq(hasText(query.getGrade()), Chapter::getGrade, query.getGrade())
				.eq(hasText(query.getTerm()), Chapter::getTerm, query.getTerm())
				.eq(hasText(query.getVersion()), Chapter::getVersion, query.getVersion())
				.orderByAsc(Chapter::getSort));
		// 一次查出相关小节（含所属章节），按 chapterId 分组统计小节数
		List<String> chapterIds = chapters.stream().map(Chapter::getId).collect(Collectors.toList());
		List<Section> chapterSections = chapterIds.isEmpty() ? Collections.emptyList() : sectionMapper.selectList(
				Wrappers.<Section>lambdaQuery().select(Section::getId, Section::getChapterId)
						.in(Section::getChapterId, chapterIds));
		Map<String, Long> sectionCountMap = chapterSections.stream()
				.filter(s -> s.getChapterId() != null)
				.collect(Collectors.groupingBy(Section::getChapterId, Collectors.counting()));
		// 小节 → 章节 归属映射；小节直绑练习按章节去重计数（不再读 t_chapter_repo）
		Map<String, String> chapterIdBySection = chapterSections.stream()
				.filter(s -> s.getChapterId() != null)
				.collect(Collectors.toMap(Section::getId, Section::getChapterId, (a, b) -> a));
		Map<String, Long> repoCountMap = new HashMap<>();
		if (!chapterIdBySection.isEmpty()) {
			Map<String, Set<String>> reposByChapter = new HashMap<>();
			sectionRepoMapper.selectList(Wrappers.<SectionRepo>lambdaQuery()
							.select(SectionRepo::getSectionId, SectionRepo::getRepoId)
							.in(SectionRepo::getSectionId, chapterIdBySection.keySet()))
					.forEach(binding -> {
						String chapterId = chapterIdBySection.get(binding.getSectionId());
						if (chapterId != null && binding.getRepoId() != null) {
							reposByChapter.computeIfAbsent(chapterId, k -> new HashSet<>()).add(binding.getRepoId());
						}
					});
			reposByChapter.forEach((chapterId, repos) -> repoCountMap.put(chapterId, (long) repos.size()));
		}
		return chapters.stream().map(chapter -> {
			ChapterView view = chapterViewMapper.toView(chapter);
			view.setSectionCount(sectionCountMap.getOrDefault(chapter.getId(), 0L));
			view.setRepoCount(repoCountMap.getOrDefault(chapter.getId(), 0L));
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 新增章节（图标/排序走系统默认：图标缺省 📖，排序自动追加到该学科末尾）。
	 */
	@Override
	public String addChapter(ChapterRequest request) {
		Chapter chapter = chapterViewMapper.fromRequest(request);
		fillDefaults(chapter);
		save(chapter);
		return chapter.getId();
	}

	/**
	 * 新增/导入章节时补齐系统默认值：
	 * icon 为空置为默认 emoji（📖），sort 为空则追加到所属学科现有最大 sort 之后。
	 */
	private void fillDefaults(Chapter chapter) {
		if (!hasText(chapter.getIcon())) {
			chapter.setIcon("📖");
		}
		if (chapter.getSort() == null) {
			Integer maxSort = getBaseMapper().selectList(Wrappers.<Chapter>lambdaQuery()
					.select(Chapter::getSort)
					.eq(Chapter::getSubjectId, chapter.getSubjectId())
					.orderByDesc(Chapter::getSort)
					.last("limit 1"))
					.stream().findFirst().map(Chapter::getSort).orElse(null);
			chapter.setSort(maxSort == null ? 1 : maxSort + 1);
		}
	}

	/**
	 * 批量导入章节（Excel：学科名/章节名称/年级(选填)/学期(选填)/版本(选填)；
	 * 按学科名匹配 t_subject.name 定位归属，学科+章节名重名跳过）。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ImportResultView importChapters(ChapterImportRequest request) {
		Map<String, String> subjectCache = subjectMapper.selectList(null).stream()
				.collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));
		List<Chapter> existingChapters = this.baseMapper.selectList(null);
		Set<String> existing = existingChapters.stream()
				.map(c -> c.getSubjectId() + "|" + c.getName()).collect(Collectors.toSet());
		// 每个学科的「下一个可用排序号」，初始 = 现有最大 sort + 1；同一学科逐行递增，跨学科互不影响
		Map<String, AtomicInteger> nextSortBySubject = existingChapters.stream()
				.filter(c -> c.getSort() != null && c.getSubjectId() != null)
				.collect(Collectors.groupingBy(Chapter::getSubjectId,
						Collectors.collectingAndThen(
								Collectors.maxBy(java.util.Comparator.comparingInt(Chapter::getSort)),
								max -> new AtomicInteger(max.get().getSort() + 1))));
		AtomicInteger imported = new AtomicInteger(0);
		AtomicInteger missingRequired = new AtomicInteger(0);
		AtomicInteger sectionNotFound = new AtomicInteger(0);
		AtomicInteger duplicate = new AtomicInteger(0);
		List<Chapter> toSave = new ArrayList<>();
		try (InputStream is = request.getFile().getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
			wb.getSheets().forEach(sheet -> {
				try (Stream<Row> rows = sheet.openStream()) {
					rows.forEach(r -> {
						if (r.getRowNum() == 1) {
							return; // 跳过表头
						}
						String subjectName = cellText(r, 0);
						String name = cellText(r, 1);
						if (!hasText(subjectName) || !hasText(name)) {
							missingRequired.incrementAndGet();
							return;
						}
						String subjectId = subjectCache.get(subjectName.trim());
						String key = (subjectId == null ? "?" : subjectId) + "|" + name.trim();
						if (subjectId == null) {
							sectionNotFound.incrementAndGet();
							return;
						}
						if (existing.contains(key)) {
							duplicate.incrementAndGet();
							return;
						}
						existing.add(key);
						Chapter chapter = new Chapter();
						chapter.setSubjectId(subjectId);
						chapter.setName(name.trim());
						chapter.setGrade(normalizeBlank(cellText(r, 2)));
						chapter.setTerm(normalizeBlank(cellText(r, 3)));
						chapter.setVersion(normalizeBlank(cellText(r, 4)));
						int nextSort = nextSortBySubject
								.computeIfAbsent(subjectId, s -> new AtomicInteger(1)).getAndIncrement();
						chapter.setSort(nextSort);
						fillDefaults(chapter);
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
	 * 更新章节（仅更新管理端可维护列：名称/年级/学期/版本/排序；
	 * 图标由系统默认维护，不随本次更新改动；排序留空时不改动原值）。
	 */
	@Override
	public void updateChapter(ChapterRequest request) {
		Chapter chapter = chapterViewMapper.fromRequest(request);
		update(Wrappers.<Chapter>lambdaUpdate()
				.eq(Chapter::getId, request.getId())
				.set(Chapter::getName, chapter.getName())
				.set(Chapter::getGrade, chapter.getGrade())
				.set(Chapter::getTerm, chapter.getTerm())
				.set(Chapter::getVersion, chapter.getVersion())
				.set(request.getSort() != null, Chapter::getSort, chapter.getSort()));
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
	 * 保存章节-题库绑定（已停用）。
	 *
	 * <p>业务规则：练习仅支持绑定到小节，章节不再作为练习的绑定对象。
	 * 已迁移的历史章节绑定（t_chapter_repo 存量数据）已展开写入其下小节。
	 * 本接口调用一律拒绝，前端不再暴露章节级「绑定练习」入口。</p>
	 */
	@Override
	public void saveRepos(ChapterRepoRequest request) {
		throw new ErrorCodeException(ErrorCode.ValidationError,
				"练习仅支持绑定到小节，章节不能直接绑定练习。请在「小节管理 / 习题列表」中为章节下的小节绑定练习。");
	}

	/**
	 * 查询章节已绑定的题库列表（已停用，恒为空）。
	 *
	 * <p>章节不再支持直接绑定题库（历史数据已迁移到小节），本方法仅保留兼容读取。</p>
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
		// 一次统计各练习题目数（t_template.repo_id 分组计数），供习题列表展示题量
		Map<String, Long> totalMap = templateMapper.selectList(Wrappers.<Template>lambdaQuery()
						.select(Template::getRepoId).in(Template::getRepoId, repoIds)).stream()
				.collect(Collectors.groupingBy(Template::getRepoId, Collectors.counting()));
		return bindings.stream().map(binding -> repoMap.get(binding.getRepoId()))
				.filter(Objects::nonNull).map(repo -> {
					RepoView view = repoViewMapper.toView(repo);
					view.setTotal(totalMap.getOrDefault(repo.getId(), 0L));
					return view;
				}).collect(Collectors.toList());
	}

	/**
	 * 导出章节列表为 Excel（列：章节名称/年级/学期/版本/小节数/练习数），
	 * 与列表页过滤口径一致（subjectId/grade/term/version 可选），附件下载。
	 */
	@Override
	@SneakyThrows
	public void exportChapters(ChapterRequest query) {
		List<ChapterView> views = listChapters(query);
		String fileName = "章节导出_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
		ContextHelper.getCurrentHttpResponse()
				.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		ContextHelper.getCurrentHttpResponse()
				.setHeader("Content-Disposition", "attachment; filename="
						+ java.net.URLEncoder.encode(fileName, "UTF-8"));

		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			Workbook workbook = new Workbook(baos, "chapters", "1.0");
			Worksheet sheet = workbook.newWorksheet("章节");
			String[] headers = { "章节名称", "年级", "学期", "版本", "小节数", "练习数" };
			for (int c = 0; c < headers.length; c++) {
				sheet.value(0, c, headers[c]);
			}
			for (int r = 0; r < views.size(); r++) {
				ChapterView v = views.get(r);
				sheet.value(r + 1, 0, textOf(v.getName()));
				sheet.value(r + 1, 1, textOf(v.getGrade()));
				sheet.value(r + 1, 2, textOf(v.getTerm()));
				sheet.value(r + 1, 3, textOf(v.getVersion()));
				sheet.value(r + 1, 4, (Number) (v.getSectionCount() == null ? 0L : v.getSectionCount()));
				sheet.value(r + 1, 5, (Number) (v.getRepoCount() == null ? 0L : v.getRepoCount()));
			}
			workbook.finish();
			ContextHelper.getCurrentHttpResponse().getOutputStream().write(baos.toByteArray());
		}
	}

	/** null 安全转字符串（导出单元格用，空值写空串）。 */
	private String textOf(String text) {
		return text == null ? "" : text;
	}

}
