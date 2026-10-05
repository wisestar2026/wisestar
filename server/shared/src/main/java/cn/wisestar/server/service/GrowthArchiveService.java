package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.growth.GrowthCompareView;
import cn.wisestar.server.domain.dto.growth.GrowthEventContext;
import cn.wisestar.server.domain.dto.growth.GrowthEventView;
import cn.wisestar.server.domain.dto.growth.GrowthReportRequest;
import cn.wisestar.server.domain.dto.growth.GrowthReportView;

import java.util.List;

/**
 * 成长档案服务（学习轨迹 + 成长对比 + 成长报告）。
 *
 * <p>把学员每一次已落库的学习行为留痕为成长轨迹事件；以学前检测冻结的成长基线为参照，
 * 计算「基线 → 当前」的成长对比；汇总基线与轨迹生成面向家长、可打印的成长报告。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
public interface GrowthArchiveService {

	/**
	 * 幂等写入一条学习轨迹事件（语法类按 grammarId + 日期累加）。
	 *
	 * @param context 轨迹事件上下文
	 */
	void record(GrowthEventContext context);

	/**
	 * 查询学习轨迹时间轴（按发生时间倒序）。
	 *
	 * @param studentId 学员ID，为空取当前登录学员
	 * @param subjectId 学科ID，可空
	 * @param from      起始日期 yyyy-MM-dd，可空
	 * @param to        结束日期 yyyy-MM-dd，可空
	 * @return 轨迹事件列表
	 */
	List<GrowthEventView> timeline(String studentId, String subjectId, String from, String to);

	/**
	 * 成长对比（基线 vs 当前）。
	 *
	 * @param studentId 学员ID，为空取当前登录学员
	 * @param subjectId 学科ID，可空
	 * @param semester  学期键，为空取当前学期
	 * @return 成长对比视图
	 */
	GrowthCompareView compare(String studentId, String subjectId, String semester);

	/**
	 * 生成成长报告（汇总基线 + 对比 + 轨迹 + 目标规划；AI 可用时润色，否则规则降级），并写入档案。
	 *
	 * @param request 生成请求
	 * @return 成长报告视图
	 */
	GrowthReportView generate(GrowthReportRequest request);

	/**
	 * 读取已生成的成长报告。
	 *
	 * @param studentId 学员ID，为空取当前登录学员
	 * @param subjectId 学科ID，可空
	 * @param semester  学期键，为空取当前学期
	 * @return 成长报告视图
	 */
	GrowthReportView report(String studentId, String subjectId, String semester);

	/**
	 * 由来源业务数据重建轨迹（运维/补偿用，幂等）。
	 *
	 * @param studentId 学员ID
	 * @param subjectId 学科ID，可空
	 * @param semester  学期键，可空
	 */
	void rebuild(String studentId, String subjectId, String semester);

}
