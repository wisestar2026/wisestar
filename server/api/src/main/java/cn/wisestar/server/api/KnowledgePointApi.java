package cn.wisestar.server.api;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointImportRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuery;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;
import cn.wisestar.server.service.KnowledgePointService;
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
 * 知识点管理接口（知识管理板块最小学习单元）。
 *
 * <p><b>定位</b>：管理端「知识管理 → 知识点」页面数据源——知识点 CRUD +
 * 题目绑定；知识点挂载于小节下（sectionId），列表支持学科/章节/小节三级下拉筛选；
 * 知识点可配图片（imageUrl 为 /api/file/create 上传返回的 previewUrl）；
 * 知识点关联的题目来自题目库（t_template），仅可绑定选择，不能在此新增。</p>
 */
@RestController
@RequestMapping("${api.prefix}/knowledge-point")
@RequiredArgsConstructor
public class KnowledgePointApi {

	/**
	 * 知识点管理服务（业务层入口，构造器注入）。
	 */
	private final KnowledgePointService knowledgePointService;

	/**
	 * 知识点分页列表（三级下拉筛选）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/knowledge-point/list
	 * （如 /api/knowledge-point/list）。</p>
	 *
	 * <p><b>请求参数</b>：{@link KnowledgePointQuery}（Query 参数：
	 * subjectId/chapterId/sectionId 均可选 + current/pageSize；都不传 → 全量分页）。</p>
	 *
	 * <p><b>功能</b>：返回知识点分页（sort 升序），每项含三级归属名称
	 * （subjectName/chapterName/sectionName）、内容设置 JSON 与已绑定题目数 questionCount。</p>
	 *
	 * <p><b>返回值结构</b>：{@link PaginationResponse}（total + list，
	 * 元素为 {@link KnowledgePointView}）。</p>
	 *
	 * @param query 分页 + 三级筛选条件
	 * @return 知识点分页结果
	 */
	@GetMapping("/list")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public PaginationResponse<KnowledgePointView> listKnowledgePoints(KnowledgePointQuery query) {
		return knowledgePointService.listKnowledgePoints(query);
	}

	/**
	 * 新增知识点。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/knowledge-point/create
	 * （如 /api/knowledge-point/create）。</p>
	 *
	 * <p><b>请求参数</b>：{@link KnowledgePointRequest}（@RequestBody JSON：
	 * sectionId/name/sort/content/imageUrl）。</p>
	 *
	 * <p><b>返回值结构</b>：新知识点 id（String）。</p>
	 *
	 * @param request 知识点请求
	 * @return 新知识点 id
	 */
	@PostMapping("/create")
	@PreAuthorize("hasAuthority('knowledge:create')")
	public String addKnowledgePoint(@RequestBody KnowledgePointRequest request) {
		return knowledgePointService.addKnowledgePoint(request);
	}

	/**
	 * 批量导入知识点（multipart 表单：sectionId + file）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/knowledgePoint/import。</p>
	 *
	 * <p><b>功能</b>：解析 Excel（列：知识点名/排序(选填)，首行为表头跳过），
	 * 按 sectionId+name 去重后批量写入 t_knowledge_point。</p>
	 *
	 * <p><b>权限</b>：hasAuthority('knowledge:create')。</p>
	 *
	 * @param request 导入请求（sectionId + Excel 文件）
	 * @return 实际新增知识点数
	 */
	@PostMapping("/import")
	@PreAuthorize("hasAuthority('knowledge:create')")
	public int importKnowledgePoints(KnowledgePointImportRequest request) {
		return knowledgePointService.importKnowledgePoints(request);
	}

	/**
	 * 更新知识点（含内容设置 JSON 与图片地址）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/knowledge-point/update
	 * （如 /api/knowledge-point/update）。</p>
	 *
	 * @param request 知识点请求（含 id）
	 */
	@PostMapping("/update")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void updateKnowledgePoint(@RequestBody KnowledgePointRequest request) {
		knowledgePointService.updateKnowledgePoint(request);
	}

	/**
	 * 删除知识点（连带逻辑删除其题目绑定）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/knowledge-point/delete
	 * （如 /api/knowledge-point/delete）。</p>
	 *
	 * @param request 知识点请求（含 id）
	 */
	@PostMapping("/delete")
	@PreAuthorize("hasAuthority('knowledge:delete')")
	public void deleteKnowledgePoint(@RequestBody KnowledgePointRequest request) {
		knowledgePointService.deleteKnowledgePoint(request);
	}

	/**
	 * 保存知识点-题目绑定（全量替换）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：POST ${api.prefix}/knowledge-point/questions
	 * （如 /api/knowledge-point/questions）。</p>
	 *
	 * <p><b>功能</b>：将题目库（t_template）中选中的题目整体绑定到知识点——
	 * 先清空旧绑定再写入新绑定（事务内完成）。题目不能在此新增。</p>
	 *
	 * <p><b>请求参数</b>：{@link KnowledgePointQuestionRequest}（@RequestBody JSON：
	 * knowledgePointId + questionIds[]）。</p>
	 *
	 * @param request 绑定请求
	 */
	@PostMapping("/questions")
	@PreAuthorize("hasAuthority('knowledge:update')")
	public void saveQuestions(@RequestBody KnowledgePointQuestionRequest request) {
		knowledgePointService.saveQuestions(request);
	}

	/**
	 * 查询知识点已绑定的题目列表。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/knowledge-point/questions
	 * （如 /api/knowledge-point/questions?knowledgePointId=xxx）。</p>
	 *
	 * <p><b>功能</b>：返回该知识点已绑定的题目库题目（保持绑定顺序），
	 * 供前端编辑绑定弹窗回显已选题目。</p>
	 *
	 * <p><b>返回值结构</b>：{@link TemplateView} 列表。</p>
	 *
	 * @param knowledgePointId 知识点ID
	 * @return 已绑定题目列表
	 */
	@GetMapping("/questions")
	@PreAuthorize("hasAuthority('knowledge:list')")
	public List<TemplateView> listQuestions(@RequestParam("knowledgePointId") String knowledgePointId) {
		return knowledgePointService.listQuestions(knowledgePointId);
	}

}
