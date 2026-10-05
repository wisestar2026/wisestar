package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.detect.DetectGenerateRequest;
import cn.wisestar.server.domain.dto.detect.DetectRecordView;
import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.detect.DetectSubmitRequest;
import cn.wisestar.server.domain.dto.detect.DetectUnitView;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;

import java.util.List;

/**
 * 学员端知识点检测服务。
 *
 * <p><b>定位</b>：学员按学科/年级/册别勾选单元、题量与难度，系统自动组卷做一次诊断，
 * 交卷后按单元与知识点标签聚合出薄弱点。检测为诊断性质，<b>不发放学习币/积分</b>，
 * 也不写入练习记录（避免污染学习进度与奖励结算）。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
public interface DetectionService {

	/**
	 * 检测可选单元列表（按学科 + 年级 + 册别过滤，带可用题量）。
	 *
	 * @param subjectId 学科ID（必填）
	 * @param grade     年级（可空）
	 * @param term      册别（可空，服务端归一化比较）
	 * @return 单元列表
	 */
	List<DetectUnitView> units(String subjectId, String grade, String term);

	/**
	 * 自动组卷（剥离标准答案与解析）。
	 *
	 * @param request 组卷条件
	 * @return 题目列表（无答案）
	 */
	List<StudentQuestionView> generate(DetectGenerateRequest request);

	/**
	 * 交卷并生成薄弱点诊断报告（不落库、不发放奖励）。
	 *
	 * @param request 逐题作答
	 * @return 诊断报告
	 */
	DetectReportView submit(DetectSubmitRequest request);

	/**
	 * 检测历史查询（按时间倒序）。
	 *
	 * @param studentId 学员ID（为空时取当前登录学员）
	 * @param subjectId 学科ID（可空）
	 * @param semester 学期键（可空）
	 * @return 检测记录列表
	 */
	List<DetectRecordView> history(String studentId, String subjectId, String semester);

}
