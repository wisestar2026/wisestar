package cn.wisestar.server.api;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.KnowledgePointQuery;
import cn.wisestar.server.domain.dto.KnowledgePointStat;
import cn.wisestar.server.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学生答题情况分析接口（AnalysisApi）
 *
 * <p><b>所属模块</b>：api 模块（Web 接口层，Spring MVC REST Controller）。</p>
 * <p><b>类职责</b>：提供 AI 自习室系统中"学生答题情况分析"相关的统计分析接口，
 * 目前包含两个只读（GET）接口：</p>
 * <ul>
 *   <li>按知识点聚合统计答题情况（正确率）——GET ${api.prefix}/analysis/knowledge-point/stats</li>
 *   <li>单学生知识点画像 ——GET ${api.prefix}/analysis/knowledge-point/student-profile</li>
 * </ul>
 * <p><b>请求路径前缀</b>：由配置项 {@code api.prefix} 决定（通常为 /api），类级路径为
 * {@code ${api.prefix}/analysis}，方法级路径见各方法注解。</p>
 * <p><b>被谁调用</b>：由前端管理后台"统计分析"页面（学生答题情况分析面板）通过 HTTP 调用，
 * 两个接口均要求登录认证（@PreAuthorize("isAuthenticated()")，需携带有效的 JWT 令牌）。</p>
 * <p><b>依赖的服务</b>：注入 {@link AnalysisService}（位于 shared 模块 cn.wisestar.server.service 包），
 * 其实现类位于 rdbms 模块，内部通过 MyBatis Mapper 查询答题明细表并做知识点维度聚合。</p>
 *
 * <p><b>完整数据流（以 knowledgePointStats 为例）</b>：</p>
 * <pre>
 *   前端 HTTP GET /api/analysis/knowledge-point/stats?studentId=xxx&amp;subject=数学
 *     --&gt; Spring Security 认证过滤器校验 JWT（isAuthenticated）
 *     --&gt; AnalysisApi#knowledgePointStats(KnowledgePointQuery query)  （本类）
 *     --&gt; AnalysisService#knowledgePointStats(KnowledgePointQuery)    （shared 接口）
 *     --&gt; AnalysisServiceImpl#knowledgePointStats(...)                （rdbms 实现）
 *     --&gt; XxxMapper 查询答题明细表，按 subject/chapter/knowledge_point GROUP BY 聚合
 *     --&gt; 组装 List&lt;KnowledgePointStat&gt; 返回前端渲染
 * </pre>
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
@RestController
@RequestMapping("${api.prefix}/analysis")
@RequiredArgsConstructor
public class AnalysisApi {

	/**
	 * 统计分析服务（业务层入口）。
	 * <p>由 Lombok @RequiredArgsConstructor 基于 final 字段自动生成构造器注入，
	 * 实际注入的是 shared 模块 {@link AnalysisService} 接口的 rdbms 实现（AnalysisServiceImpl）。</p>
	 */
	private final AnalysisService analysisService;

	/**
	 * 按知识点聚合统计答题情况（正确率）。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/analysis/knowledge-point/stats
	 * （例如 /api/analysis/knowledge-point/stats）。</p>
	 *
	 * <p><b>功能</b>：将学生的答题记录按"学科(subject) - 章节(chapter) - 知识点(knowledgePoint)"
	 * 三个维度聚合，统计每个知识点的答题次数（attempts）、正确次数（correctCount）与
	 * 正确率（correctRate，0-1 之间的小数），用于学习分析面板展示各知识点掌握程度。</p>
	 *
	 * <p><b>请求参数</b>：通过 Spring MVC 参数绑定自动封装为 {@link KnowledgePointQuery}（GET 查询参数），
	 * 均为可选过滤条件：</p>
	 * <ul>
	 *   <li>studentId：学生 id，为空时按当前登录用户统计（后端从 SecurityContext 取 userId）</li>
	 *   <li>subject：学科（模糊或精确过滤，由实现决定）</li>
	 *   <li>chapter：章节</li>
	 *   <li>knowledgePoint：知识点（精确匹配）</li>
	 * </ul>
	 *
	 * <p><b>返回值结构</b>：{@code List<KnowledgePointStat>}，每个元素包含
	 * subject、chapter、knowledgePoint、attempts（long）、correctCount（long）、
	 * correctRate（double，0~1）六个字段；直接以 JSON 数组形式返回。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")——要求请求携带有效登录态，否则 401/403。</p>
	 *
	 * <p><b>异常</b>：查询过程本身不主动抛业务异常；若未登录由 Spring Security 拦截返回未认证错误。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnalysisService#knowledgePointStats(KnowledgePointQuery)}。</p>
	 *
	 * <p><b>数据流</b>：前端 GET 请求（携带过滤参数）→ 本方法（参数绑定为 KnowledgePointQuery）
	 * → AnalysisService.knowledgePointStats → AnalysisServiceImpl 组装查询条件
	 * → MyBatis Mapper 按知识点 GROUP BY 聚合答题明细 → 结果映射为 List&lt;KnowledgePointStat&gt;
	 * → 逐层返回，Spring 序列化为 JSON 响应。</p>
	 *
	 * @param query 知识点统计查询条件（GET query 参数绑定，可传空对象表示全量统计当前用户）
	 * @return 按知识点聚合后的统计结果列表（答题次数、正确次数、正确率）
	 */
	@GetMapping("/knowledge-point/stats")
	@PreAuthorize("isAuthenticated()")
	public List<KnowledgePointStat> knowledgePointStats(KnowledgePointQuery query) {
		return analysisService.knowledgePointStats(query);
	}

	/**
	 * 单学生知识点画像。
	 *
	 * <p><b>HTTP 方法 + 完整路径</b>：GET ${api.prefix}/analysis/knowledge-point/student-profile
	 * （例如 /api/analysis/knowledge-point/student-profile?studentId=xxx）。</p>
	 *
	 * <p><b>功能</b>：针对指定学生（或当前登录学生）生成"知识点画像"，返回该生在所有
	 * 学科/章节/知识点维度上的答题次数与正确率，用于直观展示学生的强项与薄弱知识点，
	 * 供教师端或学生端"我的画像"页面使用。</p>
	 *
	 * <p><b>请求参数</b>：studentId（String，GET 查询参数，可选）。为空时后端取当前登录用户
	 * 的 userId 作为统计对象。</p>
	 *
	 * <p><b>返回值结构</b>：{@code List<KnowledgePointStat>}，与 knowledgePointStats 返回值结构相同
	 * （subject、chapter、knowledgePoint、attempts、correctCount、correctRate），
	 * 但含义上表示"单个学生"的画像数据。</p>
	 *
	 * <p><b>权限</b>：@PreAuthorize("isAuthenticated()")——要求登录。</p>
	 *
	 * <p><b>调用的下层 Service</b>：{@link AnalysisService#studentProfile(String)}。</p>
	 *
	 * <p><b>数据流</b>：前端 GET 请求（studentId 参数）→ 本方法
	 * → AnalysisService.studentProfile(studentId) → AnalysisServiceImpl 以学生 id 过滤
	 * 答题明细 → MyBatis Mapper 按知识点聚合 → List&lt;KnowledgePointStat&gt; → JSON 响应。</p>
	 *
	 * @param studentId 学生 id（GET 查询参数），为空时统计当前登录用户
	 * @return 该学生的知识点画像列表（各知识点的答题次数与正确率）
	 */
	@GetMapping("/knowledge-point/student-profile")
	@PreAuthorize("isAuthenticated()")
	public List<KnowledgePointStat> studentProfile(String studentId) {
		// 越权防护：非管理员/教师只能查询"自己"的画像。
		// 若传入的 studentId 不是当前登录用户，且当前用户无管理员权限，则强制改为查自己。
		String currentUserId = SecurityContextUtils.getUserId();
		boolean targetIsSelf = studentId == null || studentId.isEmpty() || studentId.equals(currentUserId);
		if (!targetIsSelf && !SecurityContextUtils.isAdmin()) {
			studentId = currentUserId;
		}
		return analysisService.studentProfile(studentId);
	}

}
