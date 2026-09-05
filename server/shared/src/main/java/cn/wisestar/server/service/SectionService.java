package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.SectionImportRequest;
import cn.wisestar.server.domain.dto.knowledge.ImportResultView;
import cn.wisestar.server.domain.dto.knowledge.SectionRepoRequest;
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
	 * 小节列表（chapterId 可选，年级/学期可选等值过滤，sort 升序）。
	 *
	 * @param query 查询条件（chapterId/grade/term 可选；为空返回全部小节）
	 * @return 小节视图列表（含知识点数统计与已绑定题库数统计）
	 */
	List<SectionView> listSections(SectionRequest query);

	/**
	 * 新增小节（sort 自动按所属章节末尾追加）。
	 *
	 * @param request 小节请求
	 * @return 新小节 id
	 */
	String addSection(SectionRequest request);

	/**
	 * 批量导入小节（Excel：学科名/章节名/小节名/年级/学期，首行表头跳过）。
	 *
	 * <p>按 chapterId + name 去重（已存在的同名小节跳过），返回实际新增条数。
	 * 排序不参与导入，系统按所属章节自动追加。</p>
	 *
	 * @param request 导入请求（chapterId + Excel 文件）
	 * @return 导入结果（新增/跳过条数）
	 */
	ImportResultView importSections(SectionImportRequest request);

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void updateSection(SectionRequest request);

	/**
	 * 删除小节（级联逻辑删除其下知识点、知识点题目绑定与本小节的题库绑定）。
	 *
	 * @param request 小节请求（含 id）
	 */
	void deleteSection(SectionRequest request);

	/**
	 * 保存小节-题库绑定（全量替换）。
	 *
	 * <p>传入完整的题库ID列表，先清空该小节的旧绑定再写入新绑定；
	 * 题库仅能来自题库管理（t_repo），本接口不提供新增题库能力。</p>
	 *
	 * @param request 绑定请求（sectionId + repoIds）
	 */
	void saveRepos(SectionRepoRequest request);

	/**
	 * 查询小节已绑定的题库列表。
	 *
	 * @param sectionId 小节ID
	 * @return 题库视图列表（t_repo 数据，含题库名/标签/学科/年级/难度）
	 */
	List<RepoView> listRepos(String sectionId);

}
