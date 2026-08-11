package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.ChapterQuestionRequest;
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
 * 章节测试题目经 t_chapter_question 从题目库（t_template）绑定。</p>
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
	 * <p><b>功能</b>：返回章节列表（sort 升序），每项含该章节下的小节数 sectionCount。</p>
	 *
	 * <p><b>返回值结构</b>：{@link ChapterView} 列表。</p>
	 *
	 * @param query 章节查询条件
	 * @return 章节视图列表
	 */
	@GetMapping("/list")
	@PreAuthorize("isAuthenticated()")
	public List<ChapterView> listChapters(ChapterRequest query) {
		return chapterService.listChapters(query);
	}

	/**
	 * 新增章节。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/create（如 /api/chapter/create）。</p>
	 *
	 * <p><b>请求参数</b>：{@link ChapterRequest}（@RequestBody JSON：
	 * subjectId/name/icon/sort）。</p>
	 *
	 * <p><b>返回值结构</b>：新章节 id（String）。</p>
	 *
	 * @param request 章节请求
	 * @return 新章节 id
	 */
	@PostMapping("/create")
	@PreAuthorize("isAuthenticated()")
	public String addChapter(@RequestBody ChapterRequest request) {
		return chapterService.addChapter(request);
	}

	/**
	 * 更新章节。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/update（如 /api/chapter/update）。</p>
	 *
	 * @param request 章节请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("isAuthenticated()")
	public void updateChapter(@RequestBody ChapterRequest request) {
		chapterService.updateChapter(request);
	}

	/**
	 * 删除章节（级联逻辑删除其下小节、知识点及题目绑定）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/delete（如 /api/chapter/delete）。</p>
	 *
	 * @param request 章节请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("isAuthenticated()")
	public void deleteChapter(@RequestBody ChapterRequest request) {
		chapterService.deleteChapter(request);
	}

	/**
	 * 保存章节-测试题目绑定（全量替换）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/chapter/questions
	 * （如 /api/chapter/questions）。</p>
	 *
	 * <p><b>功能</b>：将题目库（t_template）中选中的测试题目整体绑定到章节——
	 * 先清空旧绑定再写入新绑定（事务内完成）。测试题目只能来自题库管理，不能在此新增。</p>
	 *
	 * <p><b>请求参数</b>：{@link ChapterQuestionRequest}（@RequestBody JSON：
	 * chapterId + questionIds[]）。</p>
	 *
	 * @param request 绑定请求
	 */
	@PostMapping("/questions")
	@PreAuthorize("isAuthenticated()")
	public void saveQuestions(@RequestBody ChapterQuestionRequest request) {
		chapterService.saveQuestions(request);
	}

	/**
	 * 查询章节已绑定的测试题目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/chapter/questions
	 * （如 /api/chapter/questions?chapterId=xxx）。</p>
	 *
	 * <p><b>功能</b>：返回该章节已绑定的题库测试题目（保持绑定顺序），
	 * 供前端编辑绑定弹窗回显已选题目。</p>
	 *
	 * <p><b>返回值结构</b>：{@link TemplateView} 列表。</p>
	 *
	 * @param chapterId 章节ID
	 * @return 已绑定测试题目列表
	 */
	@GetMapping("/questions")
	@PreAuthorize("isAuthenticated()")
	public List<TemplateView> listQuestions(@RequestParam("chapterId") String chapterId) {
		return chapterService.listQuestions(chapterId);
	}

}
