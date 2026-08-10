package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.knowledge.SubjectRequest;
import cn.wisestar.server.domain.dto.knowledge.SubjectView;

import java.util.List;

/**
 * 学科管理服务（知识管理板块一级维度）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
public interface SubjectService {

	/**
	 * 学科列表（全量，按 sort 升序）。
	 *
	 * @return 学科视图列表（含章节数统计）
	 */
	List<SubjectView> listSubjects();

	/**
	 * 新增学科。
	 *
	 * @param request 学科请求
	 * @return 新学科 id
	 */
	String addSubject(SubjectRequest request);

	/**
	 * 更新学科。
	 *
	 * @param request 学科请求（含 id）
	 */
	void updateSubject(SubjectRequest request);

	/**
	 * 删除学科（逻辑删除；其下章节/小节/知识点不级联删除，仅学科不可见）。
	 *
	 * @param request 学科请求（含 id）
	 */
	void deleteSubject(SubjectRequest request);

}
