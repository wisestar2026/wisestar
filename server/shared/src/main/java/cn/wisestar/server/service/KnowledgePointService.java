package cn.wisestar.server.service;

import cn.wisestar.server.core.common.PaginationResponse;
import cn.wisestar.server.domain.dto.TemplateView;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointImportRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuery;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointQuestionRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointRequest;
import cn.wisestar.server.domain.dto.knowledge.KnowledgePointView;

import java.util.List;

/**
 * 知识点管理服务（知识管理板块最小学习单元）。
 *
 * @author wisestar
 * @date 2026/8/10
 */
public interface KnowledgePointService {

	/**
	 * 知识点分页列表。
	 *
	 * <p>三级下拉条件均可选：都不传 → 全量分页；只传 subjectId → 该学科全部知识点；
	 * 再传 chapterId → 缩小到该章节；传全 sectionId → 该小节知识点。
	 * 视图含三级归属名称与已绑定题目数。</p>
	 *
	 * @param query 分页 + 筛选条件
	 * @return 知识点分页视图
	 */
	PaginationResponse<KnowledgePointView> listKnowledgePoints(KnowledgePointQuery query);

	/**
	 * 新增知识点。
	 *
	 * @param request 知识点请求
	 * @return 新知识点 id
	 */
	String addKnowledgePoint(KnowledgePointRequest request);

	/**
	 * 批量导入知识点（Excel：知识点名/排序，首行表头跳过）。
	 *
	 * <p>按 sectionId + name 去重（已存在的同名知识点跳过），返回实际新增条数。</p>
	 *
	 * @param request 导入请求（sectionId + Excel 文件）
	 * @return 新增知识点数
	 */
	int importKnowledgePoints(KnowledgePointImportRequest request);

	/**
	 * 更新知识点（含内容设置/图片地址）。
	 *
	 * @param request 知识点请求（含 id）
	 */
	void updateKnowledgePoint(KnowledgePointRequest request);

	/**
	 * 删除知识点（连带逻辑删除其题目绑定）。
	 *
	 * @param request 知识点请求（含 id）
	 */
	void deleteKnowledgePoint(KnowledgePointRequest request);

	/**
	 * 保存知识点-题目绑定（全量替换）。
	 *
	 * <p>传入完整的题目ID列表，先清空该知识点的旧绑定再写入新绑定；
	 * 题目仅能来自题目库（t_template），本接口不提供新增题目能力。</p>
	 *
	 * @param request 绑定请求（knowledgePointId + questionIds）
	 */
	void saveQuestions(KnowledgePointQuestionRequest request);

	/**
	 * 查询知识点已绑定的题目列表。
	 *
	 * @param knowledgePointId 知识点ID
	 * @return 题目视图列表（t_template 数据，含题目名/题型/所属题库）
	 */
	List<TemplateView> listQuestions(String knowledgePointId);

}
