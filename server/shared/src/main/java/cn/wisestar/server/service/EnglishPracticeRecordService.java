package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.detect.DetectReportView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeHistoryView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeRecordRequest;
import cn.wisestar.server.domain.dto.english.EnglishPracticeRecordView;
import cn.wisestar.server.domain.dto.english.EnglishPracticeSummaryView;

import java.util.List;

/**
 * 英语练习作答记录与错题本服务。
 *
 * <p>只负责「练习结果」这一层：保存每次交卷得分、维护最高分/上次得分、
 * 把做错的题写入英语错题本。诊断报告仍由 {@code EnglishUnitPaperService} 负责。</p>
 *
 * @author wisestar
 * @date 2026/10/23
 */
public interface EnglishPracticeRecordService {

	/**
	 * 保存一次练习作答。
	 *
	 * @param userId  学员账号 ID
	 * @param request 交卷内容
	 * @return 本次与历史成绩概览
	 */
	EnglishPracticeSummaryView record(String userId, EnglishPracticeRecordRequest request);

	/**
	 * 用单元练习/语法练习的诊断报告保存作答记录与错题。
	 *
	 * @param userId  学员账号 ID
	 * @param module  模块 unit/grammar
	 * @param version 教材版本
	 * @param grade   年级
	 * @param term    册别
	 * @param unit    单元
	 * @param report  判分报告
	 * @return 本次与历史成绩概览
	 */
	EnglishPracticeSummaryView recordFromReport(String userId, String module, String version, String grade,
			String term, String unit, DetectReportView report);

	/**
	 * 成绩概览。
	 *
	 * @param userId 学员账号 ID
	 * @param module 模块
	 * @param unit   单元（可空）
	 * @return 概览
	 */
	EnglishPracticeSummaryView summary(String userId, String module, String unit);

	/**
	 * 历史作答记录列表（按时间倒序，不含逐题明细）。
	 *
	 * @param userId 学员账号 ID
	 * @param module 模块
	 * @param unit   单元（可空）
	 * @param limit  条数上限（默认 20，最大 100）
	 * @return 记录列表
	 */
	List<EnglishPracticeHistoryView> history(String userId, String module, String unit, Integer limit);

	/**
	 * 按记录 ID 读取一次作答的逐题明细（仅限本人记录）。
	 *
	 * @param userId   学员账号 ID
	 * @param recordId 记录 ID
	 * @return 作答明细；不存在或不属于本人返回 null
	 */
	EnglishPracticeRecordView detail(String userId, String recordId);

	/**
	 * 上一次作答记录（可按 ID 排除当前这次）。
	 *
	 * @param userId    学员账号 ID
	 * @param module    模块
	 * @param unit      单元（可空）
	 * @param excludeId 需要排除的记录 ID（可空）
	 * @return 上一次作答；无历史返回 null
	 */
	EnglishPracticeRecordView last(String userId, String module, String unit, String excludeId);

}
