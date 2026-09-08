package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.CampusScope;
import cn.wisestar.server.domain.dto.SurveySchema;
import cn.wisestar.server.domain.dto.student.StudentSupervisionView;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.Section;
import cn.wisestar.server.domain.model.Student;
import cn.wisestar.server.domain.model.StudentActivity;
import cn.wisestar.server.domain.model.Template;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.SectionMapper;
import cn.wisestar.server.mapper.StudentActivityMapper;
import cn.wisestar.server.mapper.StudentMapper;
import cn.wisestar.server.mapper.TemplateMapper;
import cn.wisestar.server.service.CampusScopeService;
import cn.wisestar.server.service.StudentSupervisionService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 学员督学服务实现（教师端实时监督在线学员学习位置）。
 *
 * <p>数据源为学员端活动上报表 t_student_activity（学员路由变化/做题切换时 upsert
 * 当前位置），仅返回最近 {@link #ONLINE_WINDOW_MINUTES} 分钟内活跃的在线学员。
 * 章节/小节归属由上报的 sectionId 反查，做题中（questionId 非空）额外回填题干、
 * 标准答案与解析文本，供督学老师直接查看。</p>
 *
 * @author wisestar
 * @date 2026/9/3
 */
@Service
@RequiredArgsConstructor
public class StudentSupervisionServiceImpl implements StudentSupervisionService {

	/** 在线判定窗口：最近 5 分钟内有活跃上报视为在线 */
	private static final int ONLINE_WINDOW_MINUTES = 5;

	private final StudentActivityMapper studentActivityMapper;
	private final StudentMapper studentMapper;
	private final SectionMapper sectionMapper;
	private final ChapterMapper chapterMapper;
	private final TemplateMapper templateMapper;
	private final CampusScopeService campusScopeService;

	@Override
	public List<StudentSupervisionView> getOnlineStudents() {
		Date activeSince = new Date(System.currentTimeMillis() - ONLINE_WINDOW_MINUTES * 60_000L);
		List<StudentActivity> activities = studentActivityMapper.selectList(
				Wrappers.<StudentActivity>lambdaQuery()
						.ge(StudentActivity::getUpdateAt, activeSince)
						.orderByDesc(StudentActivity::getUpdateAt));
		if (activities.isEmpty()) {
			return Collections.emptyList();
		}

		// 校区数据权限范围：EMPTY 无任何校区数据；SCOPED 仅保留绑定校区学员的活动
		CampusScope scope = campusScopeService.resolveScope();
		if (scope.isEmpty()) {
			return Collections.emptyList();
		}
		if (scope.isScoped()) {
			Set<String> campusNames = scope.getCampusNames();
			if (campusNames.isEmpty()) {
				return Collections.emptyList();
			}
			Set<String> allowedStudentIds = studentMapper.selectList(Wrappers.<Student>lambdaQuery()
					.select(Student::getId).in(Student::getCampus, campusNames)).stream().map(Student::getId)
					.collect(Collectors.toSet());
			if (allowedStudentIds.isEmpty()) {
				return Collections.emptyList();
			}
			activities = activities.stream().filter(a -> allowedStudentIds.contains(a.getStudentId()))
					.collect(Collectors.toList());
			if (activities.isEmpty()) {
				return Collections.emptyList();
			}
		}

		Map<String, Student> studentMap = toMap(activities.stream()
				.map(StudentActivity::getStudentId).collect(Collectors.toSet()),
				ids -> studentMapper.selectBatchIds(ids), Student::getId);
		Map<String, Section> sectionMap = toMap(activities.stream()
				.map(StudentActivity::getSectionId).filter(StringUtils::hasText).collect(Collectors.toSet()),
				ids -> sectionMapper.selectBatchIds(ids), Section::getId);
		Map<String, Chapter> chapterMap = toMap(sectionMap.values().stream()
				.map(Section::getChapterId).filter(StringUtils::hasText).collect(Collectors.toSet()),
				ids -> chapterMapper.selectBatchIds(ids), Chapter::getId);
		Map<String, Template> templateMap = toMap(activities.stream()
				.map(StudentActivity::getQuestionId).filter(StringUtils::hasText).collect(Collectors.toSet()),
				ids -> templateMapper.selectBatchIds(ids), Template::getId);

		List<StudentSupervisionView> result = new ArrayList<>();
		for (StudentActivity activity : activities) {
			Student student = studentMap.get(activity.getStudentId());
			if (student == null) {
				continue;
			}
			Section section = sectionMap.get(activity.getSectionId());
			Chapter chapter = section != null ? chapterMap.get(section.getChapterId()) : null;
			Template question = templateMap.get(activity.getQuestionId());

			StudentSupervisionView view = new StudentSupervisionView();
			view.setStudentId(student.getId());
			view.setStudentName(student.getName());
			view.setStudentNo(student.getStudentNo());
			view.setPage(activity.getPage());
			if (activity.getUpdateAt() != null) {
				view.setLastActiveTime(String.valueOf(activity.getUpdateAt().getTime()));
			}
			if (chapter != null) {
				view.setChapterName(chapter.getName());
			}
			if (section != null) {
				view.setSectionName(section.getName());
			}

			// 做题中：学员当前上报了正在查看/作答的题目（预习例题、练习、试炼均属做题）
			view.setStatus(StringUtils.hasText(activity.getQuestionId()) ? "exercising" : "learning");

			StringBuilder location = new StringBuilder();
			if (chapter != null) {
				location.append(chapter.getName());
			}
			if (section != null) {
				if (location.length() > 0) {
					location.append(" / ");
				}
				location.append(section.getName());
			}
			if (question != null) {
				view.setQuestionType(question.getQuestionType() == null ? null : question.getQuestionType().name());
				view.setQuestionContent(question.getName());
				view.setCorrectAnswer(formatAnswer(question.getTemplate()));
				view.setAnswerAnalysis(extractAnalysis(question.getTemplate()));
				if (location.length() > 0) {
					location.append(" / ");
				}
				location.append(question.getName());
			}
			view.setCurrentLocation(location.toString());
			result.add(view);
		}
		return result;
	}

	private <T> Map<String, T> toMap(Set<String> ids, Function<Set<String>, List<T>> loader,
			Function<T, String> keyFn) {
		if (ids.isEmpty()) {
			return new HashMap<>();
		}
		return loader.apply(ids).stream().collect(Collectors.toMap(keyFn, Function.identity(), (a, b) -> a));
	}

	/**
	 * 从题目 schema 提取可读的标准答案文本。
	 *
	 * <p>提取优先级：</p>
	 * <ol>
	 *   <li>整题级答案 attribute.examCorrectAnswer：选项题若命中选项 id/标题/字母（A/B/C…），
	 *       转换为「A. xxx」可读文本；多个答案按换行/逗号分隔逐项转换。</li>
	 *   <li>选项级答案：遍历子选项，attribute.examCorrectAnswer 非空的选项按位置转成
	 *       「字母. 标题」。</li>
	 *   <li>两者皆无（填空/判断/简答类文本答案或未配置）返回原文/空串。</li>
	 * </ol>
	 */
	private String formatAnswer(SurveySchema schema) {
		if (schema == null) {
			return "";
		}
		SurveySchema.Attribute attr = schema.getAttribute();
		if (attr != null && StringUtils.hasText(attr.getExamCorrectAnswer())) {
			String top = attr.getExamCorrectAnswer();
			if (StringUtils.hasText(top)) {
				return mapToOptionText(schema, top);
			}
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

	/** 顶层整题级答案：逐项尝试映射到选项字母+标题，映射不到的保留原文（填空/判断/简答文本）。 */
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

	/** 从题目 schema 提取答案解析文本。 */
	private String extractAnalysis(SurveySchema schema) {
		if (schema == null || schema.getAttribute() == null) {
			return "";
		}
		return schema.getAttribute().getExamAnalysis() == null ? "" : schema.getAttribute().getExamAnalysis();
	}

}
