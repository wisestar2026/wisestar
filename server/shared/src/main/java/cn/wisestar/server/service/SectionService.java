package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.SectionQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;

import java.util.List;

/**
 * 小节管理服务（知识管理板块三级维度）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
public interface SectionService {

	/**
	 * 小节列表（按章节过滤，sort 升序）。
	 *
	 * @param query 查询条件（chapterId 可选；为空返回全部小节）
	 * @return 小节视图列表（含知识点数统计与已绑定测试题数统计）
	 */
	List<SectionView> listSections(SectionRequest query);

	/**
	 * 新增小节。
	 *
	 * @param request 小节请求
	 * @return 新小节 id
	 */
	String addSection(SectionRequest request);

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void updateSection(SectionRequest request);

	/**
	 * 删除小节（级联逻辑删除其下知识点、知识点题目绑定与本小节的测试题目绑定）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void deleteSection(SectionRequest request);

	/**
	 * 保存小节-测试题目绑定（全量替换）。
	 *
	 * <p>传入完整的题目ID列表，先清空该小节的旧绑定再写入新绑定；
	 * 测试题目仅能来自题目库（t_template），本接口不提供新增题目能力。</p>
	 *
	 * @param request 绑定请求（sectionId + questionIds）
	 */
	void saveQuestions(SectionQuestionRequest request);

	/**
	 * 查询小节已绑定的测试题目列表。
	 *
	 * @param sectionId 小节ID
	 * @return 题目视图列表（t_template 数据，含题目名/题型/所属题库）
	 */
	List<TemplateView> listQuestions(String sectionId);

}
