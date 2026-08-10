package cn.wisestar.server.impl;

import cn.wisestar.server.domain.dto.knowledge.SubjectRequest;
import cn.wisestar.server.domain.dto.knowledge.SubjectView;
import cn.wisestar.server.domain.mapper.SubjectViewMapper;
import cn.wisestar.server.domain.model.Chapter;
import cn.wisestar.server.domain.model.Subject;
import cn.wisestar.server.mapper.ChapterMapper;
import cn.wisestar.server.mapper.SubjectMapper;
import cn.wisestar.server.service.BaseService;
import cn.wisestar.server.service.SubjectService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 学科管理业务实现（知识管理板块一级维度）。
 *
 * <p>【被谁调用】SubjectApi（管理端学科管理）。</p>
 * <p>【依赖什么】SubjectMapper/ChapterMapper（BaseMapper CRUD）、SubjectViewMapper（MapStruct 转换）。</p>
 * <p>【数据流】SubjectApi → SubjectServiceImpl → SubjectMapper（t_subject）；列表返回时经
 * ChapterMapper 统计各学科章节数填充 chapterCount。</p>
 *
 * @author wisestar
 * @date 2026/8/10
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SubjectServiceImpl extends BaseService<SubjectMapper, Subject> implements SubjectService {

	private final SubjectViewMapper subjectViewMapper;

	private final ChapterMapper chapterMapper;

	/**
	 * 学科列表（全量，sort 升序），并统计各学科下章节数。
	 */
	@Override
	public List<SubjectView> listSubjects() {
		List<Subject> subjects = list(Wrappers.<Subject>lambdaQuery().orderByAsc(Subject::getSort));
		// 一次查出全部章节，按 subjectId 分组计数，避免逐条 N+1 查询
		Map<String, Long> chapterCountMap = chapterMapper.selectList(
				Wrappers.<Chapter>lambdaQuery().select(Chapter::getSubjectId)).stream()
				.collect(Collectors.groupingBy(Chapter::getSubjectId, Collectors.counting()));
		return subjects.stream().map(subject -> {
			SubjectView view = subjectViewMapper.toView(subject);
			view.setChapterCount(chapterCountMap.getOrDefault(subject.getId(), 0L));
			return view;
		}).collect(Collectors.toList());
	}

	/**
	 * 新增学科。
	 */
	@Override
	public String addSubject(SubjectRequest request) {
		Subject subject = subjectViewMapper.fromRequest(request);
		save(subject);
		return subject.getId();
	}

	/**
	 * 更新学科。
	 */
	@Override
	public void updateSubject(SubjectRequest request) {
		updateById(subjectViewMapper.fromRequest(request));
	}

	/**
	 * 删除学科（逻辑删除；其下章节/小节/知识点不级联删除，仅学科不可见）。
	 */
	@Override
	public void deleteSubject(SubjectRequest request) {
		removeById(request.getId());
	}

}
