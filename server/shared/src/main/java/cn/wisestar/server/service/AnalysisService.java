package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.KnowledgePointStat;
import cn.wisestar.server.domain.dto.KnowledgePointQuery;

import java.util.List;

/**
 * 学生答题情况分析服务接口（AnalysisService）。
 *
 * <p><b>所属模块</b>：shared 模块服务接口包（cn.wisestar.server.service）。
 * 为 AI 自习室系统新增的统计分析能力。</p>
 * <p><b>类职责</b>：定义"学生答题情况"的统计分析接口，按知识点维度
 * 聚合学生的答题记录，输出答题次数、正确次数与正确率，用于
 * 学习情况诊断与知识点画像。</p>
 *
 * <p><b>实现类</b>：{@code cn.wisestar.server.impl.AnalysisServiceImpl}
 * （rdbms 模块，@Service 注册），内部通过 AnswerDetailMapper 查询
 * 答题明细表（t_answer_detail），在内存中按
 * "学科 subject - 章节 chapter - 知识点 knowledgePoint" 聚合；
 * 一道题可挂多个知识点（逗号分隔），会拆开分别计入各知识点。</p>
 *
 * <p><b>调用方</b>：{@code cn.wisestar.server.api.AnalysisApi}
 * （api 模块 Controller）：</p>
 * <ul>
 *   <li>GET /api/analysis/knowledge-point/stats → knowledgePointStats(KnowledgePointQuery)</li>
 *   <li>GET /api/analysis/knowledge-point/student-profile → studentProfile(String)</li>
 * </ul>
 *
 * <p><b>数据流</b>：前端 GET 请求（携带筛选条件）→ AnalysisApi →
 * 本接口 → AnalysisServiceImpl（解析 studentId 为空时取当前登录用户）
 * → AnswerDetailMapper 查询 → 聚合为 List&lt;KnowledgePointStat&gt; → 返回前端。</p>
 *
 * @author zhanghaiyang
 * @date 2026/8/1
 */
public interface AnalysisService {

	/**
	 * 按知识点聚合统计答题情况（正确率）。
	 *
	 * <p><b>功能</b>：按"学科 - 章节 - 知识点"三个维度聚合当前学生（或指定学生）
	 * 的答题明细，返回每个知识点的答题次数（attempts）、正确次数（correctCount）
	 * 与正确率（correctRate，0~1，保留两位小数）。支持按学科/章节/知识点过滤。</p>
	 *
	 * @param query 筛选条件（KnowledgePointQuery）：
	 *              <ul>
	 *                <li>studentId：学生 id，为空时统计当前登录用户；</li>
	 *                <li>subject / chapter：学科、章节过滤；</li>
	 *                <li>knowledgePoint：知识点精确过滤（命中该知识点的答题记录）。</li>
	 *              </ul>
	 * @return 知识点统计结果列表（每个元素含 subject/chapter/knowledgePoint/
	 *         attempts/correctCount/correctRate）
	 */
	List<KnowledgePointStat> knowledgePointStats(KnowledgePointQuery query);

	/**
	 * 单学生知识点画像（该生所有知识点的答题次数与正确率）。
	 *
	 * <p><b>功能</b>：针对指定学生（studentId 为空时取当前登录用户）生成
	 * 全量知识点画像：不附加学科/章节/知识点过滤，返回该生在所有知识点
	 * 维度上的答题情况，用于直观展示强项与薄弱知识点。</p>
	 *
	 * @param studentId 学生 id，为空时取当前登录用户
	 * @return 学生知识点画像列表（各知识点的答题次数与正确率）
	 */
	List<KnowledgePointStat> studentProfile(String studentId);

}
