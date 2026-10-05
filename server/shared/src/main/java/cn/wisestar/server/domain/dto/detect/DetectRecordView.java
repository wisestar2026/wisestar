package cn.wisestar.server.domain.dto.detect;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 检测记录视图（历史条目）。
 *
 * <p>用于学员端 / 管理端查询检测历史，含类型、正确率、薄弱点摘要与基线标识。</p>
 *
 * @author wisestar
 * @date 2026/10/5
 */
@Data
public class DetectRecordView {

	/** 检测记录ID */
	private String id;

	/** 学科ID */
	private String subjectId;

	/** 学科名称快照 */
	private String subjectName;

	/** 年级快照 */
	private String grade;

	/** 册别快照 */
	private String term;

	/** 学期键 */
	private String semester;

	/** 检测类型 PRE / STAGE */
	private String detectType;

	/** 是否成长基线 */
	private boolean baseline;

	/** 组卷题量 */
	private Integer questionCount;

	/** 实际判分题数 */
	private Integer total;

	/** 正确题数 */
	private Integer correct;

	/** 正确率 0-100 */
	private Integer accuracy;

	/** 作答耗时（ms） */
	private Long durationMs;

	/** 勾选章节名称 */
	private List<String> chapterNames = new ArrayList<>();

	/** 薄弱点摘要 */
	private List<DetectReportView.WeakPoint> weakPoints = new ArrayList<>();

	/** 检测时间 */
	private Date createTime;

}
