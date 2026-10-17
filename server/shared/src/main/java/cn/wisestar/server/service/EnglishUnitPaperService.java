package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperRequest;
import cn.wisestar.server.domain.dto.english.EnglishUnitPaperSubmitRequest;
import cn.wisestar.server.domain.dto.student.StudentQuestionView;

import java.util.List;

/**
 * 英语单元练习组卷服务（内容包阶梯卷）。
 *
 * <p>以 {@code t_english_content_tag} 为索引，把句子包/情景包中的真实考题按
 * 知识点分组，组内按 D1→D3 递进取题；缺失难度用最近难度补位，
 * 优先覆盖学员薄弱模块。交卷时按内容键回读题库答案判分并入账。</p>
 *
 * @author wisestar
 * @date 2026/10/17
 */
public interface EnglishUnitPaperService {

	/**
	 * 组卷（剥离答案）。
	 *
	 * @param request 组卷请求
	 * @return 题目列表（按知识点阶梯顺序）
	 */
	List<StudentQuestionView> generate(EnglishUnitPaperRequest request);

	/**
	 * 交卷判分并生成诊断报告。
	 *
	 * @param request 交卷请求
	 * @return 诊断报告
	 */
	DetectReportView submit(EnglishUnitPaperSubmitRequest request);

}
