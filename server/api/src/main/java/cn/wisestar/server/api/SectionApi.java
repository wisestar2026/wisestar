package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.SectionQuestionRequest;
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
	 * knowledgePointCount 与内容/练习设置 JSON 原文。</p>
	 *
	 * <p><b>返回值结构</b>：{@link SectionView} 列表。</p>
	 *
	 * @param query 小节查询条件
	 * @return 小节视图列表
	 */
	@GetMapping("/list")
	@PreAuthorize("isAuthenticated()")
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
	@PreAuthorize("isAuthenticated()")
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
	@PreAuthorize("isAuthenticated()")
	public void updateSection(@RequestBody SectionRequest request) {
		sectionService.updateSection(request);
	}

	/**
	 * 删除小节（级联逻辑删除其下知识点及题目绑定）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/delete（如 /api/section/delete）。</p>
	 *
	 * @param request 小节请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("isAuthenticated()")
	public void deleteSection(@RequestBody SectionRequest request) {
		sectionService.deleteSection(request);
	}

	/**
	 * 保存小节-测试题目绑定（全量替换）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/section/questions
	 * （如 /api/section/questions）。</p>
	 *
	 * <p><b>功能</b>：将题目库（t_template）中选中的测试题目整体绑定到小节——
	 * 先清空旧绑定再写入新绑定（事务内完成）。测试题目只能来自题库管理，不能在此新增。</p>
	 *
	 * <p><b>请求参数</b>：{@link SectionQuestionRequest}（@RequestBody JSON：
	 * sectionId + questionIds[]）。</p>
	 *
	 * @param request 绑定请求
	 */
	@PostMapping("/questions")
	@PreAuthorize("isAuthenticated()")
	public void saveQuestions(@RequestBody SectionQuestionRequest request) {
		sectionService.saveQuestions(request);
	}

	/**
	 * 查询小节已绑定的测试题目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/section/questions
	 * （如 /api/section/questions?sectionId=xxx）。</p>
	 *
	 * <p><b>功能</b>：返回该小节已绑定的题库测试题目（保持绑定顺序），
	 * 供前端编辑绑定弹窗回显已选题目。</p>
	 *
	 * <p><b>返回值结构</b>：{@link TemplateView} 列表。</p>
	 *
	 * @param sectionId 小节ID
	 * @return 已绑定测试题目列表
	 */
	@GetMapping("/questions")
	@PreAuthorize("isAuthenticated()")
	public List<TemplateView> listQuestions(@RequestParam("sectionId") String sectionId) {
		return sectionService.listQuestions(sectionId);
	}

}
