package cn.wisestar.server.service;

import cn.wisestar.server.domain.dto.archive.ArchiveRecordDraftView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveOverviewView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveRecordView;
import cn.wisestar.server.domain.dto.archive.StudentArchiveSaveRequest;
import cn.wisestar.server.domain.dto.archive.StudentArchiveView;
import cn.wisestar.server.domain.dto.student.StudentWeakView;

import java.util.List;

/**
 * 学员档案服务（学习规划 + 上课记录 + 学期报告）。
 *
 * <p>以知识点检测/薄弱点为新学期档案的初始快照，老师据此填写本学期目标规划表与承诺书；
 * 每次上课记录逐条汇入档案形成学习日志，学期末汇总为可打印的学期报告。档案整体可交付家长。</p>
 *
 * @author wisestar
 * @date 2026/10/3
 */
public interface StudentArchiveService {

	/**
	 * 查询学员档案详情（不存在时返回空白视图，含实时薄弱点、记录与当日情况）。
	 *
	 * @param studentId 学员ID
	 * @param semester  学期键，为空取当前学期
	 * @return 档案详情视图
	 */
	StudentArchiveView getArchive(String studentId, String semester);

	/**
	 * 查询当前登录学员本人本学期档案（学员端）。
	 *
	 * @return 档案详情视图
	 */
	StudentArchiveView myArchive();

	/**
	 * 新建/更新学员档案（目标规划表、承诺书、学期报告等）。
	 *
	 * @param request 档案保存请求
	 * @return 保存后的档案详情视图
	 */
	StudentArchiveView save(StudentArchiveSaveRequest request);

	/**
	 * 学员档案概览（列表入口角标：是否建档、薄弱点/记录数、报告状态）。
	 *
	 * @param studentId 学员ID
	 * @return 概览视图
	 */
	StudentArchiveOverviewView overview(String studentId);

	/**
	 * 新增/更新上课记录；档案不存在时自动建档。
	 *
	 * @param request 记录保存请求
	 * @return 保存后的记录视图
	 */
	StudentArchiveRecordView saveRecord(StudentArchiveRecordRequest request);

	/**
	 * 删除上课记录（逻辑删除）。
	 *
	 * @param id 记录ID
	 */
	void deleteRecord(String id);

	/**
	 * 生成某日上课记录自动草稿（拉取当日学习数据与薄弱/强化知识点）。
	 *
	 * @param studentId 学员ID
	 * @param date      日期 yyyy-MM-dd，为空取当天
	 * @return 草稿视图
	 */
	ArchiveRecordDraftView draft(String studentId, String date);

	/**
	 * 生成学期报告（汇总目标、记录与薄弱点；AI 可用时润色，否则规则模板），并写入档案。
	 *
	 * @param studentId 学员ID
	 * @param semester  学期键，为空取当前学期
	 * @return 更新后的档案详情视图
	 */
	StudentArchiveView generateReport(String studentId, String semester);

	/**
	 * 定格成长基线：把学前检测结果写入「学员 + 学期 + 学科」档案的基线快照与基线检测ID。
	 *
	 * @param studentId  学员ID
	 * @param subjectId  学科ID
	 * @param semester   学期键
	 * @param detectId   定格为基线的检测记录ID
	 * @param accuracy   基线整体正确率
	 * @param weakPoints 基线薄弱知识点（名称 + 正确率）
	 */
	void bindBaseline(String studentId, String subjectId, String semester, String detectId,
			int accuracy, List<StudentWeakView> weakPoints);

}
