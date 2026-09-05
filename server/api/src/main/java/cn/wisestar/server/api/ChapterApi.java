package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.RepoView;
import cn.wisestar.server.domain.dto.knowledge.ChapterImportRequest;
import cn.wisestar.server.domain.dto.knowledge.ImportResultView;
import cn.wisestar.server.domain.dto.knowledge.ChapterRepoRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterRequest;
import cn.wisestar.server.domain.dto.knowledge.ChapterView;
import cn.wisestar.server.service.ChapterService;
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
 * 章节管理接口（知识管理板块二级维度）。
 *
 * <p><b>定位</b>：管理端「知识管理 → 章节」页面数据源——章节 CRUD；
 * 章节挂载于学科下（subjectId），列表按学科过滤，供顶部下拉二级联动使用；
 * 章节题库经 t_chapter_repo 从题库管理（t_repo）绑定。</p>
 */
@RestController
@RequestMapping("${api.prefix}/chapter")
@RequiredArgsConstructor
public class ChapterApi {

	/**
	 * 章节管理服务（业务层入口，构造器注入）。
	 */
	private final ChapterService chapterService;

	/**
	 * 章节列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/chapter/list（如 /api/chapter/list）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ChapterRequest}（Query 参数：subjectId 可选，
	 * 不传返回全部章节）。</p>
	 *
	 * <p><b>功能</b>：返回章节列表（sort 升序），每项含该章节下的小节数 sectionCount
	 * 与已绑定题库数 repoCount。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ChapterView} 列表。</p>
	 *
	 * @param query 章节查询条件
	 * @return 章节视图列表
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public List<ChapterView> listChapters(ChapterRequest query) {
		return chapterService.listChapters(query);
	}

	/**
	 * 导出章节列表为 Excel（附件下载）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/chapter/export
	 * （如 /api/chapter/export?subjectId=&amp;grade=&amp;term=&amp;version=）。</p>
	 *
	 * <p><b>功能</b>：导出 xlsx 文件，列结构：章节名称/年级/学期/版本/小节数/练习数；
	 * 过滤条件与 {@link #listChapters(ChapterRequest)} 一致（subjectId/grade/term/version 可选）。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('knowledge:list')。</p>
	 *
	 * @param query 章节查询条件
	 */
	@GetMapping("/export")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public void exportChapters(ChapterRequest query) {
		chapterService.exportChapters(query);
	}

	/**
	 * 新增章节。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/create（如 /api/chapter/create）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ChapterRequest}（@RequestBody JSON：
	 * subjectId/name/grade/term/version；icon/sort 由系统默认维护，可不传）。</p>
	 *
	 * <p><b>返回值结构</b>：新章节 id（String）。</p>
	 *
	 * @param request 章节请求
	 * @return 新章节 id
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('knowledge:create')")
	public String addChapter(@RequestBody ChapterRequest request) {
		return chapterService.addChapter(request);
	}

	/**
	 * 批量导入章节（multipart 表单：subjectId + file）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/import
	 * （如 /api/chapter/import）。</p>
	 *
	 * <p><b>功能</b>：解析 Excel（列：学科名/章节名称/年级(选填)/学期(选填)/版本(选填)，
	 * 首行为表头跳过），按「学科名+章节名」去重后批量写入 t_chapter；
	 * 图标与排序由系统默认维护（图标 📖、排序追加至学科末尾）。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('knowledge:create')。</p>
	 *
	 * @param request 导入请求（subjectId + Excel 文件）
	 * @return 实际新增章节数
	 */
	@PostMapping("/import")
	@PreAuthorize("hasAuthority('knowledge:create')")
	public ImportResultView importChapters(ChapterImportRequest request) {
		return chapterService.importChapters(request);
	}

	/**
	 * 更新章节。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/update（如 /api/chapter/update）。</p>
	 *
	 * @param request 章节请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void updateChapter(@RequestBody ChapterRequest request) {
		chapterService.updateChapter(request);
	}

	/**
	 * 删除章节（级联逻辑删除其下小节、知识点及题库绑定）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/delete（如 /api/chapter/delete）。</p>
	 *
	 * @param request 章节请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('knowledge:delete')")
	public void deleteChapter(@RequestBody ChapterRequest request) {
		chapterService.deleteChapter(request);
	}

	/**
	 * 保存章节-题库绑定（全量替换）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/repos
	 * （如 /api/chapter/repos）。</p>
	 *
	 * <p><b>功能</b>：将题库管理（t_repo）中选中的题库整体绑定到章节——
	 * 先清空旧绑定再写入新绑定（事务内完成）。题库只能来自题库管理，不能在此新增。</p>
	 *
	 * <p><b>请求参数</b>：{@link ChapterRepoRequest}（@RequestBody JSON：
	 * chapterId + repoIds[]）。</p>
	 *
	 * @param request 绑定请求
	 */
	@PostMapping("/repos")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void saveRepos(@RequestBody ChapterRepoRequest request) {
		chapterService.saveRepos(request);
	}

	/**
	 * 查询章节已绑定的题库列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/chapter/repos
	 * （如 /api/chapter/repos?chapterId=xxx）。</p>
	 *
	 * <p><b>功能</b>：返回该章节已绑定的题库（保持绑定顺序），
	 * 供前端编辑绑定弹窗回显已选题库。</p>
	 *
	 * <p><b>返回值结构</b>：{@link RepoView} 列表。</p>
	 *
	 * @param chapterId 章节ID
	 * @return 已绑定题库列表
	 */
	@GetMapping("/repos")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public List<RepoView> listRepos(@RequestParam("chapterId") String chapterId) {
		return chapterService.listRepos(chapterId);
	}

}
