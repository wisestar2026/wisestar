package cn.wisestar.server.impl;

import cn.wisestar.server.core.uitls.SecurityContextUtils;
import cn.wisestar.server.domain.dto.KnowledgePointQuery;
import cn.wisestar.server.domain.dto.KnowledgePointStat;
import cn.wisestar.server.domain.model.AnswerDetail;
import cn.wisestar.server.mapper.AnswerDetailMapper;
import cn.wisestar.server.service.AnalysisService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 学生答题情况分析实现：基于答题明细表 t_answer_detail 按知识点聚合统计。
 *
 * 【类职责】
 * 提供学生"知识点掌握情况"分析能力：按学生 +（可选）学科/章节/知识点筛选，
 * 汇总每个知识点的作答次数（attempts）、答对次数（correctCount）与正确率（correctRate），
 * 用于生成学生画像（优势/薄弱知识点），支撑 AI 自习室的个性化学情反馈。
 *
 * 【被谁调用】
 * - 上层：AnalysisController（知识点评分统计接口、学生画像接口）
 * - 接口定义：AnalysisService（本类是其实现）
 *
 * 【依赖什么】
 * - AnswerDetailMapper：读取 t_answer_detail（MyBatis-Plus lambda 条件查询）
 * - SecurityContextUtils：获取当前登录用户 ID（studentId 未传时默认查自己）
 * - KnowledgePointQuery / KnowledgePointStat：查询入参与聚合结果 DTO
 *
 * 【核心数据流】
 * 前端请求 → AnalysisController → knowledgePointStats(query) 或 studentProfile(studentId)
 * → 确定查询目标学生（入参优先，否则当前登录用户）→ aggregate() 从 t_answer_detail
 * 按 createBy(学生) + subject/chapter 过滤 → 内存中把每题的 knowledge_point 逗号拆开，
 * 按"学科|章节|知识点"聚合 → 计算正确率 → 返回 List<KnowledgePointStat>。
 *
 * 【注意（越权防护，已处理）】
 * AnalysisApi（Controller 层）已加入越权校验：非管理员传入他人 studentId 查询画像时，
 * 强制改为查询当前登录用户自己；管理员可查任意学生。本实现层保持纯查询职责。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

	/**
	 * 知识点为空/拆分后为空白时的兜底归类名称。
	 */
	private static final String UNCLASSIFIED = "未分类";

	/**
	 * 答题明细表 Mapper：读取 t_answer_detail 明细数据。
	 */
	private final AnswerDetailMapper answerDetailMapper;

	/**
	 * 知识点掌握情况统计（对外接口）。
	 *
	 * @param query 查询条件：studentId（可空，缺省取当前登录用户）、subject（学科）、
	 *              chapter（章节）、knowledgePoint（知识点，可空）
	 * @return 按"学科|章节|知识点"聚合的统计列表（attempts/correctCount/correctRate）
	 * 【注意（越权防护）】
	 * Controller 层已做校验：非管理员传入他人 studentId 时强制改为当前登录用户；
	 * 本实现层不再重复校验，保持"只按入参过滤"的纯查询职责。
	 * 调用方若绕过 Controller 直接使用本 Service，请自行确保 studentId 归属合法。
	 */
	@Override
	public List<KnowledgePointStat> knowledgePointStats(KnowledgePointQuery query) {
		String studentId = StringUtils.hasText(query.getStudentId())
				? query.getStudentId() : SecurityContextUtils.getUserId();
		return aggregate(studentId, query.getSubject(), query.getChapter(), query.getKnowledgePoint());
	}

	/**
	 * 学生画像（对外接口）：聚合指定学生的全部答题明细，不限制学科/章节/知识点维度。
	 *
	 * @param studentId 目标学生 ID（可空，缺省取当前登录用户）
	 * @return 全量聚合的知识点统计列表
	 * @implNote 调用链：AnalysisController → studentProfile → aggregate。
	 *          越权防护在 Controller 层完成（非管理员强制查自己），本层仅按入参聚合。
	 */
	@Override
	public List<KnowledgePointStat> studentProfile(String studentId) {
		String userId = StringUtils.hasText(studentId) ? studentId : SecurityContextUtils.getUserId();
		return aggregate(userId, null, null, null);
	}

	/**
	 * 按学生 + 学科/章节/知识点筛选聚合答题明细（私有核心逻辑）。
	 *
	 * 【内部逻辑步骤】
	 * 1. 构造 lambda 查询：createBy=studentId（必填，Student 归属）、subject/chapter 可选等值过滤；
	 *    knowledgePoint 只做内存过滤（因为存储为逗号分隔字符串，无法精确等值匹配）。
	 * 2. 遍历明细：将每题 knowledge_point 按逗号拆分（splitKnowledgePoints），
	 *    一道题挂多个知识点时分别计入各知识点（同一条明细可贡献多次 attempts）。
	 * 3. 内存 key 为 "subject|chapter|kp"，用 LinkedHashMap 保序聚合：
	 *    attempts+1；isCorrect=1 时 correctCount+1。
	 * 4. 计算正确率：correctRate = correctCount*10000.0/attempts 取整后 /100.0（保留两位小数）。
	 *
	 * 【为什么这么写】
	 * - 知识点多值：存储端是逗号分隔字符串，无法用 SQL IN 精确匹配"某知识点"，故拆开逐点计数；
	 * - 正确率保留两位：乘法放大 10000 再整除 100，等价于 Math.round 两位小数，避免浮点误差。
	 *
	 * @param studentId      目标学生 ID（t_answer_detail.createBy）
	 * @param subject        学科筛选（可空）
	 * @param chapter        章节筛选（可空）
	 * @param knowledgePoint 知识点筛选（可空，非空时只统计匹配的知识点）
	 * @return 聚合统计结果列表
	 */
	private List<KnowledgePointStat> aggregate(String studentId, String subject, String chapter, String knowledgePoint) {
		// 从 t_answer_detail 查询该学生的答题明细（逻辑删除自动过滤）
		List<AnswerDetail> details = answerDetailMapper.selectList(
				Wrappers.<AnswerDetail>lambdaQuery()
						.eq(StringUtils.hasText(studentId), AnswerDetail::getCreateBy, studentId)
						.eq(StringUtils.hasText(subject), AnswerDetail::getSubject, subject)
						.eq(StringUtils.hasText(chapter), AnswerDetail::getChapter, chapter));

		// key: subject|chapter|knowledgePoint（用 LinkedHashMap 保持首次出现顺序，结果稳定可读）
		Map<String, KnowledgePointStat> statsMap = new LinkedHashMap<>();
		for (AnswerDetail detail : details) {
			List<String> kps = splitKnowledgePoints(detail.getKnowledgePoint());
			for (String kp : kps) {
				// 知识点筛选：非空且不相等则跳过（kps 中已含 UNCLASSIFIED 兜底值）
				if (StringUtils.hasText(knowledgePoint) && !knowledgePoint.equals(kp)) {
					continue;
				}
				String key = (detail.getSubject() == null ? "" : detail.getSubject())
						+ "|" + (detail.getChapter() == null ? "" : detail.getChapter())
						+ "|" + kp;
				KnowledgePointStat stat = statsMap.computeIfAbsent(key, k -> {
					KnowledgePointStat s = new KnowledgePointStat();
					s.setSubject(detail.getSubject());
					s.setChapter(detail.getChapter());
					s.setKnowledgePoint(kp);
					return s;
				});
				stat.setAttempts(stat.getAttempts() + 1);
				if (Integer.valueOf(1).equals(detail.getIsCorrect())) {
					stat.setCorrectCount(stat.getCorrectCount() + 1);
				}
			}
		}
		List<KnowledgePointStat> result = new ArrayList<>(statsMap.values());
		result.forEach(stat -> {
			if (stat.getAttempts() > 0) {
				stat.setCorrectRate(Math.round(stat.getCorrectCount() * 10000.0 / stat.getAttempts()) / 100.0);
			}
		});
		return result;
	}

	/**
	 * 拆分知识点字符串；空值/空白段归「未分类」。
	 *
	 * 【数据从哪来】AnswerDetail.knowledgePoint —— 生成明细时由题目 attribute.knowledgePoint
	 * 数组 join 逗号得到（如 "函数单调性,奇偶性"）。
	 * 【格式约定】逗号分隔；每个段 trim 后为空也归 UNCLASSIFIED，保证聚合 key 始终非空。
	 *
	 * @param knowledgePoint 逗号分隔的知识点字符串（可为 null/空白）
	 * @return 拆分后的知识点列表（空值时为单元素 ["未分类"]）
	 */
	private List<String> splitKnowledgePoints(String knowledgePoint) {
		List<String> result = new ArrayList<>();
		if (!StringUtils.hasText(knowledgePoint)) {
			result.add(UNCLASSIFIED);
			return result;
		}
		for (String kp : knowledgePoint.split(",")) {
			result.add(StringUtils.hasText(kp) ? kp.trim() : UNCLASSIFIED);
		}
		return result;
	}

}
