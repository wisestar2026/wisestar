package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.SectionRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionRequest;
import cn.wisestar.server.domain.dto.knowledge.SectionView;
import cn.wisestar.server.service.SectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小节管理接口（知识管理板块三级维度）。
 *
 * <p><b>定位</b>：管理端「知识管理 → 小节」页面数据源——小节 CRUD；
 * 小节挂载于章节下（chapterId），列表按章节过滤，供顶部下拉三级联动使用；
 * 小节的"内容设置/练习设置"为 JSON 文本透传存储。</p>
 */
@RestController
@RequestMapping("${api.prefix}/section")
@RequiredArgsConstructor
public class SectionApi {

	/**
	 * 小节管理服务（业务层入口，构造器注入）。
	 */
	private final SectionService sectionService;

	/**
	 * 小节列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/section/list（如 /api/section/list）。</p>
	 *
	 * <p><b>请求参数</b>：{@link SectionRequest}（Query 参数：chapterId 可选，
	 * 不传返回全部小节）。</p>
	 *
	 * <p><b>功能</b>：返回小节列表（sort 升序），每项含该小节下的知识点数
	 * knowledgePointCount、已绑定题库数 repoCount 与内容/练习设置 JSON 原文。</p>
	 *
	 * <p><b>返回值结构</b>：{@link SectionView} 列表。</p>
	 *
	 * @param query 小节查询条件
	 * @return 小节视图列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public List<SectionView> listSections(SectionRequest query) {
		return sectionService.listSections(query);
	}

	/**
	 * 新增小节。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/create（如 /api/section/create）。</p>
	 *
	 * <p><b>请求参数</b>：{@link SectionRequest}（@RequestBody JSON：
	 * chapterId/name/sort/content/practice）。</p>
	 *
	 * <p><b>返回值结构</b>：新小节 id（String）。</p>
	 *
	 * @param request 小节请求
	 * @return 新小节 id
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('knowledge:create')")
	public String addSection(@RequestBody SectionRequest request) {
		return sectionService.addSection(request);
	}

	/**
	 * 更新小节（含内容设置/练习设置 JSON）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/update（如 /api/section/update）。</p>
	 *
	 * @param request 小节请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void updateSection(@RequestBody SectionRequest request) {
		sectionService.updateSection(request);
	}

	/**
	 * 删除小节（级联逻辑删除其下知识点及题库绑定）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/delete（如 /api/section/delete）。</p>
	 *
	 * @param request 小节请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('knowledge:delete')")
	public void deleteSection(@RequestBody SectionRequest request) {
		sectionService.deleteSection(request);
	}

	/**
	 * 保存小节-题库绑定（全量替换）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/repos
	 * （如 /api/section/repos）。</p>
	 *
	 * <p><b>功能</b>：将题库管理（t_repo）中选中的题库整体绑定到小节——
	 * 先清空旧绑定再写入新绑定（事务内完成）。题库只能来自题库管理，不能在此新增。</p>
	 *
	 * <p><b>请求参数</b>：{@link SectionRepoRequest}（@RequestBody JSON：
	 * sectionId + repoIds[]）。</p>
	 *
	 * @param request 绑定请求
	 */
	@PostMapping("/repos")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void saveRepos(@RequestBody SectionRepoRequest request) {
		sectionService.saveRepos(request);
	}

	/**
	 * 查询小节已绑定的题库列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/section/repos
	 * （如 /api/section/repos?sectionId=xxx）。</p>
	 *
	 * <p><b>功能</b>：返回该小节已绑定的题库（保持绑定顺序），
	 * 供前端编辑绑定弹窗回显已选题库。</p>
	 *
	 * <p><b>返回值结构</b>：{@link RepoView} 列表。</p>
	 *
	 * @param sectionId 小节ID
	 * @return 已绑定题库列表
	 */
	@GetMapping("/repos")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public List<RepoView> listRepos(@RequestParam("sectionId") String sectionId) {
		return sectionService.listRepos(sectionId);
	}

}
