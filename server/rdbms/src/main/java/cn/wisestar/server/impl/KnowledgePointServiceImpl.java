package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.exception.InternalServerError;
import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.ImportResultView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointImportRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuery;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.domain.mapper.KnowledgePointViewMapper;
import cn.wisestar.server.domain.mapper.TemplateViewMapper;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.KnowledgePoint;
import cn.wisestar.server.domain.model.KnowledgePointQuestion;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.KnowledgePointMapper;
import cn.wisestar.server.mapper.KnowledgePointQuestionMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.KnowledgePointService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
 * 知识点管理业务实现（知识管理板块最小学习单元）。
 *
 * <p>【被谁调用】KnowledgePointApi（管理端知识点管理）。</p>
 * <p>【依赖什么】KnowledgePointMapper/SectionMapper/ChapterMapper/SubjectMapper/
 * KnowledgePointQuestionMapper/TemplateMapper（BaseMapper CRUD）、
 * KnowledgePointViewMapper/TemplateViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】KnowledgePointApi → KnowledgePointServiceImpl → KnowledgePointMapper
 * （t_knowledge_point）；分页列表回填三级归属名称（学科/章节/小节）与绑定题目数；
 * 题目绑定经 t_knowledge_point_question 关联题目库（t_template），全量替换式保存。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class KnowledgePointServiceImpl extends BaseService<KnowledgePointMapper, KnowledgePoint>
		implements KnowledgePointService {

	private final KnowledgePointViewMapper knowledgePointViewMapper;

	private final TemplateViewMapper templateViewMapper;

	private final SectionMapper sectionMapper;

	private final ChapterMapper chapterMapper;

	private final SubjectMapper subjectMapper;

	private final KnowledgePointQuestionMapper knowledgePointQuestionMapper;

	private final TemplateMapper templateMapper;

	/**
	 * 知识点分页列表（三级下拉筛选：学科 → 章节 → 小节，条件均可选）。
	 *
	 * <p>【筛选逻辑】sectionId 优先；未传时按 chapterId 找到该章节下所有小节；
	 * 再未传时按 subjectId 找到该学科下所有小节；都未传则全量分页。
	 * 返回视图回填学科/章节/小节名称与已绑定题目数。</p>
	 */
	@Override
	public PaginationResponse<KnowledgePointView> listKnowledgePoints(KnowledgePointQuery query) {
		List<String> sectionIds = resolveSectionIds(query);
		Page<KnowledgePoint> page = pageByQuery(query, Wrappers.<KnowledgePoint>lambdaQuery()
				.in(!sectionIds.isEmpty(), KnowledgePoint::getSectionId, sectionIds)
				.orderByAsc(KnowledgePoint::getSort)
				.orderByDesc(KnowledgePoint::getCreateAt));
		List<KnowledgePointView> views = fillHierarchyAndQuestionCount(page.getRecords());
		return new PaginationResponse<>(page.getTotal(), views);
	}

	/**
	 * 新增知识点。
	 */
	@Override
	public String addKnowledgePoint(KnowledgePointRequest request) {
		KnowledgePoint knowledgePoint = knowledgePointViewMapper.fromRequest(request);
		save(knowledgePoint);
		return knowledgePoint.getId();
	}

	/**
	 * 批量导入知识点（Excel：知识点名/排序；按 sectionId+name 去重）。
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public ImportResultView importKnowledgePoints(KnowledgePointImportRequest request) {
		Map<String, String> subjectCache = subjectMapper.selectList(null).stream()
				.collect(Collectors.toMap(Subject::getName, Subject::getId, (a, b) -> a));
		Map<String, String> chapterCache = chapterMapper.selectList(null).stream()
				.collect(Collectors.toMap(c -> c.getSubjectId() + "|" + c.getName(), Chapter::getId, (a, b) -> a));
		Map<String, String> sectionCache = sectionMapper.selectList(null).stream()
				.collect(Collectors.toMap(s -> s.getChapterId() + "|" + s.getName(), Section::getId, (a, b) -> a));
		Set<String> existing = this.baseMapper.selectList(null).stream()
				.map(k -> k.getSectionId() + "|" + k.getName()).collect(Collectors.toSet());
		AtomicInteger imported = new AtomicInteger(0);
		AtomicInteger skipped = new AtomicInteger(0);
		List<KnowledgePoint> toSave = new ArrayList<>();
		try (InputStream is = request.getFile().getInputStream(); ReadableWorkbook wb = new ReadableWorkbook(is)) {
			wb.getSheets().forEach(sheet -> {
				try (Stream<Row> rows = sheet.openStream()) {
					rows.forEach(r -> {
						if (r.getRowNum() == 1) {
							return; // 跳过表头
						}
						String subjectName = r.getCellText(0);
						String chapterName = r.getCellText(1);
						String sectionName = r.getCellText(2);
						String name = r.getCellText(3);
						if (!hasText(subjectName) || !hasText(chapterName) || !hasText(sectionName) || !hasText(name)) {
							skipped.incrementAndGet();
							return;
						}
						String subjectId = subjectCache.get(subjectName.trim());
						String chapterId = subjectId == null ? null : chapterCache.get(subjectId + "|" + chapterName.trim());
						String sectionId = chapterId == null ? null : sectionCache.get(chapterId + "|" + sectionName.trim());
						String key = (sectionId == null ? "?" : sectionId) + "|" + name.trim();
						if (sectionId == null || existing.contains(key)) {
							skipped.incrementAndGet();
							return;
						}
						existing.add(key);
						KnowledgePoint point = new KnowledgePoint();
						point.setSectionId(sectionId);
						point.setName(name.trim());
						point.setSort(r.getCellAsNumber(4).orElse(BigDecimal.ONE).intValue());
						toSave.add(point);
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
	 * 更新知识点（含内容设置 JSON 与图片地址）。
	 */
	@Override
	public void updateKnowledgePoint(KnowledgePointRequest request) {
		updateById(knowledgePointViewMapper.fromRequest(request));
	}

	/**
	 * 删除知识点（连带逻辑删除其知识点-题目绑定）。
	 */
	@Override
	public void deleteKnowledgePoint(KnowledgePointRequest request) {
		knowledgePointQuestionMapper.delete(Wrappers.<KnowledgePointQuestion>lambdaQuery()
				.eq(KnowledgePointQuestion::getKnowledgePointId, request.getId()));
		removeById(request.getId());
	}

	/**
	 * 保存知识点-题目绑定（全量替换：先清空旧绑定，再批量写入新绑定，事务内完成）。
	 */
	@Override
	public void saveQuestions(KnowledgePointQuestionRequest request) {
		knowledgePointQuestionMapper.delete(Wrappers.<KnowledgePointQuestion>lambdaQuery()
				.eq(KnowledgePointQuestion::getKnowledgePointId, request.getKnowledgePointId()));
		if (CollectionUtils.isEmpty(request.getQuestionIds())) {
			return;
		}
		List<KnowledgePointQuestion> bindings = request.getQuestionIds().stream()
				.filter(Objects::nonNull).distinct().map(questionId -> {
					KnowledgePointQuestion binding = new KnowledgePointQuestion();
					binding.setKnowledgePointId(request.getKnowledgePointId());
					binding.setQuestionId(questionId);
					return binding;
				}).collect(Collectors.toList());
		bindings.forEach(knowledgePointQuestionMapper::insert);
	}

	/**
	 * 查询知识点已绑定的题目列表（题目库 t_template 数据，保持绑定顺序）。
	 */
	@Override
	public List<TemplateView> listQuestions(String knowledgePointId) {
		List<KnowledgePointQuestion> bindings = knowledgePointQuestionMapper.selectList(
				Wrappers.<KnowledgePointQuestion>lambdaQuery()
						.eq(KnowledgePointQuestion::getKnowledgePointId, knowledgePointId)
						.orderByAsc(KnowledgePointQuestion::getCreateAt));
		if (CollectionUtils.isEmpty(bindings)) {
			return Collections.emptyList();
		}
		List<String> questionIds = bindings.stream().map(KnowledgePointQuestion::getQuestionId)
				.collect(Collectors.toList());
		Map<String, Template> templateMap = templateMapper.selectBatchIds(questionIds).stream()
				.collect(Collectors.toMap(Template::getId, t -> t));
		return bindings.stream().map(binding -> templateMap.get(binding.getQuestionId()))
				.filter(Objects::nonNull).map(templateViewMapper::toView).collect(Collectors.toList());
	}

	/**
	 * 根据三级筛选条件解析出目标小节 id 集合（空集合 = 不限定）。
	 */
	private List<String> resolveSectionIds(KnowledgePointQuery query) {
		List<String> sectionIds = new ArrayList<>();
		if (hasText(query.getSectionId())) {
			sectionIds.add(query.getSectionId());
			return sectionIds;
		}
		if (hasText(query.getChapterId())) {
			return sectionMapper.selectList(Wrappers.<Section>lambdaQuery().select(Section::getId)
					.eq(Section::getChapterId, query.getChapterId())).stream()
					.map(Section::getId).collect(Collectors.toList());
		}
		if (hasText(query.getSubjectId())) {
			List<String> chapterIds = chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery()
					.select(Chapter::getId).eq(Chapter::getSubjectId, query.getSubjectId())).stream()
					.map(Chapter::getId).collect(Collectors.toList());
			if (chapterIds.isEmpty()) {
				return Collections.emptyList();
			}
			return sectionMapper.selectList(Wrappers.<Section>lambdaQuery().select(Section::getId)
					.in(Section::getChapterId, chapterIds)).stream()
					.map(Section::getId).collect(Collectors.toList());
		}
		return sectionIds;
	}

	/**
	 * 回填视图的三级归属名称与绑定题目数（按需批量查询，避免 N+1）。
	 */
	private List<KnowledgePointView> fillHierarchyAndQuestionCount(List<KnowledgePoint> records) {
		if (CollectionUtils.isEmpty(records)) {
			return Collections.emptyList();
		}
		List<String> knowledgePointIds = records.stream().map(KnowledgePoint::getId).collect(Collectors.toList());
		Set<String> sectionIdSet = records.stream().map(KnowledgePoint::getSectionId)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		List<Section> sections = sectionIdSet.isEmpty() ? Collections.emptyList()
				: sectionMapper.selectList(Wrappers.<Section>lambdaQuery().in(Section::getId, sectionIdSet));
		Map<String, Section> sectionMap = sections.stream()
				.collect(Collectors.toMap(Section::getId, s -> s));
		Set<String> chapterIdSet = sections.stream().map(Section::getChapterId)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		List<Chapter> chapters = chapterIdSet.isEmpty() ? Collections.emptyList()
				: chapterMapper.selectList(Wrappers.<Chapter>lambdaQuery().in(Chapter::getId, chapterIdSet));
		Map<String, Chapter> chapterMap = chapters.stream()
				.collect(Collectors.toMap(Chapter::getId, c -> c));
		Set<String> subjectIdSet = chapters.stream().map(Chapter::getSubjectId)
				.filter(Objects::nonNull).collect(Collectors.toSet());
		Map<String, Subject> subjectMap = subjectIdSet.isEmpty() ? Collections.emptyMap()
				: subjectMapper.selectList(Wrappers.<Subject>lambdaQuery().in(Subject::getId, subjectIdSet)).stream()
				.collect(Collectors.toMap(Subject::getId, s -> s));
		Map<String, Long> questionCountMap = knowledgePointQuestionMapper.selectList(
				Wrappers.<KnowledgePointQuestion>lambdaQuery().select(KnowledgePointQuestion::getKnowledgePointId)
						.in(KnowledgePointQuestion::getKnowledgePointId, knowledgePointIds)).stream()
				.collect(Collectors.groupingBy(KnowledgePointQuestion::getKnowledgePointId, Collectors.counting()));
		return records.stream().map(point -> {
			KnowledgePointView view = knowledgePointViewMapper.toView(point);
			Section section = sectionMap.get(point.getSectionId());
			view.setSectionName(section == null ? null : section.getName());
			Chapter chapter = section == null ? null : chapterMap.get(section.getChapterId());
			view.setChapterName(chapter == null ? null : chapter.getName());
			Subject subject = chapter == null ? null : subjectMap.get(chapter.getSubjectId());
			view.setSubjectName(subject == null ? null : subject.getName());
			view.setQuestionCount(questionCountMap.getOrDefault(point.getId(), 0L));
			return view;
		}).collect(Collectors.toList());
	}

}
