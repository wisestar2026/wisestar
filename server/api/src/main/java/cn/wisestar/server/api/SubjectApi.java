package cn.wisestar.server.api;

import cn.wisestar.server.domain.dto.knowledge.SubjectRequest;
import cn.wisestar.server.domain.dto.knowledge.SubjectView;
import cn.wisestar.server.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学科管理接口（知识管理板块一级维度）。
 *
 * <p><b>定位</b>：管理端「知识管理 → 学科」页面数据源——学科 CRUD；
 * 学科是知识管理三级层级（学科 → 章节 → 小节 → 知识点）的最顶层。</p>
 */
@RestController
@RequestMapping("${api.prefix}/subject")
@RequiredArgsConstructor
public class SubjectApi {

	/**
	 * 学科管理服务（业务层入口，构造器注入）。
	 */
	private final SubjectService subjectService;

	/**
	 * 学科列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/subject/list（如 /api/subject/list）。</p>
	 *
	 * <p><b>功能</b>：返回全量学科（sort 升序），每项含该学科下的章节数 chapterCount，
	 * 供管理端学科列表与顶部下拉联动使用。</p>
	 *
	 * <p><b>返回值结构</b>：{@link SubjectView} 列表。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")（登录用户可查看）。</p>
	 *
	 * @return 学科视图列表
	 */
	@GetMapping("/list")
	@PreAuthorize("isAuthenticated()")
	public List<SubjectView> listSubjects() {
		return subjectService.listSubjects();
	}

	/**
	 * 新增学科。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/subject/create（如 /api/subject/create）。</p>
	 *
	 * <p><b>请求参数</b>：{@link SubjectRequest}（@RequestBody JSON：
	 * name/code/icon/themeColor/sort）。</p>
	 *
	 * <p><b>返回值结构</b>：新学科 id（String）。</p>
	 *
	 * @param request 学科请求
	 * @return 新学科 id
	 */
	@PostMapping("/create")
	@PreAuthorize("isAuthenticated()")
	public String addSubject(@RequestBody SubjectRequest request) {
		return subjectService.addSubject(request);
	}

	/**
	 * 更新学科。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/subject/update（如 /api/subject/update）。</p>
	 *
	 * @param request 学科请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("isAuthenticated()")
	public void updateSubject(@RequestBody SubjectRequest request) {
		subjectService.updateSubject(request);
	}

	/**
	 * 删除学科（逻辑删除）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/subject/delete（如 /api/subject/delete）。</p>
	 *
	 * @param request 学科请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("isAuthenticated()")
	public void deleteSubject(@RequestBody SubjectRequest request) {
		subjectService.deleteSubject(request);
	}

}
