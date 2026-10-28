package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.math.MathPaperMetaView;
import cn.wisestar.server.domain.dto.math.MathPaperRequest;
import cn.wisestar.server.domain.dto.math.MathPaperStudentView;
import cn.wisestar.server.domain.dto.math.MathPaperView;

import java.util.List;

/**
 * 数学组卷服务（后台管理端）。
 *
 * <p>通用组卷按「章节/小节/知识点 + 难度 + 题型 + 题量」从数学题库抽题；
 * 按学员组卷优先取该学员数学错题（错题次数高者靠前），题量不足时用同范围练习题补齐。
 * 试题按题型分组、组内由易到难排序，便于直接打印。</p>
 *
 * @author wisestar
 * @date 2026/10/26
 */
public interface MathPaperService {

	/**
	 * 组卷配置元数据（年级/册别/章节/小节/知识点树 + 题型/难度统计）。
	 *
	 * @return 配置元数据
	 */
	MathPaperMetaView meta();

	/**
	 * 按学号/姓名搜索学员（最多 20 条）。
	 *
	 * @param keyword 学号或姓名关键字
	 * @return 学员列表
	 */
	List<MathPaperStudentView> searchStudents(String keyword);

	/**
	 * 生成试卷。
	 *
	 * @param request 组卷请求
	 * @return 试卷（含标准答案与解析）
	 */
	MathPaperView generate(MathPaperRequest request);

}
