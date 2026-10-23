package cn.wisestar.server.impl;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.handraise.HandRaiseHandleRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseQuery;
import cn.wisestar.server.domain.dto.handraise.HandRaiseRequest;
import cn.wisestar.server.domain.dto.handraise.HandRaiseView;
import cn.wisestar.server.domain.model.HandRaise;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.HandRaiseMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.CampusScopeService;
import cn.wisestar.server.service.HandRaiseService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员举手反馈服务实现。
 *
 * <p><b>提交链路</b>：学员端题目页「举手」→ 本类 createByStudent。题目快照（题干/选项/
 * 标准答案/解析/知识点）以 t_template 当前内容为准回填，学员作答与选中选项 ID 取学员端上报值，
 * 二者随反馈落库，保证题目后续被修改后仍可还原学员当时的作答现场。</p>
 *
 * <p><b>处理链路</b>：教师端「举手提醒」列表（本类 listHandRaises，按校区数据权限过滤）
 * → explain 类展示学员作答多选界面并标记已处理；question_error / answer_error 类
 * 作为题目管理端待修任务，处理人可直接跳转题目管理修改题干或答案。</p>
 *
 * @author wisestar
 * @date 2026/10/22
 */
@Service
@RequiredArgsConstructor
public class HandRaiseServiceImpl extends BaseService<HandRaiseMapper, HandRaise> implements HandRaiseService {

	/** 合法反馈类型 */
	private static final Set<String> RAISE_TYPES = new java.util.LinkedHashSet<>(
			Arrays.asList(HandRaise.TYPE_EXPLAIN, HandRaise.TYPE_QUESTION_ERROR, HandRaise.TYPE_ANSWER_ERROR));

	/** 选项类题型（只有这些题型保存选项快照与选项ID） */
	private static final Set<String> OPTION_TYPES = new java.util.LinkedHashSet<>(
			Arrays.asList("Radio", "Checkbox", "Multiple", "Judge"));

	private final StudentMapper studentMapper;

	private final TemplateMapper templateMapper;

	private final CampusScopeService campusScopeService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void createByStudent(HandRaiseRequest request) {
		if (request == null || !StringUtils.hasText(request.getQuestionId())) {
			throw new ValidationException("题目信息缺失，无法提交反馈");
		}
		String studentId = SecurityContextUtils.getUserId();
		if (studentMapper.selectById(studentId) == null) {
			throw new ValidationException("当前用户不是学员");
		}
		String raiseType = StringUtils.hasText(request.getRaiseType()) ? request.getRaiseType()
				: HandRaise.TYPE_EXPLAIN;
		if (!RAISE_TYPES.contains(raiseType)) {
			throw new ValidationException("反馈类型不正确");
		}

		HandRaise entity = new HandRaise();
		entity.setStudentId(studentId);
		entity.setRaiseType(raiseType);
		entity.setStatus(HandRaise.STATUS_PENDING);
		entity.setQuestionId(request.getQuestionId());
		entity.setQuestionType(request.getQuestionType());
		entity.setSubject(request.getSubject());
		entity.setChapter(request.getChapter());
		entity.setKnowledgePoint(request.getKnowledgePoint());
		entity.setDifficulty(request.getDifficulty());
		entity.setQuestionContent(request.getQuestionContent());
		entity.setOptionsJson(request.getOptionsJson());
		entity.setStudentAnswer(request.getStudentAnswer());
		entity.setStudentOptionIds(request.getStudentOptionIds());
		entity.setCorrectAnswer(request.getCorrectAnswer());
		entity.setCorrectOptionIds(request.getCorrectOptionIds());
		entity.setAnswerAnalysis(request.getAnswerAnalysis());
		entity.setPage(trimToNull(request.getPage(), 255));
		entity.setSectionId(request.getSectionId());
		entity.setRemark(trimToNull(request.getRemark(), 500));

		// 以题目库当前内容为准回填快照（题目已被删除时保留学员端上报值）
		fillQuestionSnapshot(entity);
		this.baseMapper.insert(entity);
	}

	@Override
	public PaginationResponse<HandRaiseView> listHandRaises(HandRaiseQuery query) {
		CampusScope scope = campusScopeService.resolveScope();
		if (scope.isEmpty()) {
			return emptyPage(query);
		}
		LambdaQueryWrapper<HandRaise> wrapper = Wrappers.<HandRaise>lambdaQuery();
		if (StringUtils.hasText(query.getRaiseType())) {
			if ("error".equals(query.getRaiseType())) {
				wrapper.in(HandRaise::getRaiseType, HandRaise.TYPE_QUESTION_ERROR, HandRaise.TYPE_ANSWER_ERROR);
			}
			else {
				wrapper.eq(HandRaise::getRaiseType, query.getRaiseType());
			}
		}
		wrapper.eq(query.getStatus() != null, HandRaise::getStatus, query.getStatus());
		wrapper.eq(StringUtils.hasText(query.getSubject()), HandRaise::getSubject, query.getSubject());
		wrapper.eq(StringUtils.hasText(query.getStudentId()), HandRaise::getStudentId, query.getStudentId());

		Set<String> campusStudentIds = resolveCampusStudentIds(scope);
		if (scope.isScoped()) {
			if (campusStudentIds.isEmpty()) {
				return emptyPage(query);
			}
			wrapper.in(HandRaise::getStudentId, campusStudentIds);
		}

		if (StringUtils.hasText(query.getKeyword())) {
			String keyword = query.getKeyword().trim();
			Set<String> matchedStudentIds = studentMapper
					.selectList(Wrappers.<Student>lambdaQuery()
							.like(Student::getName, keyword)
							.or()
							.like(Student::getStudentNo, keyword))
					.stream().map(Student::getId).collect(Collectors.toSet());
			wrapper.and(w -> {
				w.like(HandRaise::getQuestionContent, keyword);
				if (!matchedStudentIds.isEmpty()) {
					w.or().in(HandRaise::getStudentId, matchedStudentIds);
				}
			});
		}
		wrapper.orderByAsc(HandRaise::getStatus).orderByDesc(HandRaise::getCreateAt);

		Page<HandRaise> page = pageByQuery(query, wrapper);
		List<HandRaise> records = page.getRecords();
		Map<String, Student> studentMap = new HashMap<>();
		if (!records.isEmpty()) {
			Set<String> studentIds = records.stream().map(HandRaise::getStudentId)
					.filter(StringUtils::hasText).collect(Collectors.toSet());
			if (!studentIds.isEmpty()) {
				studentMap = studentMapper.selectBatchIds(studentIds).stream()
						.collect(Collectors.toMap(Student::getId, Function.identity(), (a, b) -> a));
			}
		}
		List<HandRaiseView> views = new ArrayList<>();
		for (HandRaise record : records) {
			views.add(toView(record, studentMap.get(record.getStudentId())));
		}
		PaginationResponse<HandRaiseView> response = new PaginationResponse<>(page.getTotal(), views);
		response.setCurrent(query.getCurrent());
		response.setPageSize(query.getPageSize());
		return response;
	}

	@Override
	public void handle(HandRaiseHandleRequest request) {
		if (request == null || !StringUtils.hasText(request.getId())) {
			throw new ValidationException("反馈ID不能为空");
		}
		Integer status = request.getStatus();
		if (status == null || (status != HandRaise.STATUS_HANDLED && status != HandRaise.STATUS_IGNORED
				&& status != HandRaise.STATUS_PENDING)) {
			throw new ValidationException("处理状态不正确");
		}
		HandRaise entity = this.baseMapper.selectById(request.getId());
		if (entity == null) {
			throw new ValidationException("反馈不存在或已删除");
		}
		entity.setStatus(status);
		if (status == HandRaise.STATUS_PENDING) {
			entity.setHandlerId(null);
			entity.setHandlerName(null);
			entity.setHandleAt(null);
			entity.setHandleNote(null);
		}
		else {
			entity.setHandlerId(SecurityContextUtils.getUserId());
			String handlerName = SecurityContextUtils.getUser().getName();
			entity.setHandlerName(StringUtils.hasText(handlerName) ? handlerName : SecurityContextUtils.getUsername());
			entity.setHandleAt(new Date());
			entity.setHandleNote(trimToNull(request.getHandleNote(), 500));
		}
		this.baseMapper.updateById(entity);
	}

	@Override
	public Map<String, Long> pendingCount() {
		CampusScope scope = campusScopeService.resolveScope();
		Map<String, Long> result = new LinkedHashMap<>();
		if (scope.isEmpty()) {
			result.put("explain", 0L);
			result.put("error", 0L);
			result.put("total", 0L);
			return result;
		}
		Set<String> campusStudentIds = resolveCampusStudentIds(scope);
		if (scope.isScoped() && campusStudentIds.isEmpty()) {
			result.put("explain", 0L);
			result.put("error", 0L);
			result.put("total", 0L);
			return result;
		}
		long explain = countPending(HandRaise.TYPE_EXPLAIN, scope, campusStudentIds);
		long questionError = countPending(HandRaise.TYPE_QUESTION_ERROR, scope, campusStudentIds);
		long answerError = countPending(HandRaise.TYPE_ANSWER_ERROR, scope, campusStudentIds);
		result.put("explain", explain);
		result.put("error", questionError + answerError);
		result.put("total", explain + questionError + answerError);
		return result;
	}

	private long countPending(String raiseType, CampusScope scope, Set<String> campusStudentIds) {
		LambdaQueryWrapper<HandRaise> wrapper = Wrappers.<HandRaise>lambdaQuery()
				.eq(HandRaise::getRaiseType, raiseType)
				.eq(HandRaise::getStatus, HandRaise.STATUS_PENDING);
		if (scope.isScoped()) {
			wrapper.in(HandRaise::getStudentId, campusStudentIds);
		}
		return this.baseMapper.selectCount(wrapper);
	}

	private Set<String> resolveCampusStudentIds(CampusScope scope) {
		if (!scope.isScoped()) {
			return Collections.emptySet();
		}
		return studentMapper.selectList(Wrappers.<Student>lambdaQuery().in(Student::getCampus, scope.getCampusNames()))
				.stream().map(Student::getId).collect(Collectors.toSet());
	}

	private PaginationResponse<HandRaiseView> emptyPage(HandRaiseQuery query) {
		PaginationResponse<HandRaiseView> response = new PaginationResponse<>(0L, Collections.emptyList());
		response.setCurrent(query.getCurrent());
		response.setPageSize(query.getPageSize());
		return response;
	}

	/**
	 * 按题目库当前内容回填题干/选项/标准答案/解析与知识点四维字段。
	 */
	private void fillQuestionSnapshot(HandRaise entity) {
		Template template = templateMapper.selectById(entity.getQuestionId());
		if (template == null) {
			return;
		}
		SurveySchema schema = template.getTemplate();
		if (template.getQuestionType() != null) {
			entity.setQuestionType(template.getQuestionType().name());
		}
		entity.setSubject(firstText(template.getSubject(), schema, true));
		entity.setChapter(firstText(template.getChapter(), schema, false));
		entity.setDifficulty(schema != null && schema.getAttribute() != null
				&& StringUtils.hasText(schema.getAttribute().getDifficulty()) ? schema.getAttribute().getDifficulty()
						: template.getDifficulty());
		List<String> points = new ArrayList<>();
		if (template.getKnowledgePoint() != null) {
			points.addAll(Arrays.asList(template.getKnowledgePoint()));
		}
		if (points.isEmpty() && schema != null && schema.getAttribute() != null
				&& schema.getAttribute().getKnowledgePoint() != null) {
			points.addAll(schema.getAttribute().getKnowledgePoint());
		}
		if (!points.isEmpty()) {
			entity.setKnowledgePoint(String.join("、", points));
		}
		if (StringUtils.hasText(template.getName())) {
			entity.setQuestionContent(template.getName());
		}
		if (schema == null) {
			return;
		}
		boolean optionType = entity.getQuestionType() != null && OPTION_TYPES.contains(entity.getQuestionType());
		if (!optionType) {
			// 填空/简答类没有选项（schema.children 为占位空项），清空客户端可能上报的选项快照
			entity.setOptionsJson(null);
			entity.setCorrectOptionIds(null);
			entity.setStudentOptionIds(null);
		}
		else {
			List<SurveySchema> options = schema.getChildren() == null ? Collections.emptyList() : schema.getChildren();
			if (!options.isEmpty()) {
				entity.setOptionsJson(buildOptionsJson(options));
				entity.setCorrectOptionIds(buildCorrectOptionIds(schema, options));
			}
		}
		entity.setCorrectAnswer(formatAnswer(schema));
		entity.setAnswerAnalysis(extractAnalysis(schema));
	}

	private String firstText(String topLevel, SurveySchema schema, boolean subject) {
		if (StringUtils.hasText(topLevel)) {
			return topLevel;
		}
		if (schema == null || schema.getAttribute() == null) {
			return null;
		}
		return subject ? schema.getAttribute().getSubject() : schema.getAttribute().getChapter();
	}

	/** 选项快照 JSON：[{"id":..,"title":..}]。 */
	private String buildOptionsJson(List<SurveySchema> options) {
		List<Map<String, String>> list = new ArrayList<>();
		for (SurveySchema option : options) {
			Map<String, String> item = new LinkedHashMap<>();
			item.put("id", option.getId());
			item.put("title", option.getTitle() == null ? "" : option.getTitle());
			list.add(item);
		}
		try {
			return objectMapper.writeValueAsString(list);
		}
		catch (Exception e) {
			return "[]";
		}
	}

	/** 从题目 schema 解析正确选项 ID（逗号分隔）。 */
	private String buildCorrectOptionIds(SurveySchema schema, List<SurveySchema> options) {
		List<String> ids = new ArrayList<>();
		String top = schema.getAttribute() == null ? null : schema.getAttribute().getExamCorrectAnswer();
		if (StringUtils.hasText(top)) {
			for (String raw : top.split("[\n,，、]")) {
				String part = raw.trim();
				if (part.isEmpty()) {
					continue;
				}
				for (int i = 0; i < options.size(); i++) {
					SurveySchema option = options.get(i);
					if (part.equals(option.getId()) || part.equals(option.getTitle())
							|| (part.length() == 1 && part.equalsIgnoreCase(letter(i)))) {
						if (option.getId() != null && !ids.contains(option.getId())) {
							ids.add(option.getId());
						}
						break;
					}
				}
			}
		}
		if (ids.isEmpty()) {
			for (SurveySchema option : options) {
				if (option.getAttribute() != null && StringUtils.hasText(option.getAttribute().getExamCorrectAnswer())
						&& option.getId() != null) {
					ids.add(option.getId());
				}
			}
		}
		return String.join(",", ids);
	}

	/**
	 * 从题目 schema 提取可读的标准答案文本（与督学页口径一致）：
	 * 整题级答案优先，其次选项级答案，最后回退原文（填空/判断/简答）。
	 */
	private String formatAnswer(SurveySchema schema) {
		if (schema == null) {
			return "";
		}
		SurveySchema.Attribute attr = schema.getAttribute();
		if (attr != null && StringUtils.hasText(attr.getExamCorrectAnswer())) {
			return mapToOptionText(schema, attr.getExamCorrectAnswer());
		}
		List<String> letters = new ArrayList<>();
		if (schema.getChildren() != null) {
			for (int i = 0; i < schema.getChildren().size(); i++) {
				SurveySchema option = schema.getChildren().get(i);
				if (option.getAttribute() != null
						&& StringUtils.hasText(option.getAttribute().getExamCorrectAnswer())) {
					letters.add(letter(i) + optionTitle(option));
				}
			}
		}
		return String.join(", ", letters);
	}

	private String mapToOptionText(SurveySchema schema, String topAnswer) {
		List<SurveySchema> options = schema.getChildren() == null ? Collections.emptyList() : schema.getChildren();
		List<String> pieces = new ArrayList<>();
		for (String raw : topAnswer.split("[\n,，、]")) {
			String part = raw.trim();
			if (part.isEmpty()) {
				continue;
			}
			String mapped = null;
			for (int i = 0; i < options.size(); i++) {
				SurveySchema option = options.get(i);
				String id = option.getId();
				String title = option.getTitle();
				if ((id != null && part.equals(id)) || (title != null && part.equals(title))
						|| (part.length() == 1 && part.equalsIgnoreCase(letter(i)))) {
					mapped = letter(i) + optionTitle(option);
					break;
				}
			}
			pieces.add(mapped != null ? mapped : part);
		}
		return String.join(", ", pieces);
	}

	private String letter(int index) {
		return String.valueOf((char) ('A' + index)) + ".";
	}

	private String optionTitle(SurveySchema option) {
		String title = option.getTitle();
		return StringUtils.hasText(title) ? " " + title : "";
	}

	private String extractAnalysis(SurveySchema schema) {
		if (schema == null || schema.getAttribute() == null) {
			return "";
		}
		return schema.getAttribute().getExamAnalysis() == null ? "" : schema.getAttribute().getExamAnalysis();
	}

	private HandRaiseView toView(HandRaise record, Student student) {
		HandRaiseView view = new HandRaiseView();
		view.setId(record.getId());
		view.setRaiseType(record.getRaiseType());
		view.setStatus(record.getStatus());
		view.setStudentId(record.getStudentId());
		if (student != null) {
			view.setStudentName(student.getName());
			view.setStudentNo(student.getStudentNo());
			view.setCampus(student.getCampus());
		}
		view.setQuestionId(record.getQuestionId());
		view.setQuestionType(record.getQuestionType());
		view.setSubject(record.getSubject());
		view.setChapter(record.getChapter());
		view.setKnowledgePoint(record.getKnowledgePoint());
		view.setDifficulty(record.getDifficulty());
		view.setQuestionContent(record.getQuestionContent());
		view.setOptionsJson(record.getOptionsJson());
		view.setStudentAnswer(record.getStudentAnswer());
		view.setStudentOptionIds(record.getStudentOptionIds());
		view.setCorrectAnswer(record.getCorrectAnswer());
		view.setCorrectOptionIds(record.getCorrectOptionIds());
		view.setAnswerAnalysis(record.getAnswerAnalysis());
		view.setPage(record.getPage());
		view.setRemark(record.getRemark());
		view.setHandlerName(record.getHandlerName());
		view.setHandleAt(record.getHandleAt());
		view.setHandleNote(record.getHandleNote());
		view.setCreateAt(record.getCreateAt());
		return view;
	}

	private String trimToNull(String value, int maxLength) {
		if (!StringUtils.hasText(value)) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.length() > maxLength ? trimmed.substring(0, maxLength) : trimmed;
	}

}
